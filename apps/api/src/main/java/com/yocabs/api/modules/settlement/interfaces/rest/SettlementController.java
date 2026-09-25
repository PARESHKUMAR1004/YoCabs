package com.yocabs.api.modules.settlement.interfaces.rest;

import com.yocabs.api.modules.settlement.application.SettlementService;
import com.yocabs.api.modules.settlement.domain.Payout;
import com.yocabs.api.modules.settlement.domain.WalletEntry;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/wallet")
    public WalletResponse wallet(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        var wallet = settlementService.wallet(actor, travelPartnerId, limit);

        return new WalletResponse(
                wallet.balance(),
                wallet.pendingPayouts(),
                wallet.balance().subtract(wallet.pendingPayouts()),
                wallet.entries().stream().map(EntryResponse::from).toList()
        );
    }

    @PostMapping("/api/v1/travel-partners/{travelPartnerId}/payouts")
    @ResponseStatus(HttpStatus.CREATED)
    public PayoutResponse request(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestBody PayoutRequest request
    ) {
        return PayoutResponse.from(
                settlementService.requestPayout(actor, travelPartnerId, request.amount())
        );
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/payouts")
    public List<PayoutResponse> list(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId
    ) {
        return settlementService.listPayouts(actor, travelPartnerId).stream()
                .map(PayoutResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/payouts")
    public List<PayoutResponse> listForAdmin(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "REQUESTED") Payout.Status status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return settlementService.listByStatus(actor, status, limit).stream()
                .map(PayoutResponse::from).toList();
    }

    @PostMapping("/api/v1/admin/payouts/{payoutId}/pay")
    public PayoutResponse pay(
            @CurrentActor Actor actor,
            @PathVariable UUID payoutId,
            @RequestBody PayRequest request
    ) {
        return PayoutResponse.from(
                settlementService.markPaid(actor, payoutId, request.bankReference())
        );
    }

    @PostMapping("/api/v1/admin/payouts/{payoutId}/reject")
    public PayoutResponse reject(
            @CurrentActor Actor actor,
            @PathVariable UUID payoutId,
            @RequestBody RejectRequest request
    ) {
        return PayoutResponse.from(
                settlementService.reject(actor, payoutId, request.note())
        );
    }

    public record PayoutRequest(BigDecimal amount) {
    }

    public record PayRequest(String bankReference) {
    }

    public record RejectRequest(String note) {
    }

    public record WalletResponse(
            BigDecimal balance,
            BigDecimal pendingPayouts,
            BigDecimal availableForPayout,
            List<EntryResponse> entries
    ) {
    }

    public record EntryResponse(
            UUID id,
            WalletEntry.Type type,
            BigDecimal amount,
            String referenceType,
            UUID referenceId,
            String description,
            Instant createdAt
    ) {

        static EntryResponse from(WalletEntry entry) {
            return new EntryResponse(
                    entry.id(), entry.type(), entry.amount(), entry.referenceType(),
                    entry.referenceId(), entry.description(), entry.createdAt()
            );
        }
    }

    public record PayoutResponse(
            UUID id,
            UUID travelPartnerId,
            BigDecimal amount,
            Payout.Status status,
            String bankReference,
            String note,
            Instant createdAt,
            Instant processedAt
    ) {

        static PayoutResponse from(Payout payout) {
            return new PayoutResponse(
                    payout.getId(), payout.getTravelPartnerId(), payout.getAmount(),
                    payout.getStatus(), payout.getBankReference(), payout.getNote(),
                    payout.getCreatedAt(), payout.getProcessedAt()
            );
        }
    }
}
