package com.yocabs.api.modules.support.infrastructure;

import com.yocabs.api.modules.support.domain.SupportTicket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SupportTicketJpaRepository extends JpaRepository<SupportTicketEntity, UUID> {

    List<SupportTicketEntity> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    List<SupportTicketEntity> findByStatusOrderByCreatedAtAsc(SupportTicket.Status status, Pageable pageable);

    List<SupportTicketEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

interface SupportMessageJpaRepository extends JpaRepository<SupportMessageEntity, UUID> {

    List<SupportMessageEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
