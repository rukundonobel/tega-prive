-- TEGA PRIVÉ V3: locations, journeys, seat inventory, bookings
-- All money stored as NUMERIC(15,2). Overbooking prevention via DB constraints + row locking.

-- ─────────────────────────────────────────────
-- LOCATIONS  (admin-managed destinations)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS locations (
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    region     VARCHAR(128),
    country    VARCHAR(64)  NOT NULL DEFAULT 'Rwanda',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_locations_name_country UNIQUE (name, country)
);

-- Seed Rwandan destinations
INSERT INTO locations (id, name, region, country) VALUES
  (gen_random_uuid(), 'Kigali',    'Kigali City',       'Rwanda'),
  (gen_random_uuid(), 'Musanze',   'Northern Province',  'Rwanda'),
  (gen_random_uuid(), 'Rubavu',    'Western Province',   'Rwanda'),
  (gen_random_uuid(), 'Huye',      'Southern Province',  'Rwanda'),
  (gen_random_uuid(), 'Nyagatare', 'Eastern Province',   'Rwanda'),
  (gen_random_uuid(), 'Rusizi',    'Western Province',   'Rwanda'),
  (gen_random_uuid(), 'Karongi',   'Western Province',   'Rwanda'),
  (gen_random_uuid(), 'Muhanga',   'Southern Province',  'Rwanda'),
  (gen_random_uuid(), 'Rwamagana', 'Eastern Province',   'Rwanda'),
  (gen_random_uuid(), 'Kayonza',   'Eastern Province',   'Rwanda'),
  (gen_random_uuid(), 'Ngoma',     'Eastern Province',   'Rwanda'),
  (gen_random_uuid(), 'Bugesera',  'Eastern Province',   'Rwanda')
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────────
-- JOURNEYS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS journeys (
    id                   UUID            NOT NULL PRIMARY KEY,
    driver_id            UUID            NOT NULL REFERENCES drivers (id),
    vehicle_id           UUID            NOT NULL REFERENCES vehicles (id),
    origin_id            UUID            NOT NULL REFERENCES locations (id),
    destination_id       UUID            NOT NULL REFERENCES locations (id),
    departure_time       TIMESTAMP WITH TIME ZONE NOT NULL,
    estimated_arrival    TIMESTAMP WITH TIME ZONE,
    price_per_seat       NUMERIC(15,2)   NOT NULL,
    total_seats          INTEGER         NOT NULL,
    available_seats      INTEGER         NOT NULL,
    status               VARCHAR(32)     NOT NULL DEFAULT 'SCHEDULED',
    pickup_instructions  TEXT,
    dropoff_instructions TEXT,
    notes                TEXT,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_journeys_seats        CHECK (available_seats >= 0),
    CONSTRAINT chk_journeys_total_seats  CHECK (total_seats >= 1),
    CONSTRAINT chk_journeys_capacity     CHECK (available_seats <= total_seats),
    CONSTRAINT chk_journeys_price        CHECK (price_per_seat >= 0)
);
CREATE INDEX IF NOT EXISTS idx_journeys_driver      ON journeys (driver_id);
CREATE INDEX IF NOT EXISTS idx_journeys_status      ON journeys (status);
CREATE INDEX IF NOT EXISTS idx_journeys_departure   ON journeys (departure_time);
CREATE INDEX IF NOT EXISTS idx_journeys_origin      ON journeys (origin_id);
CREATE INDEX IF NOT EXISTS idx_journeys_destination ON journeys (destination_id);

-- ─────────────────────────────────────────────
-- BOOKINGS
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id               UUID          NOT NULL PRIMARY KEY,
    journey_id       UUID          NOT NULL REFERENCES journeys (id),
    passenger_id     UUID          NOT NULL REFERENCES users (id),
    seats_booked     INTEGER       NOT NULL DEFAULT 1,
    status           VARCHAR(32)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount     NUMERIC(15,2) NOT NULL,
    service_fee      NUMERIC(15,2) NOT NULL DEFAULT 0,
    gross_fare       NUMERIC(15,2) NOT NULL,
    payment_method   VARCHAR(32),
    expires_at       TIMESTAMP WITH TIME ZONE,
    confirmed_at     TIMESTAMP WITH TIME ZONE,
    cancelled_at     TIMESTAMP WITH TIME ZONE,
    cancel_reason    VARCHAR(500),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_bookings_seats CHECK (seats_booked >= 1),
    CONSTRAINT chk_bookings_amount CHECK (total_amount >= 0)
);
CREATE INDEX IF NOT EXISTS idx_bookings_journey   ON bookings (journey_id);
CREATE INDEX IF NOT EXISTS idx_bookings_passenger ON bookings (passenger_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status    ON bookings (status);

-- Prevent a passenger from having two active bookings on the same journey.
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_booking_per_passenger_journey
    ON bookings (journey_id, passenger_id)
    WHERE status NOT IN ('CANCELLED', 'REFUNDED', 'EXPIRED');
