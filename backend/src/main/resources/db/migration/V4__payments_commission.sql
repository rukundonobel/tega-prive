-- TEGA PRIVÉ V4: payments, webhooks, commission rules, commission transactions,
--                driver earnings ledger, driver payouts, refunds, platform settings

-- ─────────────────────────────────────────────
-- PAYMENTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payments (
    id                 UUID          NOT NULL PRIMARY KEY,
    booking_id         UUID          NOT NULL REFERENCES bookings (id),
    provider           VARCHAR(32)   NOT NULL,           -- MTN_MOMO | AIRTEL | CASH
    internal_reference VARCHAR(128)  NOT NULL UNIQUE,    -- our idempotency key
    provider_reference VARCHAR(255),                     -- provider's transaction id
    amount             NUMERIC(15,2) NOT NULL,
    currency           VARCHAR(8)    NOT NULL DEFAULT 'RWF',
    phone_number       VARCHAR(20)   NOT NULL,
    status             VARCHAR(32)   NOT NULL DEFAULT 'INITIATED',
    -- INITIATED | PENDING | SUCCESSFUL | FAILED | REFUNDED
    verified           BOOLEAN       NOT NULL DEFAULT FALSE, -- backend-verified only
    provider_payload   JSONB,                            -- raw provider response
    failure_reason     VARCHAR(255),
    initiated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    verified_at        TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payments_booking   ON payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON payments (internal_reference);
CREATE INDEX IF NOT EXISTS idx_payments_status    ON payments (status);

-- ─────────────────────────────────────────────
-- PAYMENT WEBHOOKS (audit trail of raw callbacks)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment_webhooks (
    id                 UUID          NOT NULL PRIMARY KEY,
    provider           VARCHAR(32)   NOT NULL,
    provider_reference VARCHAR(255),
    internal_reference VARCHAR(128),
    raw_payload        JSONB         NOT NULL,
    signature_valid    BOOLEAN       NOT NULL DEFAULT FALSE,
    processed          BOOLEAN       NOT NULL DEFAULT FALSE,
    processing_error   VARCHAR(500),
    received_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    processed_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_webhooks_reference ON payment_webhooks (internal_reference);

-- ─────────────────────────────────────────────
-- COMMISSION RULES (admin-managed, append-only versioning)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS commission_rules (
    id                     UUID          NOT NULL PRIMARY KEY,
    driver_commission_pct  NUMERIC(5,4)  NOT NULL,  -- e.g. 0.1000 = 10%
    passenger_service_fee_pct NUMERIC(5,4) NOT NULL,-- e.g. 0.0300 = 3%
    min_service_fee        NUMERIC(15,2) NOT NULL DEFAULT 0,
    max_service_fee        NUMERIC(15,2),           -- NULL = no cap
    fixed_booking_fee      NUMERIC(15,2) NOT NULL DEFAULT 0,
    cancellation_fee_pct   NUMERIC(5,4)  NOT NULL DEFAULT 0,
    effective_from         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by             UUID,
    note                   VARCHAR(500),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Default commission rule
INSERT INTO commission_rules (
    id, driver_commission_pct, passenger_service_fee_pct,
    min_service_fee, fixed_booking_fee, cancellation_fee_pct, note
) VALUES (
    gen_random_uuid(), 0.1000, 0.0300,
    0, 0, 0.0000, 'Default rule — 10% driver commission, 3% passenger service fee'
);

-- ─────────────────────────────────────────────
-- COMMISSION TRANSACTIONS (immutable record per paid booking)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS commission_transactions (
    id                   UUID          NOT NULL PRIMARY KEY,
    booking_id           UUID          NOT NULL UNIQUE REFERENCES bookings (id),
    payment_id           UUID          NOT NULL REFERENCES payments (id),
    commission_rule_id   UUID          NOT NULL REFERENCES commission_rules (id),
    gross_fare           NUMERIC(15,2) NOT NULL,
    driver_commission    NUMERIC(15,2) NOT NULL,  -- amount platform takes from driver
    passenger_service_fee NUMERIC(15,2) NOT NULL, -- amount platform takes from passenger
    driver_net           NUMERIC(15,2) NOT NULL,  -- gross_fare - driver_commission
    platform_revenue     NUMERIC(15,2) NOT NULL,  -- driver_commission + passenger_service_fee
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_commission_tx_booking ON commission_transactions (booking_id);

-- ─────────────────────────────────────────────
-- DRIVER EARNINGS LEDGER
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS driver_earnings (
    id               UUID          NOT NULL PRIMARY KEY,
    driver_id        UUID          NOT NULL REFERENCES drivers (id),
    booking_id       UUID          NOT NULL UNIQUE REFERENCES bookings (id),
    gross_fare       NUMERIC(15,2) NOT NULL,
    commission       NUMERIC(15,2) NOT NULL,     -- amount deducted from driver
    net_earnings     NUMERIC(15,2) NOT NULL,     -- what driver actually earns
    settlement_status VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    -- PENDING | PAID | HELD
    payout_id        UUID,                        -- FK → driver_payouts (set when settled)
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_driver_earnings_driver   ON driver_earnings (driver_id);
CREATE INDEX IF NOT EXISTS idx_driver_earnings_settlement ON driver_earnings (settlement_status);

-- ─────────────────────────────────────────────
-- DRIVER PAYOUTS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS driver_payouts (
    id                 UUID          NOT NULL PRIMARY KEY,
    driver_id          UUID          NOT NULL REFERENCES drivers (id),
    amount             NUMERIC(15,2) NOT NULL,
    provider           VARCHAR(32),               -- MTN_MOMO | AIRTEL | MANUAL
    provider_reference VARCHAR(255),
    phone_number       VARCHAR(20),
    status             VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    -- PENDING | PROCESSING | PAID | FAILED
    initiated_by       UUID,
    failure_reason     VARCHAR(255),
    paid_at            TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_driver_payouts_driver ON driver_payouts (driver_id);

-- ─────────────────────────────────────────────
-- REFUNDS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refunds (
    id               UUID          NOT NULL PRIMARY KEY,
    booking_id       UUID          NOT NULL REFERENCES bookings (id),
    payment_id       UUID          NOT NULL REFERENCES payments (id),
    amount           NUMERIC(15,2) NOT NULL,
    reason           VARCHAR(500),
    status           VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    -- PENDING | PROCESSED | FAILED
    initiated_by     UUID,
    provider_reference VARCHAR(255),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────
-- PLATFORM SETTINGS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS platform_settings (
    key         VARCHAR(128) NOT NULL PRIMARY KEY,
    value       TEXT         NOT NULL,
    description VARCHAR(500),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_by  UUID
);

INSERT INTO platform_settings (key, value, description) VALUES
  ('cash_payments_enabled',    'false',         'Allow cash payment option for bookings'),
  ('booking_workflow',         'INSTANT',        'INSTANT | DRIVER_APPROVAL'),
  ('max_cash_commission_debt', '50000',          'Max unpaid commission balance (RWF) before driver account is restricted'),
  ('payment_expiry_minutes',   '30',             'Minutes before an unpaid booking expires')
ON CONFLICT (key) DO NOTHING;
