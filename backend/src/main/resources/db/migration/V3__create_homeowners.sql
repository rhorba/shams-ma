CREATE TABLE homeowners (
  user_id        UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  full_name      VARCHAR(255) NOT NULL,
  phone          VARCHAR(30),
  address_text   VARCHAR(500) NOT NULL,
  location       GEOGRAPHY(POINT, 4326) NOT NULL,  -- geocoded from address_text
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
