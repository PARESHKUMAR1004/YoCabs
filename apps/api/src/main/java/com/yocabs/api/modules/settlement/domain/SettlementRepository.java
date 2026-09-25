package com.yocabs.api.modules.settlement.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository {

    WalletEntry append(WalletEntry entry);

    boolean entryExists(String referenceType, UUID referenceId, WalletEntry.Type type);

    BigDecimal balance(UUID travelPartnerId);

    List<WalletEntry> findEntries(UUID travelPartnerId, int limit);

    Payout createPayout(Payout payout);

    Payout updatePayout(Payout payout);

    Optional<Payout> findPayoutById(UUID id);

    List<Payout> findPayoutsByPartner(UUID travelPartnerId);

    List<Payout> findPayoutsByStatus(Payout.Status status, int limit);

    BigDecimal sumOpenPayoutRequests(UUID travelPartnerId);

    /** Serialises ledger/payout operations for one partner until commit. */
    void lockPartner(UUID travelPartnerId);
}
