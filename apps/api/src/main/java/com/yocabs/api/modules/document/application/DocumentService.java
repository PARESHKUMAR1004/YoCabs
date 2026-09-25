package com.yocabs.api.modules.document.application;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.document.domain.DocumentRepository;
import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.modules.driver.domain.repository.DriverRepository;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Map<String, String> EXTENSIONS =
            Map.of("application/pdf", "pdf", "image/jpeg", "jpg", "image/png", "png");

    private final DocumentRepository documents;
    private final DocumentStorage storage;
    private final TravelPartnerRepository partners;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;
    private final long maxBytes;

    public DocumentService(
            DocumentRepository documents,
            DocumentStorage storage,
            TravelPartnerRepository partners,
            VehicleRepository vehicles,
            DriverRepository drivers,
            AuditService auditService,
            ApplicationEventPublisher events,
            @Value("${yocabs.storage.max-document-bytes:5242880}") long maxBytes
    ) {
        this.documents = documents;
        this.storage = storage;
        this.partners = partners;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.auditService = auditService;
        this.events = events;
        this.maxBytes = maxBytes;
    }

    @Transactional
    public Document upload(
            Actor actor,
            Document.OwnerType ownerType,
            UUID ownerId,
            Document.Type type,
            String filename,
            String contentType,
            byte[] content,
            LocalDate expiryDate
    ) {
        requireOwnerAccess(actor, ownerType, ownerId);

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("The document is empty");
        }

        if (content.length > maxBytes) {
            throw new IllegalArgumentException(
                    "The document exceeds the maximum size of " + (maxBytes / 1024 / 1024) + " MB"
            );
        }

        if (type == Document.Type.VEHICLE_PHOTO) {
            if (ownerType != Document.OwnerType.VEHICLE) {
                throw new IllegalArgumentException("Vehicle photos must be attached to a vehicle");
            }
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new IllegalArgumentException("Vehicle photos must be JPEG or PNG images");
            }
        }

        String extension = EXTENSIONS.get(contentType == null ? "" : contentType.toLowerCase());

        if (extension == null || !matchesSignature(content, extension)) {
            throw new IllegalArgumentException("Only PDF, JPEG and PNG documents are accepted");
        }

        String storageKey = ownerType.name().toLowerCase() + "/" + UUID.randomUUID() + "." + extension;

        storage.store(storageKey, content);

        return documents.create(
                Document.upload(
                        ownerType, ownerId, type, storageKey, sanitize(filename),
                        contentType.toLowerCase(), content.length, expiryDate, actor.userId()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Document> list(Actor actor, Document.OwnerType ownerType, UUID ownerId) {
        requireOwnerAccess(actor, ownerType, ownerId);
        return documents.findByOwner(ownerType, ownerId);
    }

    @Transactional(readOnly = true)
    public Content download(Actor actor, UUID documentId) {

        Document document = load(documentId);
        requireOwnerAccess(actor, document.getOwnerType(), document.getOwnerId());

        return new Content(
                document.getOriginalFilename(),
                document.getContentType(),
                storage.load(document.getStorageKey())
        );
    }

    /** Approved vehicle photos are shown to tourists; nothing else is served this way. */
    @Transactional(readOnly = true)
    public Content downloadApprovedVehiclePhoto(UUID vehicleId, UUID documentId) {

        Document document = load(documentId);

        if (document.getType() != Document.Type.VEHICLE_PHOTO
                || document.getOwnerType() != Document.OwnerType.VEHICLE
                || !document.getOwnerId().equals(vehicleId)
                || document.getStatus() != Document.Status.APPROVED) {
            throw new ResourceNotFoundException("Photo not found");
        }

        return new Content(
                document.getOriginalFilename(),
                document.getContentType(),
                storage.load(document.getStorageKey())
        );
    }

    @Transactional(readOnly = true)
    public List<Document> listByStatus(Actor actor, Document.Status status, int limit) {
        actor.requireAdmin();
        return documents.findByStatus(status, limit);
    }

    @Transactional
    public Document approve(Actor actor, UUID documentId) {

        actor.requireAdmin();

        Document document = load(documentId);
        document.approve(actor.userId());
        Document saved = documents.update(document);

        auditService.record(actor, "DOCUMENT_APPROVED", "DOCUMENT", saved.getId(),
                saved.getOwnerType() + ":" + saved.getOwnerId() + " " + saved.getType());

        notifyUploader(saved, "DOCUMENT_APPROVED", "Document approved",
                "Your " + saved.getType() + " was approved.");

        return saved;
    }

    @Transactional
    public Document reject(Actor actor, UUID documentId, String reason) {

        actor.requireAdmin();

        Document document = load(documentId);
        document.reject(actor.userId(), reason);
        Document saved = documents.update(document);

        auditService.record(actor, "DOCUMENT_REJECTED", "DOCUMENT", saved.getId(),
                saved.getOwnerType() + ":" + saved.getOwnerId() + " " + saved.getType() + " - " + reason);

        notifyUploader(saved, "DOCUMENT_REJECTED", "Document rejected",
                "Your " + saved.getType() + " was rejected: " + saved.getRejectionReason());

        return saved;
    }

    @Transactional(readOnly = true)
    public boolean hasApproved(Document.OwnerType ownerType, UUID ownerId, Document.Type type) {
        return documents.existsApproved(ownerType, ownerId, type);
    }

    private void notifyUploader(Document document, String type, String title, String body) {
        events.publishEvent(
                NotificationRequested.toUser(
                        document.getUploadedBy(), type, title, body, "DOCUMENT", document.getId()
                )
        );
    }

    private void requireOwnerAccess(Actor actor, Document.OwnerType ownerType, UUID ownerId) {

        if (ownerId == null || ownerType == null) {
            throw new IllegalArgumentException("Document owner is required");
        }

        switch (ownerType) {
            case PARTNER -> {
                partners.findById(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Travel partner not found: " + ownerId));
                actor.requirePartnerAccess(ownerId);
            }
            case VEHICLE -> {
                Vehicle vehicle =
                        vehicles.findById(ownerId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException("Vehicle not found: " + ownerId));
                actor.requirePartnerAccess(vehicle.getTravelPartnerId());
            }
            case DRIVER -> {
                Driver driver =
                        drivers.findById(ownerId)
                                .orElseThrow(() ->
                                        new ResourceNotFoundException("Driver not found: " + ownerId));

                if (actor.role() == Role.DRIVER && actor.userId().equals(ownerId)) {
                    return;
                }
                actor.requirePartnerAccess(driver.getTravelPartnerId());
            }
        }
    }

    private Document load(UUID documentId) {
        return documents.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    /** Guards against a client mislabelling content: checks the file's magic bytes. */
    private static boolean matchesSignature(byte[] content, String extension) {
        return switch (extension) {
            case "pdf" -> startsWith(content, 0x25, 0x50, 0x44, 0x46);
            case "png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47);
            case "jpg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String name = filename.replaceAll("[\\\\/\\r\\n\"]", "_").trim();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    public record Content(String filename, String contentType, byte[] bytes) {
    }
}
