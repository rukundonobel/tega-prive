-- TEGA PRIVÉ V6: complaints, notifications, reviews, audit_logs

-- ─────────────────────────────────────────────
-- REVIEWS (passenger rates driver after completed journey)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id           UUID         NOT NULL PRIMARY KEY,
    booking_id   UUID         NOT NULL UNIQUE REFERENCES bookings (id),
    reviewer_id  UUID         NOT NULL REFERENCES users (id),
    driver_id    UUID         NOT NULL REFERENCES drivers (id),
    rating       SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    moderated    BOOLEAN      NOT NULL DEFAULT FALSE,
    hidden       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_reviews_driver   ON reviews (driver_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer ON reviews (reviewer_id);

-- ─────────────────────────────────────────────
-- NOTIFICATIONS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(64)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    read_at    TIMESTAMP WITH TIME ZONE,
    payload    JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user   ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;

-- ─────────────────────────────────────────────
-- COMPLAINTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS complaints (
    id              UUID         NOT NULL PRIMARY KEY,
    complainant_id  UUID         NOT NULL REFERENCES users (id),
    subject_type    VARCHAR(32)  NOT NULL,  -- USER | DRIVER | BOOKING | JOURNEY
    subject_id      UUID         NOT NULL,
    category        VARCHAR(64)  NOT NULL,
    description     TEXT         NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    -- OPEN | UNDER_REVIEW | RESOLVED | CLOSED
    resolution      TEXT,
    assigned_to     UUID         REFERENCES users (id),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_complaints_complainant ON complaints (complainant_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status      ON complaints (status);

-- ─────────────────────────────────────────────
-- AUDIT LOGS (financial and administrative actions)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id          UUID         NOT NULL PRIMARY KEY,
    actor_id    UUID         REFERENCES users (id),
    action      VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64)  NOT NULL,
    entity_id   UUID,
    description TEXT,
    ip_address  VARCHAR(64),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor  ON audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_time   ON audit_logs (created_at DESC);
