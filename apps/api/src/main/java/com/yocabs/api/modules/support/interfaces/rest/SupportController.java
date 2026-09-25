package com.yocabs.api.modules.support.interfaces.rest;

import com.yocabs.api.modules.support.application.SupportService;
import com.yocabs.api.modules.support.application.SupportService.TicketView;
import com.yocabs.api.modules.support.domain.SupportMessage;
import com.yocabs.api.modules.support.domain.SupportTicket;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/api/v1/support/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse open(
            @CurrentActor Actor actor,
            @RequestBody OpenTicketRequest request
    ) {
        return TicketResponse.from(
                supportService.open(
                        actor, request.category(), request.subject(), request.description(), request.bookingId()
                )
        );
    }

    @GetMapping("/api/v1/support/tickets")
    public List<TicketSummary> mine(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return supportService.listMine(actor, limit).stream().map(TicketSummary::from).toList();
    }

    @GetMapping("/api/v1/support/tickets/{ticketId}")
    public TicketResponse get(@CurrentActor Actor actor, @PathVariable UUID ticketId) {
        return TicketResponse.from(supportService.get(actor, ticketId));
    }

    @PostMapping("/api/v1/support/tickets/{ticketId}/messages")
    public TicketResponse reply(
            @CurrentActor Actor actor,
            @PathVariable UUID ticketId,
            @RequestBody ReplyRequest request
    ) {
        return TicketResponse.from(supportService.reply(actor, ticketId, request.body()));
    }

    @GetMapping("/api/v1/admin/support/tickets")
    public List<TicketSummary> queue(
            @CurrentActor Actor actor,
            @RequestParam(required = false) SupportTicket.Status status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return supportService.listForAdmin(actor, status, limit).stream().map(TicketSummary::from).toList();
    }

    @PostMapping("/api/v1/admin/support/tickets/{ticketId}/assign")
    public TicketSummary assign(@CurrentActor Actor actor, @PathVariable UUID ticketId) {
        return TicketSummary.from(supportService.assignToMe(actor, ticketId));
    }

    @PostMapping("/api/v1/admin/support/tickets/{ticketId}/resolve")
    public TicketSummary resolve(@CurrentActor Actor actor, @PathVariable UUID ticketId) {
        return TicketSummary.from(supportService.resolve(actor, ticketId));
    }

    @PostMapping("/api/v1/admin/support/tickets/{ticketId}/close")
    public TicketSummary close(@CurrentActor Actor actor, @PathVariable UUID ticketId) {
        return TicketSummary.from(supportService.close(actor, ticketId));
    }

    public record OpenTicketRequest(
            SupportTicket.Category category,
            String subject,
            String description,
            UUID bookingId
    ) {
    }

    public record ReplyRequest(String body) {
    }

    public record TicketSummary(
            UUID id,
            SupportTicket.Category category,
            String subject,
            SupportTicket.Status status,
            UUID bookingId,
            UUID createdBy,
            String creatorRole,
            UUID assignedAdminId,
            Instant createdAt,
            Instant updatedAt
    ) {

        static TicketSummary from(SupportTicket ticket) {
            return new TicketSummary(
                    ticket.getId(), ticket.getCategory(), ticket.getSubject(), ticket.getStatus(),
                    ticket.getBookingId(), ticket.getCreatedBy(), ticket.getCreatorRole().name(),
                    ticket.getAssignedAdminId(), ticket.getCreatedAt(), ticket.getUpdatedAt()
            );
        }
    }

    public record MessageResponse(UUID id, UUID authorId, String authorRole, String body, Instant createdAt) {

        static MessageResponse from(SupportMessage message) {
            return new MessageResponse(
                    message.id(), message.authorId(), message.authorRole().name(),
                    message.body(), message.createdAt()
            );
        }
    }

    public record TicketResponse(TicketSummary ticket, List<MessageResponse> messages) {

        static TicketResponse from(TicketView view) {
            return new TicketResponse(
                    TicketSummary.from(view.ticket()),
                    view.messages().stream().map(MessageResponse::from).toList()
            );
        }
    }
}
