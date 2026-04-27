import postgres from "postgres";

const connectionString = process.env["NEON_DATABASE_URL"]!;

export const sql = postgres(connectionString, {
  ssl: "require",
  max: 10,
  idle_timeout: 30,
  connect_timeout: 10,
});

export async function initNeonSchema(): Promise<void> {
  await sql`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT UNIQUE NOT NULL,
      username TEXT UNIQUE NOT NULL,
      display_name TEXT NOT NULL,
      avatar TEXT,
      bio TEXT DEFAULT '',
      is_verified BOOLEAN DEFAULT false,
      is_creator BOOLEAN DEFAULT false,
      is_premium BOOLEAN DEFAULT false,
      followers_count INTEGER DEFAULT 0,
      following_count INTEGER DEFAULT 0,
      created_at TIMESTAMPTZ DEFAULT now(),
      updated_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS subscriptions (
      id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
      subscriber_id TEXT NOT NULL REFERENCES users(id),
      creator_id TEXT NOT NULL REFERENCES users(id),
      tier TEXT NOT NULL DEFAULT 'basic',
      price_cents INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'active',
      started_at TIMESTAMPTZ DEFAULT now(),
      expires_at TIMESTAMPTZ,
      UNIQUE(subscriber_id, creator_id)
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS follows (
      follower_id TEXT NOT NULL REFERENCES users(id),
      following_id TEXT NOT NULL REFERENCES users(id),
      created_at TIMESTAMPTZ DEFAULT now(),
      PRIMARY KEY (follower_id, following_id)
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS creator_tiers (
      id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
      creator_id TEXT NOT NULL REFERENCES users(id),
      name TEXT NOT NULL,
      description TEXT,
      price_cents INTEGER NOT NULL,
      perks TEXT[],
      is_active BOOLEAN DEFAULT true,
      created_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS reports (
      id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
      reporter_id TEXT NOT NULL REFERENCES users(id),
      target_type TEXT NOT NULL,
      target_id TEXT NOT NULL,
      reason TEXT NOT NULL,
      details TEXT,
      status TEXT DEFAULT 'pending',
      created_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS payment_orders (
      order_id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      plan_id TEXT NOT NULL,
      billing TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'pending',
      created_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS premium_subscriptions (
      id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
      user_id TEXT NOT NULL,
      plan_id TEXT NOT NULL,
      billing TEXT NOT NULL,
      order_id TEXT UNIQUE NOT NULL,
      payer_email TEXT,
      amount_cents INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'active',
      expires_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`
    CREATE TABLE IF NOT EXISTS push_tokens (
      user_id TEXT PRIMARY KEY,
      token TEXT NOT NULL,
      platform TEXT NOT NULL DEFAULT 'expo',
      updated_at TIMESTAMPTZ DEFAULT now()
    )
  `;

  await sql`CREATE INDEX IF NOT EXISTS idx_follows_follower ON follows(follower_id)`;
  await sql`CREATE INDEX IF NOT EXISTS idx_follows_following ON follows(following_id)`;
  await sql`CREATE INDEX IF NOT EXISTS idx_subscriptions_creator ON subscriptions(creator_id)`;
  await sql`CREATE INDEX IF NOT EXISTS idx_subscriptions_subscriber ON subscriptions(subscriber_id)`;
  await sql`CREATE INDEX IF NOT EXISTS idx_premium_subs_user ON premium_subscriptions(user_id)`;
}

export async function upsertUser(user: {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatar?: string;
}): Promise<void> {
  await sql`
    INSERT INTO users (id, email, username, display_name, avatar)
    VALUES (${user.id}, ${user.email}, ${user.username}, ${user.displayName}, ${user.avatar ?? null})
    ON CONFLICT (id) DO UPDATE SET
      username = EXCLUDED.username,
      display_name = EXCLUDED.display_name,
      avatar = EXCLUDED.avatar,
      updated_at = now()
  `;
}

export async function getUserById(id: string) {
  const rows = await sql`SELECT * FROM users WHERE id = ${id}`;
  return rows[0] ?? null;
}

export async function isFollowing(followerId: string, followingId: string): Promise<boolean> {
  const rows = await sql`
    SELECT 1 FROM follows WHERE follower_id = ${followerId} AND following_id = ${followingId}
  `;
  return rows.length > 0;
}

export async function addFollow(followerId: string, followingId: string): Promise<void> {
  await sql`
    INSERT INTO follows (follower_id, following_id)
    VALUES (${followerId}, ${followingId})
    ON CONFLICT DO NOTHING
  `;
  await sql`UPDATE users SET following_count = following_count + 1 WHERE id = ${followerId}`;
  await sql`UPDATE users SET followers_count = followers_count + 1 WHERE id = ${followingId}`;
}

export async function getFollowingIds(userId: string): Promise<string[]> {
  const rows = await sql`SELECT following_id FROM follows WHERE follower_id = ${userId}`;
  return rows.map((r: any) => r.following_id);
}

export async function removeFollow(followerId: string, followingId: string): Promise<void> {
  const result = await sql`
    DELETE FROM follows WHERE follower_id = ${followerId} AND following_id = ${followingId}
  `;
  if ((result as any).count > 0) {
    await sql`UPDATE users SET following_count = following_count - 1 WHERE id = ${followerId}`;
    await sql`UPDATE users SET followers_count = followers_count - 1 WHERE id = ${followingId}`;
  }
}

export async function getCreatorTiers(creatorId: string) {
  return sql`SELECT * FROM creator_tiers WHERE creator_id = ${creatorId} AND is_active = true ORDER BY price_cents ASC`;
}

export async function createSubscription(data: {
  subscriberId: string;
  creatorId: string;
  tier: string;
  priceCents: number;
  expiresAt?: Date;
}): Promise<void> {
  await sql`
    INSERT INTO subscriptions (id, subscriber_id, creator_id, tier, price_cents, expires_at)
    VALUES (gen_random_uuid()::text, ${data.subscriberId}, ${data.creatorId}, ${data.tier}, ${data.priceCents}, ${data.expiresAt ?? null})
    ON CONFLICT (subscriber_id, creator_id) DO UPDATE SET
      tier = EXCLUDED.tier,
      price_cents = EXCLUDED.price_cents,
      status = 'active',
      expires_at = EXCLUDED.expires_at
  `;
}
