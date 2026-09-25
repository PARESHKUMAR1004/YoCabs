CREATE TABLE travel_partners (
                                 id UUID PRIMARY KEY,

                                 name VARCHAR(200) NOT NULL,

                                 status VARCHAR(30) NOT NULL,

                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT chk_travel_partner_status
                                     CHECK (
                                         status IN (
                                                    'PENDING_APPROVAL',
                                                    'ACTIVE',
                                                    'SUSPENDED',
                                                    'INACTIVE'
                                             )
                                         )
);

CREATE INDEX idx_travel_partners_status
    ON travel_partners(status);