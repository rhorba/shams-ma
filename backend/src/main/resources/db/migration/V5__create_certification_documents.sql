CREATE TABLE certification_documents (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  installer_id   UUID NOT NULL REFERENCES installers(user_id) ON DELETE CASCADE,
  file_url       VARCHAR(1000) NOT NULL,   -- object storage key, not public URL
  status         verification_status NOT NULL DEFAULT 'PENDING',
  reviewed_by    UUID REFERENCES users(id),
  reviewed_at    TIMESTAMPTZ,
  uploaded_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
