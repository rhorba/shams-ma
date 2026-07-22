CREATE TYPE verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE installers (
  user_id            UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  business_name      VARCHAR(255) NOT NULL,
  phone              VARCHAR(30),
  verification_status verification_status NOT NULL DEFAULT 'PENDING',
  base_location      GEOGRAPHY(POINT, 4326),        -- coverage-zone center; nullable until installer sets it
  coverage_radius_km NUMERIC(6,2),                  -- nullable until installer sets it
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
