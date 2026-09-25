package com.yocabs.api.modules.notification.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository
        extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    List<NotificationEntity> findByRecipientUserIdAndReadAtIsNullOrderByCreatedAtDesc(
            UUID recipientUserId,
            Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now "
            + "where n.id = :id and n.recipientUserId = :userId and n.readAt is null")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now "
            + "where n.recipientUserId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);

    boolean existsByIdAndRecipientUserId(UUID id, UUID recipientUserId);
}
