-- TEGA PRIVÉ V2: driver profiles, documents, vehicles, photos, vehicle documents

CREATE TABLE IF NOT EXISTS drivers (
    id                        UUID                        NOT NULL PRIMARY KEY,
    user_id                   UUID                        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    licence_number            VARCHAR(64),
    licence_expiry            DATE,
    profile_photo_url         VARCHAR(512),
    verification_status        VARCHAR(32)                 NOT NULL DEFAULT 'PENDING',
    is_verified               BOOLEAN                     NOT NULL DEFAULT FALSE,
    rating_avg                NUMERIC(4,2)                NOT NULL DEFAULT 0,
    rating_count              INTEGER                     NOT NULL DEFAULT 0,
    commission_wallet_balance NUMERIC(15,2)               NOT NULL DEFAULT 0,
    rejection_reason          VARCHAR(500),
    verified_at               TIMESTAMP WITH TIME ZONE,
    verified_by               UUID,
    created_at                TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at                TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_drivers_user ON drivers (user_id);
CREATE INDEX IF NOT EXISTS idx_drivers_verification_status ON drivers (verification_status);

CREATE TABLE IF NOT EXISTS driver_documents (
    id               UUID                        NOT NULL PRIMARY KEY,
    driver_id        UUID                        NOT NULL REFERENCES drivers (id) ON DELETE CASCADE,
    type             VARCHAR(32)                 NOT NULL,
    url              VARCHAR(512)                NOT NULL,
    public_id        VARCHAR(255),
    status           VARCHAR(32)                 NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    reviewed_by      UUID,
    reviewed_at      TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_driver_documents_driver ON driver_documents (driver_id);

CREATE TABLE IF NOT EXISTS vehicles (
    id                  UUID                        NOT NULL PRIMARY KEY,
    driver_id           UUID                        NOT NULL REFERENCES drivers (id) ON DELETE CASCADE,
    make                VARCHAR(64)                 NOT NULL,
    model               VARCHAR(64)                 NOT NULL,
    manufacturing_year  INTEGER,
    colour              VARCHAR(32)                 NOT NULL,
    registration_number VARCHAR(32)                 NOT NULL,
    category            VARCHAR(32)                 NOT NULL,
    seat_capacity       INTEGER                     NOT NULL,
    luggage_capacity    INTEGER                     NOT NULL,
    has_ac              BOOLEAN                      NOT NULL DEFAULT FALSE,
    description         TEXT,
    approval_status     VARCHAR(32)                 NOT NULL DEFAULT 'DRAFT',
    rejection_reason    VARCHAR(500),
    approved_at         TIMESTAMP WITH TIME ZONE,
    approved_by         UUID,
    created_at          TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_vehicles_registration UNIQUE (registration_number)
);
CREATE INDEX IF NOT EXISTS idx_vehicles_driver ON vehicles (driver_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_approval_status ON vehicles (approval_status);

CREATE TABLE IF NOT EXISTS vehicle_photos (
    id          UUID                        NOT NULL PRIMARY KEY,
    vehicle_id  UUID                        NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    url         VARCHAR(512)                NOT NULL,
    public_id   VARCHAR(255),
    position    INTEGER                     NOT NULL DEFAULT 0,
    is_primary  BOOLEAN                      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_vehicle_photos_vehicle ON vehicle_photos (vehicle_id);

CREATE TABLE IF NOT EXISTS vehicle_documents (
    id               UUID                        NOT NULL PRIMARY KEY,
    vehicle_id       UUID                        NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    type             VARCHAR(32)                 NOT NULL,
    url               VARCHAR(512)                NOT NULL,
    public_id        VARCHAR(255),
    status           VARCHAR(32)                 NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    reviewed_by      UUID,
    reviewed_at      TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE     NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_vehicle_documents_vehicle ON vehicle_documents (vehicle_id);
