CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    created_by UUID NOT NULL,
    creator_role VARCHAR(30) NOT NULL,
    category VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    booking_id UUID,
    status VARCHAR(20) NOT NULL,
    assigned_admin_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_ticket_creator FOREIGN KEY (created_by) REFERENCES user_accounts(id),
    CONSTRAINT fk_ticket_admin FOREIGN KEY (assigned_admin_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_ticket_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT chk_ticket_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_ticket_category CHECK (
        category IN ('BOOKING_ISSUE', 'PAYMENT_ISSUE', 'DRIVER_ISSUE',
                     'CANCELLATION_REFUND', 'SAFETY', 'OTHER')
    )
);

CREATE INDEX idx_support_tickets_creator ON support_tickets(created_by, created_at DESC);
CREATE INDEX idx_support_tickets_status ON support_tickets(status, created_at);

-- Append-only conversation on a ticket.
CREATE TABLE support_messages (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL,
    author_id UUID NOT NULL,
    author_role VARCHAR(30) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_support_message_ticket
        FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_message_author FOREIGN KEY (author_id) REFERENCES user_accounts(id)
);

CREATE INDEX idx_support_messages_ticket ON support_messages(ticket_id, created_at);
