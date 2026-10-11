-- Flyway Migration V6: Ecological Impact Calculator & ESG Analytics
-- Scientific carbon avoidance models, environmental equivalents, and verifiable ESG certificates

-- 1. Fine-grained Impact Records
CREATE TABLE IF NOT EXISTS impact_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    source_type VARCHAR(64) NOT NULL, -- 'COLLECTION', 'MARKETPLACE', 'MANUAL_REPORT'
    source_id VARCHAR(128),
    material_type VARCHAR(64) NOT NULL,
    weight_kg NUMERIC(10, 2) NOT NULL,
    co2_saved_kg NUMERIC(10, 3) NOT NULL,
    water_saved_liters NUMERIC(10, 2) NOT NULL DEFAULT 0,
    energy_saved_kwh NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_impact_user ON impact_records(user_id);
CREATE INDEX IF NOT EXISTS idx_impact_material ON impact_records(material_type);
CREATE INDEX IF NOT EXISTS idx_impact_created ON impact_records(created_at DESC);

-- 2. Verifiable ESG Impact Certificates
CREATE TABLE IF NOT EXISTS impact_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_number VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    title VARCHAR(128) NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    total_weight_kg NUMERIC(10, 2) NOT NULL,
    total_co2_kg NUMERIC(10, 3) NOT NULL,
    trees_equivalent NUMERIC(10, 2) NOT NULL DEFAULT 0,
    water_saved_liters NUMERIC(10, 2) NOT NULL DEFAULT 0,
    energy_saved_kwh NUMERIC(10, 2) NOT NULL DEFAULT 0,
    verification_hash VARCHAR(128) NOT NULL, -- SHA-256 tamper-proof verification signature
    status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED'
        CHECK (status IN ('VERIFIED', 'REVOKED')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_certificates_user ON impact_certificates(user_id);
CREATE INDEX IF NOT EXISTS idx_certificates_number ON impact_certificates(certificate_number);
