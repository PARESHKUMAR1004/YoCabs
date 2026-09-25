package com.yocabs.api.modules.document.interfaces.rest;

import com.yocabs.api.modules.document.application.DocumentService;
import com.yocabs.api.modules.document.application.DocumentService.Content;
import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/api/v1/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @CurrentActor Actor actor,
            @RequestParam Document.OwnerType ownerType,
            @RequestParam UUID ownerId,
            @RequestParam Document.Type documentType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return DocumentResponse.from(
                documentService.upload(
                        actor, ownerType, ownerId, documentType,
                        file.getOriginalFilename(), file.getContentType(), file.getBytes(), expiryDate
                )
        );
    }

    @GetMapping("/api/v1/documents")
    public List<DocumentResponse> list(
            @CurrentActor Actor actor,
            @RequestParam Document.OwnerType ownerType,
            @RequestParam UUID ownerId
    ) {
        return documentService.list(actor, ownerType, ownerId).stream()
                .map(DocumentResponse::from).toList();
    }

    @GetMapping("/api/v1/documents/{documentId}/content")
    public ResponseEntity<byte[]> download(
            @CurrentActor Actor actor,
            @PathVariable UUID documentId
    ) {
        Content content = documentService.download(actor, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(content.filename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(content.bytes());
    }

    @GetMapping("/api/v1/admin/documents")
    public List<DocumentResponse> listByStatus(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "PENDING") Document.Status status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return documentService.listByStatus(actor, status, limit).stream()
                .map(DocumentResponse::from).toList();
    }

    @PostMapping("/api/v1/admin/documents/{documentId}/approve")
    public DocumentResponse approve(
            @CurrentActor Actor actor,
            @PathVariable UUID documentId
    ) {
        return DocumentResponse.from(documentService.approve(actor, documentId));
    }

    @PostMapping("/api/v1/admin/documents/{documentId}/reject")
    public DocumentResponse reject(
            @CurrentActor Actor actor,
            @PathVariable UUID documentId,
            @RequestBody RejectDocumentRequest request
    ) {
        return DocumentResponse.from(documentService.reject(actor, documentId, request.reason()));
    }

    public record RejectDocumentRequest(String reason) {
    }

    public record DocumentResponse(
            UUID id,
            Document.OwnerType ownerType,
            UUID ownerId,
            Document.Type documentType,
            String filename,
            String contentType,
            long sizeBytes,
            Document.Status status,
            String rejectionReason,
            LocalDate expiryDate,
            Instant createdAt
    ) {

        static DocumentResponse from(Document document) {
            return new DocumentResponse(
                    document.getId(), document.getOwnerType(), document.getOwnerId(),
                    document.getType(), document.getOriginalFilename(), document.getContentType(),
                    document.getSizeBytes(), document.getStatus(), document.getRejectionReason(),
                    document.getExpiryDate(), document.getCreatedAt()
            );
        }
    }
}
