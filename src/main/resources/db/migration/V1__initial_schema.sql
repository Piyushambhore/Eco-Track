-- EcoTrack Initial Schema
-- V1: Users and Activity Records

-- Users table
CREATE TABLE app_users (
    id              UUID PRIMARY KEY,
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'active'
                    CHECK (status IN ('active', 'disabled', 'pending')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX app_users_email_lower_uq
    ON app_users (lower(email));

-- Activity Records (Environmental tracking entries)
CREATE TABLE activity_records (
    id              UUID PRIMARY KEY,
    owner_user_id   UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    record_type     TEXT NOT NULL
                    CHECK (record_type IN (
                        'carbon_footprint',
                        'energy_usage',
                        'water_usage',
                        'waste_recycled',
                        'transport',
                        'food_consumption',
                        'tree_planting',
                        'other'
                    )),
    title           TEXT NOT NULL,
    quantity        NUMERIC(12, 3) CHECK (quantity >= 0),
    unit            TEXT CHECK (unit IN (
                        'kg', 'g', 'liters', 'ml', 'kwh', 'miles', 'km',
                        'hours', 'count', 'tons', 'gallons'
                    )),
    carbon_impact   NUMERIC(10, 2),  -- estimated CO2 impact in kg
    occurred_at     TIMESTAMPTZ NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX activity_records_owner_occurred_idx
    ON activity_records (owner_user_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX activity_records_type_idx
    ON activity_records (record_type)
    WHERE deleted_at IS NULL;

-- Updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_app_users_updated_at
    BEFORE UPDATE ON app_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_activity_records_updated_at
    BEFORE UPDATE ON activity_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
