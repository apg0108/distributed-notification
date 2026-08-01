CREATE TABLE notifications (
    id            UUID PRIMARY KEY,
    recipient     VARCHAR(255) NOT NULL,
    channel       VARCHAR(20)  NOT NULL,
    subject       VARCHAR(255),
    body          TEXT         NOT NULL,
    priority      VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    metadata      JSONB,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER      NOT NULL DEFAULT 0,
    last_error    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at       TIMESTAMPTZ
);

CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_status_updated_at ON notifications (status, updated_at);
