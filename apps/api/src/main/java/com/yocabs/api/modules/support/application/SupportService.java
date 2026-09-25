package com.yocabs.api.modules.support.application;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.support.domain.SupportMessage;
import com.yocabs.api.modules.support.domain.SupportRepository;
import com.yocabs.api.modules.support.domain.SupportTicket;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Help & support for tourists, partners and drivers; worked by admins. */
@Service
public class SupportService {

    private static final String TICKET = "SUPPORT_TICKET";

    private final SupportRepository support;
    private final BookingService bookingService;
    private final UserAccountRepository users;
    private final AuditService auditService;
    private final ApplicationEventPublisher events;

    public SupportService(
            SupportRepository support,
            BookingService bookingService,
            UserAccountRepository users,
            AuditService auditService,
            ApplicationEventPublisher events
    ) {
        this.support = support;
        this.bookingService = bookingService;
        this.users = users;
        this.auditService = auditService;
        this.events = events;
    }

    @Transactional
    public TicketView open(
            Actor actor,
            SupportTicket.Category category,
            String subject,
            String description,
            UUID bookingId
    ) {
        if (actor.isAdmin()) {
            throw new AccessDeniedException("Administrators do not raise tickets");
        }

        if (bookingId != null) {
            // Throws when the booking does not exist or is not the caller's.
            bookingService.get(actor, bookingId);
        }

        SupportTicket ticket = SupportTicket.open(actor.userId(), actor.role(), category, subject, bookingId);
        SupportMessage first = SupportMessage.of(ticket.getId(), actor.userId(), actor.role(), description);

        SupportTicket saved = support.createTicket(ticket, first);

        notifyAdmins(saved);

        return new TicketView(saved, List.of(first));
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> listMine(Actor actor, int limit) {
        return support.findByCreator(actor.userId(), limit);
    }

    @Transactional(readOnly = true)
    public TicketView get(Actor actor, UUID ticketId) {

        SupportTicket ticket = load(ticketId);
        requireAccess(actor, ticket);

        return new TicketView(ticket, support.findMessages(ticketId));
    }

    @Transactional
    public TicketView reply(Actor actor, UUID ticketId, String body) {

        SupportTicket ticket = load(ticketId);
        requireAccess(actor, ticket);

        if (!ticket.acceptsMessages()) {
            throw new IllegalStateException("This ticket is closed");
        }

        support.appendMessage(SupportMessage.of(ticketId, actor.userId(), actor.role(), body));

        if (actor.isAdmin()) {
            if (ticket.getAssignedAdminId() == null) {
                ticket.assignTo(actor.userId());
            } else {
                ticket.touch();
            }

            events.publishEvent(
                    NotificationRequested.toUser(
                            ticket.getCreatedBy(), "SUPPORT_REPLY", "Support replied",
                            "Support replied to \"" + ticket.getSubject() + "\".", TICKET, ticketId
                    )
            );
        } else {
            if (ticket.getStatus() == SupportTicket.Status.RESOLVED) {
                ticket.reopen();
            } else {
                ticket.touch();
            }

            if (ticket.getAssignedAdminId() != null) {
                events.publishEvent(
                        NotificationRequested.toUser(
                                ticket.getAssignedAdminId(), "SUPPORT_MESSAGE", "New message on a ticket",
                                "New message on \"" + ticket.getSubject() + "\".", TICKET, ticketId
                        )
                );
            }
        }

        SupportTicket saved = support.updateTicket(ticket);

        return new TicketView(saved, support.findMessages(ticketId));
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> listForAdmin(Actor actor, SupportTicket.Status status, int limit) {
        actor.requireAdmin();
        return support.findByStatus(status, limit);
    }

    @Transactional
    public SupportTicket assignToMe(Actor actor, UUID ticketId) {

        actor.requireAdmin();

        SupportTicket ticket = load(ticketId);
        ticket.assignTo(actor.userId());
        SupportTicket saved = support.updateTicket(ticket);

        auditService.record(actor, "SUPPORT_TICKET_ASSIGNED", TICKET, ticketId, saved.getSubject());
        return saved;
    }

    @Transactional
    public SupportTicket resolve(Actor actor, UUID ticketId) {

        actor.requireAdmin();

        SupportTicket ticket = load(ticketId);
        ticket.resolve();
        SupportTicket saved = support.updateTicket(ticket);

        auditService.record(actor, "SUPPORT_TICKET_RESOLVED", TICKET, ticketId, saved.getSubject());

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getCreatedBy(), "SUPPORT_RESOLVED", "Your issue was resolved",
                        "\"" + saved.getSubject() + "\" was marked resolved. Reply if it is not.",
                        TICKET, ticketId
                )
        );
        return saved;
    }

    @Transactional
    public SupportTicket close(Actor actor, UUID ticketId) {

        actor.requireAdmin();

        SupportTicket ticket = load(ticketId);
        ticket.close();
        SupportTicket saved = support.updateTicket(ticket);

        auditService.record(actor, "SUPPORT_TICKET_CLOSED", TICKET, ticketId, saved.getSubject());
        return saved;
    }

    private void notifyAdmins(SupportTicket ticket) {
        for (Role role : new Role[]{Role.ADMIN, Role.SUPER_ADMIN}) {
            for (UserAccount admin : users.findByRole(role)) {
                if (admin.isActive()) {
                    events.publishEvent(
                            NotificationRequested.toUser(
                                    admin.getId(), "SUPPORT_TICKET_OPENED", "New support ticket",
                                    ticket.getCategory() + ": " + ticket.getSubject(), TICKET, ticket.getId()
                            )
                    );
                }
            }
        }
    }

    private void requireAccess(Actor actor, SupportTicket ticket) {
        if (actor.isAdmin() || actor.userId().equals(ticket.getCreatedBy())) {
            return;
        }
        throw new AccessDeniedException("Not your ticket");
    }

    private SupportTicket load(UUID ticketId) {
        return support.findTicket(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
    }

    public record TicketView(SupportTicket ticket, List<SupportMessage> messages) {
    }
}
