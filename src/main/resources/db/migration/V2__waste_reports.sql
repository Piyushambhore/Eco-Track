-- EcoTrack Migration V2: Waste Reports Schema
-- Supports AI Waste Identification and persistent user reports

CREATE TABLE waste_reports (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    category        TEXT NOT NULL CHECK (category IN (
                        'plastic',
                        'paper',
                        'metal',
                        'e_waste',
                        'glass',
                        'organic',
                        'hazardous'
                    )),
    sub_type        TEXT,
    photo_url       TEXT,
    latitude        NUMERIC(9, 6),
    longitude       NUMERIC(9, 6),
    quantity        NUMERIC(12, 3) CHECK (quantity >= 0),
    unit            TEXT CHECK (unit IN (
                        'kg', 'g', 'liters', 'ml', 'count', 'items', 'tons', 'lbs'
                    )),
    confidence      NUMERIC(5, 2),
    status          TEXT NOT NULL DEFAULT 'identified' CHECK (status IN (
                        'identified',
                        'scheduled',
                        'collected',
                        'rejected'
                    )),
    disposal_guide  TEXT,
    carbon_impact   NUMERIC(10, 2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX waste_reports_user_created_idx
    ON waste_reports (user_id, created_at DESC);

CREATE INDEX waste_reports_category_idx
    ON waste_reports (category);

CREATE TRIGGER update_waste_reports_updated_at
    BEFORE UPDATE ON waste_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
