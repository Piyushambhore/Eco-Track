-- Flyway Migration V3: Location-Aware Recycler Search & Collection Requests
-- Supports Haversine distance spatial lookup, verified recycler directory, and pickup scheduling lifecycle

-- 1. Verified Recycling Facilities & Scrap Aggregators Directory
CREATE TABLE IF NOT EXISTS recycling_facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(64) NOT NULL, -- 'scrap_dealer', 'recycling_center', 'e_waste_hub', 'compost_facility'
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    contact_phone VARCHAR(50),
    operating_hours VARCHAR(100) DEFAULT 'Mon-Sat 09:00 - 18:00',
    accepted_materials TEXT NOT NULL, -- comma-separated: 'plastic,paper,metal,e_waste,glass,organic'
    pricing_info TEXT, -- e.g. 'Cardboard: $0.15/kg | PET Plastic: $0.25/kg | Aluminum: $1.10/kg'
    rating NUMERIC(3, 2) DEFAULT 4.80,
    verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_facilities_city ON recycling_facilities(city);
CREATE INDEX IF NOT EXISTS idx_facilities_coords ON recycling_facilities(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_facilities_type ON recycling_facilities(facility_type);

-- 2. On-Demand Waste Collection Requests & Logistics Lifecycle
CREATE TABLE IF NOT EXISTS collection_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    collector_id UUID REFERENCES app_users(id) ON DELETE SET NULL,
    facility_id UUID REFERENCES recycling_facilities(id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted'
        CHECK (status IN ('submitted', 'assigned', 'in_transit', 'completed', 'cancelled')),
    pickup_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Metropolis',
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    preferred_date DATE NOT NULL,
    preferred_slot VARCHAR(50) NOT NULL DEFAULT '09:00 - 12:00',
    materials TEXT NOT NULL, -- comma-separated: 'plastic,paper,metal,e_waste'
    estimated_weight_kg NUMERIC(10, 2) DEFAULT 5.00,
    actual_weight_kg NUMERIC(10, 2),
    special_instructions TEXT,
    proof_photo_url TEXT,
    eco_points_earned INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_collection_requests_user ON collection_requests(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_collection_requests_status ON collection_requests(status);
CREATE INDEX IF NOT EXISTS idx_collection_requests_collector ON collection_requests(collector_id);

-- 3. Seed verified recycling centers & scrap collectors for instant geolocation discovery
INSERT INTO recycling_facilities (name, facility_type, address, city, postal_code, latitude, longitude, contact_phone, operating_hours, accepted_materials, pricing_info, rating, verified)
VALUES
(
    'GreenCycle Central Depot',
    'recycling_center',
    '104 Eco Industrial Blvd, Sector 4',
    'Metropolis',
    '10001',
    40.7128,
    -74.0060,
    '+1 (555) 234-5678',
    'Mon-Sat 08:00 - 18:00',
    'plastic,paper,metal,glass',
    'Plastic: $0.22/kg | Paper: $0.12/kg | Aluminum: $1.20/kg',
    4.92,
    TRUE
),
(
    'Urban Scrap & Metal Aggregators',
    'scrap_dealer',
    '45 Foundry Lane, Harbor District',
    'Metropolis',
    '10004',
    40.7042,
    -74.0112,
    '+1 (555) 345-6789',
    'Mon-Sat 07:30 - 19:00',
    'metal,e_waste,plastic',
    'Copper: $5.80/kg | Brass: $3.40/kg | Batteries: $0.75/unit',
    4.85,
    TRUE
),
(
    'TerraNova E-Waste Safe Hub',
    'e_waste_hub',
    '88 Silicon Avenue, Tech Park',
    'Metropolis',
    '10010',
    40.7410,
    -73.9897,
    '+1 (555) 456-7890',
    'Tue-Sun 10:00 - 17:00',
    'e_waste,hazardous',
    'Certified PCB & battery drop-off | Free certificate of safe destruction',
    4.96,
    TRUE
),
(
    'BioLoop Community Compost Center',
    'compost_facility',
    '12 Garden Grove Way, Green Valley',
    'Metropolis',
    '10025',
    40.7989,
    -73.9665,
    '+1 (555) 567-8901',
    'Mon-Sun 08:00 - 16:00',
    'organic',
    'Free organic drop-off | Take home free compost soil',
    4.88,
    TRUE
),
(
    'ClearStream Glass & Paper Processors',
    'recycling_center',
    '230 Riverbank Rd, North Riverside',
    'Metropolis',
    '10032',
    40.8385,
    -73.9421,
    '+1 (555) 678-9012',
    'Mon-Fri 08:30 - 17:30',
    'glass,paper,plastic',
    'Cardboard: $0.14/kg | Glass bottles: $0.08/kg',
    4.79,
    TRUE
)
ON CONFLICT DO NOTHING;
