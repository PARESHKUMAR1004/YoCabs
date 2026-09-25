package com.yocabs.api.modules.support.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportRepository {

    SupportTicket createTicket(SupportTicket ticket, SupportMessage firstMessage);

    SupportTicket updateTicket(SupportTicket ticket);

    Optional<SupportTicket> findTicket(UUID id);

    List<SupportTicket> findByCreator(UUID creatorId, int limit);

    /** A null status returns tickets of every status. */
    List<SupportTicket> findByStatus(SupportTicket.Status status, int limit);

    SupportMessage appendMessage(SupportMessage message);

    List<SupportMessage> findMessages(UUID ticketId);
}
