-- Flyway Migration V5: Gamified Rewards Ledger & Community Leaderboards
-- Establishes append-only points ledger, badge tracking, and reward redemptions

-- 1. Append-Only Rewards Ledger Table
CREATE TABLE IF NOT EXISTS rewards_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    points INT NOT NULL, -- Positive for rewards, negative for redemptions
    balance_after INT NOT NULL,
    reason VARCHAR(64) NOT NULL, -- e.g. 'COLLECTION_VERIFIED', 'MARKETPLACE_SALE', 'DAILY_STREAK', 'REDEMPTION'
    reference_id VARCHAR(128),   -- e.g. collection request ID or transaction ID
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rewards_ledger_user ON rewards_ledger(user_id);
CREATE INDEX IF NOT EXISTS idx_rewards_ledger_created ON rewards_ledger(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_rewards_ledger_reason ON rewards_ledger(reason);

-- 2. Reward Redemptions (Vouchers, Tree Planting, Municipal Credits)
CREATE TABLE IF NOT EXISTS rewards_redemptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    reward_type VARCHAR(64) NOT NULL,
    points_spent INT NOT NULL,
    voucher_code VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'redeemed', 'expired')),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_redemptions_user ON rewards_redemptions(user_id);
CREATE INDEX IF NOT EXISTS idx_redemptions_code ON rewards_redemptions(voucher_code);

-- 3. User Achievement Badges
CREATE TABLE IF NOT EXISTS user_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    badge_key VARCHAR(64) NOT NULL, -- e.g. 'FIRST_RECYCLING', 'COMMUNITY_CONTRIBUTOR', 'CONSISTENT_RECYCLER', 'ZERO_WASTE_HERO'
    title VARCHAR(128) NOT NULL,
    description TEXT,
    icon_name VARCHAR(64) DEFAULT 'badge_eco',
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_badge UNIQUE (user_id, badge_key)
);

CREATE INDEX IF NOT EXISTS idx_user_badges_user ON user_badges(user_id);
