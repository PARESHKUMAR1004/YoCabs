package com.yocabs.api.modules.document.infrastructure;

import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.document.domain.DocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpa;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Document create(Document document) {
        return jpa.saveAndFlush(DocumentEntity.fromDomain(document)).toDomain();
    }

    @Override
    @Transactional
    public Document update(Document document) {
        DocumentEntity entity =
                jpa.findById(document.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Document not found: " + document.getId()));

        entity.updateFromDomain(document);
        jpa.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return jpa.findById(id).map(DocumentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByOwner(Document.OwnerType ownerType, UUID ownerId) {
        return jpa.findByOwnerTypeAndOwnerIdOrderByCreatedAtDesc(ownerType, ownerId).stream()
                .map(DocumentEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByStatus(Document.Status status, int limit) {
        return jpa.findByStatusOrderByCreatedAtAsc(
                        status, PageRequest.of(0, Math.max(1, Math.min(limit, 200))))
                .stream().map(DocumentEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findByOwnersAndTypeAndStatus(
            Document.OwnerType ownerType,
            Collection<UUID> ownerIds,
            Document.Type type,
            Document.Status status
    ) {
        if (ownerIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByOwnerTypeAndOwnerIdInAndTypeAndStatusOrderByCreatedAtAsc(
                        ownerType, ownerIds, type, status)
                .stream().map(DocumentEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsApproved(Document.OwnerType ownerType, UUID ownerId, Document.Type type) {
        return jpa.existsByOwnerTypeAndOwnerIdAndTypeAndStatus(
                ownerType, ownerId, type, Document.Status.APPROVED
        );
    }
}
