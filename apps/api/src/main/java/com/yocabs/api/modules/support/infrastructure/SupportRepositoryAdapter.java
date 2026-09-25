package com.yocabs.api.modules.support.infrastructure;

import com.yocabs.api.modules.support.domain.SupportMessage;
import com.yocabs.api.modules.support.domain.SupportRepository;
import com.yocabs.api.modules.support.domain.SupportTicket;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SupportRepositoryAdapter implements SupportRepository {

    private final SupportTicketJpaRepository tickets;
    private final SupportMessageJpaRepository messages;

    SupportRepositoryAdapter(SupportTicketJpaRepository tickets, SupportMessageJpaRepository messages) {
        this.tickets = tickets;
        this.messages = messages;
    }

    private static PageRequest page(int limit) {
        return PageRequest.of(0, Math.max(1, Math.min(limit, 200)));
    }

    @Override
    @Transactional
    public SupportTicket createTicket(SupportTicket ticket, SupportMessage firstMessage) {
        SupportTicket saved = tickets.saveAndFlush(SupportTicketEntity.fromDomain(ticket)).toDomain();
        messages.saveAndFlush(SupportMessageEntity.fromDomain(firstMessage));
        return saved;
    }

    @Override
    @Transactional
    public SupportTicket updateTicket(SupportTicket ticket) {
        SupportTicketEntity entity =
                tickets.findById(ticket.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Ticket not found: " + ticket.getId()));

        entity.apply(ticket);
        tickets.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicket> findTicket(UUID id) {
        return tickets.findById(id).map(SupportTicketEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findByCreator(UUID creatorId, int limit) {
        return tickets.findByCreatedByOrderByCreatedAtDesc(creatorId, page(limit)).stream()
                .map(SupportTicketEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> findByStatus(SupportTicket.Status status, int limit) {
        return (status == null
                ? tickets.findAllByOrderByCreatedAtDesc(page(limit))
                : tickets.findByStatusOrderByCreatedAtAsc(status, page(limit)))
                .stream().map(SupportTicketEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public SupportMessage appendMessage(SupportMessage message) {
        return messages.saveAndFlush(SupportMessageEntity.fromDomain(message)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportMessage> findMessages(UUID ticketId) {
        return messages.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(SupportMessageEntity::toDomain).toList();
    }
}
