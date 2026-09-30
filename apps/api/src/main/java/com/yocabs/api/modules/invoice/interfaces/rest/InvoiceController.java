package com.yocabs.api.modules.invoice.interfaces.rest;

import com.yocabs.api.modules.invoice.application.InvoiceService;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

@RestController
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /** Authenticated: asks for a link the app can hand to a browser or a share sheet. */
    @PostMapping("/api/v1/bookings/{bookingId}/invoice-link")
    public InvoiceLinkResponse createLink(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        InvoiceService.Link link = invoiceService.createLink(actor, bookingId);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/bookings/invoice")
                .queryParam("token", link.token())
                .build()
                .toUriString();

        return new InvoiceLinkResponse(url, link.expiresAt());
    }

    /**
     * Public: opened directly by a browser (a share sheet, a "view bill" tap), so it carries no
     * Authorization header. The token is the authorisation instead, and it expires quickly.
     */
    @GetMapping("/api/v1/bookings/invoice")
    public ResponseEntity<byte[]> view(@RequestParam String token) {

        byte[] pdf = invoiceService.renderForToken(token);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("yocabs-invoice.pdf").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(pdf);
    }

    public record InvoiceLinkResponse(String url, Instant expiresAt) {
    }
}
