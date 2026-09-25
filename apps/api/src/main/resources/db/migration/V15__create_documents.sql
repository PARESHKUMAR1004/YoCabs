CREATE TABLE documents (
    id UUID PRIMARY KEY,
    owner_type VARCHAR(20) NOT NULL,
    owner_id UUID NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    storage_key VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(500),
    expiry_date DATE,
    uploaded_by UUID NOT NULL,
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_document_owner_type CHECK (owner_type IN ('PARTNER', 'VEHICLE', 'DRIVER')),
    CONSTRAINT chk_document_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_document_size CHECK (size_bytes > 0),
    CONSTRAINT uq_document_storage_key UNIQUE (storage_key)
);

CREATE INDEX idx_documents_owner ON documents(owner_type, owner_id);
CREATE INDEX idx_documents_status ON documents(status);
