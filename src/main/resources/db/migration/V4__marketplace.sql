-- Flyway Migration V4: Circular Marketplace & Material Trading
-- Supports batch scrap listings, buyer offers, and transactional settlement

-- 1. Batch Scrap & Recyclable Material Listings
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    material_type VARCHAR(64) NOT NULL, -- 'plastic', 'paper', 'metal', 'e_waste', 'glass', 'organic'
    sub_type VARCHAR(100),
    quantity_kg NUMERIC(10, 2) NOT NULL,
    asking_price_per_kg NUMERIC(10, 2),
    total_asking_price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    pickup_city VARCHAR(100) NOT NULL DEFAULT 'Metropolis',
    pickup_address TEXT NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    photo_url TEXT,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'reserved', 'sold', 'cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_material ON marketplace_listings(material_type);
CREATE INDEX IF NOT EXISTS idx_marketplace_status ON marketplace_listings(status);
CREATE INDEX IF NOT EXISTS idx_marketplace_seller ON marketplace_listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_created ON marketplace_listings(created_at DESC);

-- 2. Buyer Bids & Commercial Scrap Offers
CREATE TABLE IF NOT EXISTS marketplace_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES marketplace_listings(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    offered_price_per_kg NUMERIC(10, 2),
    total_offered_price NUMERIC(10, 2) NOT NULL,
    message TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected', 'withdrawn')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_marketplace_offers_listing ON marketplace_offers(listing_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_offers_buyer ON marketplace_offers(buyer_id);

-- 3. Deal Settlement & Verification Ledger
CREATE TABLE IF NOT EXISTS marketplace_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES marketplace_listings(id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES marketplace_offers(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES app_users(id),
    buyer_id UUID NOT NULL REFERENCES app_users(id),
    agreed_price NUMERIC(10, 2) NOT NULL,
    verified_weight_kg NUMERIC(10, 2),
    settlement_status VARCHAR(32) NOT NULL DEFAULT 'escrow'
        CHECK (settlement_status IN ('escrow', 'settled', 'disputed', 'cancelled')),
    eco_points_awarded INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_marketplace_trans_seller ON marketplace_transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_trans_buyer ON marketplace_transactions(buyer_id);

-- 4. Seed initial marketplace batches for trading simulation
INSERT INTO marketplace_listings (id, seller_id, title, material_type, sub_type, quantity_kg, asking_price_per_kg, total_asking_price, pickup_city, pickup_address, description, status)
VALUES
(
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '00000000-0000-0000-0000-000000000001',
    'Clean PET Flakes & Crushed Bottles (350 kg)',
    'plastic',
    'PET Flakes',
    350.00,
    0.28,
    98.00,
    'Metropolis',
    'Green Park Logistics Hub, Sector 9',
    'Industrial grade washed transparent PET flakes packed in 7 super-sacks. Moisture < 1%. Ready for immediate forklift loading.',
    'active'
),
(
    'b2222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '00000000-0000-0000-0000-000000000001',
    'Baled Corrugated Cardboard Boxes (1,200 kg)',
    'paper',
    'OCC Cardboard',
    1200.00,
    0.15,
    180.00,
    'Metropolis',
    'Harbor Distribution Center, Dock 4',
    'Tight wire-bound high-density OCC cardboard bales. Dry storage kept, zero mold, ideal for pulp mills.',
    'active'
),
(
    'c3333333-cccc-cccc-cccc-cccccccccccc',
    '00000000-0000-0000-0000-000000000001',
    'Stripped Copper Wiring & Brass Valves (75 kg)',
    'metal',
    'Copper Millberry & Brass',
    75.00,
    6.20,
    465.00,
    'Metropolis',
    'Foundry District Warehouse 12',
    'Bright clean electrical copper wire and plumbing brass valves sorted in steel drums. High metallurgical purity.',
    'active'
),
(
    'd4444444-dddd-dddd-dddd-dddddddddddd',
    '00000000-0000-0000-0000-000000000001',
    'Decommissioned Server Racks & Motherboard PCBs (120 kg)',
    'e_waste',
    'Telecom & Server E-Waste',
    120.00,
    2.50,
    300.00,
    'Metropolis',
    'Silicon Valley Innovation Campus, Gate 2',
    'Enterprise server power supplies, telecom network cards, gold-pin server memory, and PCI PCBs.',
    'active'
)
ON CONFLICT DO NOTHING;
