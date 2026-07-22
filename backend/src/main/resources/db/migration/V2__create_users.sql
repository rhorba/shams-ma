CREATE TYPE user_role AS ENUM ('HOMEOWNER', 'INSTALLER', 'ADMIN');

CREATE TABLE users (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email          VARCHAR(255) NOT NULL UNIQUE,
  password_hash  VARCHAR(255) NOT NULL,
  role           user_role NOT NULL,
  mfa_secret     VARCHAR(255),          -- non-null only for ADMIN (Security doc: MFA required for Admin)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
