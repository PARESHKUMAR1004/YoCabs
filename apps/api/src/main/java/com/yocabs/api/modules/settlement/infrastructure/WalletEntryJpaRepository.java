package com.yocabs.api.modules.settlement.infrastructure;

import com.yocabs.api.modules.settlement.domain.WalletEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletEntryJpaRepository
        extends JpaRepository<WalletEntryEntity, UUID> {

    boolean existsByReferenceTypeAndReferenceIdAndType(
            String referenceType,
            UUID referenceId,
            WalletEntry.Type type
    );

    List<WalletEntryEntity> findByTravelPartnerIdOrderByCreatedAtDesc(UUID travelPartnerId, Pageable pageable);

    @Query("select coalesce(sum(case when e.type = com.yocabs.api.modules.settlement.domain.WalletEntry.Type.CREDIT "
            + "then e.amount else -e.amount end), 0) "
            + "from WalletEntryEntity e where e.travelPartnerId = :partnerId")
    BigDecimal balance(@Param("partnerId") UUID partnerId);
}
