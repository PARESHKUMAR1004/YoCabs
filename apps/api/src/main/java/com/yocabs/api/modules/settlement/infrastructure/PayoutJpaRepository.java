package com.yocabs.api.modules.settlement.infrastructure;

import com.yocabs.api.modules.settlement.domain.Payout;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PayoutJpaRepository
        extends JpaRepository<PayoutEntity, UUID> {

    List<PayoutEntity> findByTravelPartnerIdOrderByCreatedAtDesc(UUID travelPartnerId);

    List<PayoutEntity> findByStatusOrderByCreatedAtAsc(Payout.Status status, Pageable pageable);

    @Query("select coalesce(sum(p.amount), 0) from PayoutEntity p "
            + "where p.travelPartnerId = :partnerId "
            + "and p.status = com.yocabs.api.modules.settlement.domain.Payout.Status.REQUESTED")
    BigDecimal sumOpenRequests(@Param("partnerId") UUID partnerId);
}
