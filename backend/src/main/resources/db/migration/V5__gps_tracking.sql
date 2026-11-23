-- TEGA PRIVÉ V5: GPS location tracking
-- Requires the PostGIS extension (already enabled by V3 or manually: CREATE EXTENSION postgis).
-- geography(Point, 4326) stores WGS-84 lon/lat; queries use ST_Distance, ST_DWithin etc.

CREATE TABLE IF NOT EXISTS gps_locations (
    id           UUID                     NOT NULL PRIMARY KEY,
    journey_id   UUID                     NOT NULL REFERENCES journeys (id) ON DELETE CASCADE,
    driver_id    UUID                     NOT NULL REFERENCES drivers (id),
    -- PostGIS geographic point: geography(Point, SRID 4326 = WGS-84)
    location     geography(Point, 4326)   NOT NULL,
    -- Convenience columns for fast retrieval without PostGIS functions
    latitude     NUMERIC(10, 7)           NOT NULL,
    longitude    NUMERIC(10, 7)           NOT NULL,
    heading      NUMERIC(5, 2),           -- degrees 0-360
    speed        NUMERIC(6, 2),           -- km/h
    accuracy     NUMERIC(8, 2),           -- metres
    -- Source device; "BROWSER" for web, "MOBILE_APP" for future React Native app
    device       VARCHAR(32)              NOT NULL DEFAULT 'BROWSER',
    recorded_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Spatial index for efficient bounding-box queries
CREATE INDEX IF NOT EXISTS idx_gps_locations_location
    ON gps_locations USING GIST (location);

-- Time-ordered index per journey (for fetching most-recent location)
CREATE INDEX IF NOT EXISTS idx_gps_journey_time
    ON gps_locations (journey_id, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_gps_driver ON gps_locations (driver_id);
