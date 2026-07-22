-- idx_users_email and idx_payments_cmi_txn are implicit via UNIQUE constraints (V2, V7) — not created here.

CREATE INDEX idx_homeowners_location ON homeowners USING GIST (location);

CREATE INDEX idx_installers_base_location ON installers USING GIST (base_location);
CREATE INDEX idx_installers_verification_status ON installers (verification_status);

CREATE INDEX idx_certdocs_installer_status ON certification_documents (installer_id, status);

CREATE INDEX idx_qr_installer_status ON quote_requests (installer_id, status);
CREATE INDEX idx_qr_homeowner ON quote_requests (homeowner_id);

CREATE INDEX idx_bookings_status ON bookings (status);

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
