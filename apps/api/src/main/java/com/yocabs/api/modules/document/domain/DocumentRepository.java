package com.yocabs.api.modules.document.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {

    Document create(Document document);

    Document update(Document document);

    void delete(UUID id);

    Optional<Document> findById(UUID id);

    List<Document> findByOwner(Document.OwnerType ownerType, UUID ownerId);

    List<Document> findByStatus(Document.Status status, int limit);

    List<Document> findByOwnersAndTypeAndStatus(
            Document.OwnerType ownerType,
            Collection<UUID> ownerIds,
            Document.Type type,
            Document.Status status
    );

    boolean existsApproved(Document.OwnerType ownerType, UUID ownerId, Document.Type type);
}
