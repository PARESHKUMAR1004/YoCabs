package com.yocabs.api.modules.settlement.application;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.settlement.domain.Payout;
import com.yocabs.api.modules.settlement.domain.SettlementRepository;
import com.yocabs.api.modules.settlement.domain.WalletEntry;
import com.yocabs.api.shared.events.BalancePaid;
import com.yocabs.api.shared.events.BookingCompleted;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Partner ledger. Settlement model (to be confirmed by finance): YoCabs
 * collects the booking token from the tourist; the partner collects the
 * remainder directly. When a trip completes, the ledger nets the token
 * against the commission: a surplus is owed to the partner, a shortfall is
 * owed by the partner. The rule lives solely in {@link #onBookingCompleted}.
 */
@Service
public class SettlementService {

    private static final String BOOKING = "BOOKING";
    private static final String BOOKING_BALANCE = "BOOKING_BALANCE";
    private static final String PAYOUT = "PAYOUT";

    private final SettlementRepository settlement;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public SettlementService(
            SettlementRepository settlement,
            AuditService auditService,
            ApplicationEventPublisher events
    ) {
        this.settlement = settlement;
        this.auditService = auditService;
        this.events = events;
    }

    @EventListener
    @Transactional
    public void onBookingCompleted(BookingCompleted event) {

        BigDecimal net = event.tokenAmount().subtract(event.commissionAmount());

        if (net.signum() == 0) {
            return;
        }

        WalletEntry.Type type = net.signum() > 0 ? WalletEntry.Type.CREDIT : WalletEntry.Type.DEBIT;

        // Idempotent: a booking settles at most once.
        if (settlement.entryExists(BOOKING, event.bookingId(), type)) {
            return;
        }

        settlement.lockPartner(event.travelPartnerId());

        settlement.append(
                WalletEntry.of(
                        event.travelPartnerId(),
                        type,
                        net.abs(),
                        BOOKING,
                        event.bookingId(),
                        type == WalletEntry.Type.CREDIT
                                ? "Token collected exceeds commission on completed trip"
                                : "Commission owed on completed trip"
                )
        );
    }

    /** The tourist paid the balance through YoCabs, so YoCabs owes that money to the partner. */
    @EventListener
    @Transactional
    public void onBalancePaid(BalancePaid event) {

        if (event.amount().signum() <= 0
                || settlement.entryExists(BOOKING_BALANCE, event.bookingId(), WalletEntry.Type.CREDIT)) {
            return;
        }

        settlement.lockPartner(event.travelPartnerId());

        settlement.append(
                WalletEntry.of(
                        event.travelPartnerId(),
                        WalletEntry.Type.CREDIT,
                        event.amount(),
                        BOOKING_BALANCE,
                        event.bookingId(),
                        "Balance paid online by the traveller"
                )
        );

        events.publishEvent(
                NotificationRequested.toPartner(
                        event.travelPartnerId(),
                        "BALANCE_PAID",
                        "Balance paid online",
                        "The traveller paid " + event.amount() + " online. It has been added to your wallet.",
                        BOOKING,
                        event.bookingId()
                )
        );
    }

    @Transactional(readOnly = true)
    public Wallet wallet(Actor actor, UUID travelPartnerId, int limit) {
        actor.requirePartnerAccess(travelPartnerId);

        return new Wallet(
                settlement.balance(travelPartnerId),
                settlement.sumOpenPayoutRequests(travelPartnerId),
                settlement.findEntries(travelPartnerId, limit)
        );
    }

    @Transactional
    public Payout requestPayout(Actor actor, UUID travelPartnerId, BigDecimal amount) {

        actor.requirePartnerAccess(travelPartnerId);

        if (actor.role() == Role.PARTNER_STAFF) {
            throw new AccessDeniedException("Only the partner owner can request a payout");
        }

        settlement.lockPartner(travelPartnerId);

        BigDecimal available =
                settlement.balance(travelPartnerId)
                        .subtract(settlement.sumOpenPayoutRequests(travelPartnerId));

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payout amount must be greater than zero");
        }

        if (amount.compareTo(available) > 0) {
            throw new IllegalStateException("The requested amount exceeds the available balance");
        }

        return settlement.createPayout(Payout.request(travelPartnerId, amount, actor.userId()));
    }

    @Transactional(readOnly = true)
    public List<Payout> listPayouts(Actor actor, UUID travelPartnerId) {
        actor.requirePartnerAccess(travelPartnerId);
        return settlement.findPayoutsByPartner(travelPartnerId);
    }

    @Transactional(readOnly = true)
    public List<Payout> listByStatus(Actor actor, Payout.Status status, int limit) {
        actor.requireAdmin();
        return settlement.findPayoutsByStatus(status, limit);
    }

    @Transactional
    public Payout markPaid(Actor actor, UUID payoutId, String bankReference) {

        actor.requireAdmin();

        Payout payout = loadPayout(payoutId);

        settlement.lockPartner(payout.getTravelPartnerId());

        if (settlement.balance(payout.getTravelPartnerId()).compareTo(payout.getAmount()) < 0) {
            throw new IllegalStateException("The partner balance no longer covers this payout");
        }

        payout.markPaid(actor.userId(), bankReference);
        Payout saved = settlement.updatePayout(payout);

        settlement.append(
                WalletEntry.of(
                        saved.getTravelPartnerId(),
                        WalletEntry.Type.DEBIT,
                        saved.getAmount(),
                        PAYOUT,
                        saved.getId(),
                        "Payout " + saved.getBankReference()
                )
        );

        auditService.record(actor, "PAYOUT_PAID", PAYOUT, saved.getId(),
                "amount=" + saved.getAmount() + " ref=" + saved.getBankReference());

        events.publishEvent(
                NotificationRequested.toPartner(
                        saved.getTravelPartnerId(),
                        "PAYOUT_PAID",
                        "Payout processed",
                        "Your payout of " + saved.getAmount() + " has been processed.",
                        PAYOUT,
                        saved.getId()
                )
        );

        return saved;
    }

    @Transactional
    public Payout reject(Actor actor, UUID payoutId, String note) {

        actor.requireAdmin();

        Payout payout = loadPayout(payoutId);
        payout.reject(actor.userId(), note);
        Payout saved = settlement.updatePayout(payout);

        auditService.record(actor, "PAYOUT_REJECTED", PAYOUT, saved.getId(), note);

        events.publishEvent(
                NotificationRequested.toPartner(
                        saved.getTravelPartnerId(),
                        "PAYOUT_REJECTED",
                        "Payout rejected",
                        note == null || note.isBlank()
                                ? "Your payout request was rejected."
                                : "Your payout request was rejected: " + note,
                        PAYOUT,
                        saved.getId()
                )
        );

        return saved;
    }

    private Payout loadPayout(UUID payoutId) {
        return settlement.findPayoutById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found: " + payoutId));
    }

    public record Wallet(BigDecimal balance, BigDecimal pendingPayouts, List<WalletEntry> entries) {
    }
}
