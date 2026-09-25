package com.yocabs.api.modules.document.infrastructure;

import com.yocabs.api.modules.document.domain.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentJpaRepository
        extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(
            Document.OwnerType ownerType,
            UUID ownerId
    );

    List<DocumentEntity> findByStatusOrderByCreatedAtAsc(Document.Status status, Pageable pageable);

    List<DocumentEntity> findByOwnerTypeAndOwnerIdInAndTypeAndStatusOrderByCreatedAtAsc(
            Document.OwnerType ownerType,
            Collection<UUID> ownerIds,
            Document.Type type,
            Document.Status status
    );

    boolean existsByOwnerTypeAndOwnerIdAndTypeAndStatus(
            Document.OwnerType ownerType,
            UUID ownerId,
            Document.Type type,
            Document.Status status
    );
}
