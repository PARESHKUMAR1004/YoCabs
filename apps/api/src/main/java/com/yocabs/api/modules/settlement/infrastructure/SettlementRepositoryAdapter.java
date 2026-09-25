package com.yocabs.api.modules.settlement.infrastructure;

import com.yocabs.api.modules.settlement.domain.Payout;
import com.yocabs.api.modules.settlement.domain.SettlementRepository;
import com.yocabs.api.modules.settlement.domain.WalletEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SettlementRepositoryAdapter implements SettlementRepository {

    private final WalletEntryJpaRepository entries;
    private final PayoutJpaRepository payouts;

    @PersistenceContext
    private EntityManager entityManager;

    public SettlementRepositoryAdapter(
            WalletEntryJpaRepository entries,
            PayoutJpaRepository payouts
    ) {
        this.entries = entries;
        this.payouts = payouts;
    }

    @Override
    @Transactional
    public WalletEntry append(WalletEntry entry) {
        return entries.saveAndFlush(WalletEntryEntity.fromDomain(entry)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean entryExists(String referenceType, UUID referenceId, WalletEntry.Type type) {
        return entries.existsByReferenceTypeAndReferenceIdAndType(referenceType, referenceId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balance(UUID travelPartnerId) {
        return entries.balance(travelPartnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletEntry> findEntries(UUID travelPartnerId, int limit) {
        return entries.findByTravelPartnerIdOrderByCreatedAtDesc(
                        travelPartnerId, PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream().map(WalletEntryEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public Payout createPayout(Payout payout) {
        return payouts.saveAndFlush(PayoutEntity.fromDomain(payout)).toDomain();
    }

    @Override
    @Transactional
    public Payout updatePayout(Payout payout) {
        PayoutEntity entity =
                payouts.findById(payout.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Payout not found: " + payout.getId()));

        entity.updateFromDomain(payout);
        payouts.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payout> findPayoutById(UUID id) {
        return payouts.findById(id).map(PayoutEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> findPayoutsByPartner(UUID travelPartnerId) {
        return payouts.findByTravelPartnerIdOrderByCreatedAtDesc(travelPartnerId).stream()
                .map(PayoutEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payout> findPayoutsByStatus(Payout.Status status, int limit) {
        return payouts.findByStatusOrderByCreatedAtAsc(
                        status, PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream().map(PayoutEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumOpenPayoutRequests(UUID travelPartnerId) {
        return payouts.sumOpenRequests(travelPartnerId);
    }

    @Override
    @Transactional
    public void lockPartner(UUID travelPartnerId) {
        entityManager
                .createNativeQuery("select id from travel_partners where id = :id for update")
                .setParameter("id", travelPartnerId)
                .getResultList();
    }
}
