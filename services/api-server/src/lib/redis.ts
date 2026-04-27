import { Redis } from "@upstash/redis";

export const redis = new Redis({
  url: process.env["UPSTASH_REDIS_REST_URL"]!,
  token: process.env["UPSTASH_REDIS_REST_TOKEN"]!,
});

const DEFAULT_TTL = 60;

export async function cacheGet<T>(key: string): Promise<T | null> {
  try {
    return await redis.get<T>(key);
  } catch {
    return null;
  }
}

export async function cacheSet(key: string, value: unknown, ttl = DEFAULT_TTL): Promise<void> {
  try {
    await redis.set(key, value, { ex: ttl });
  } catch {}
}

export async function cacheDel(key: string): Promise<void> {
  try {
    await redis.del(key);
  } catch {}
}

export async function cacheGetOrSet<T>(
  key: string,
  fetcher: () => Promise<T>,
  ttl = DEFAULT_TTL
): Promise<T> {
  const cached = await cacheGet<T>(key);
  if (cached !== null) return cached;
  const value = await fetcher();
  await cacheSet(key, value, ttl);
  return value;
}

export async function incrementLiveViewers(streamId: string, delta: 1 | -1): Promise<number> {
  const key = `stream:viewers:${streamId}`;
  if (delta === 1) {
    return redis.incr(key);
  } else {
    const val = await redis.decr(key);
    if (val < 0) await redis.set(key, 0);
    return Math.max(0, val);
  }
}

export async function getLiveViewers(streamId: string): Promise<number> {
  const val = await redis.get<number>(`stream:viewers:${streamId}`);
  return val ?? 0;
}

export async function rateLimitCheck(
  userId: string,
  action: string,
  limit: number,
  windowSeconds: number
): Promise<{ allowed: boolean; remaining: number }> {
  const key = `ratelimit:${action}:${userId}`;
  const count = await redis.incr(key);
  if (count === 1) {
    await redis.expire(key, windowSeconds);
  }
  return {
    allowed: count <= limit,
    remaining: Math.max(0, limit - count),
  };
}

export async function publishNotification(userId: string, notification: object): Promise<void> {
  await redis.lpush(`notifications:queue:${userId}`, JSON.stringify(notification));
  await redis.expire(`notifications:queue:${userId}`, 86400);
}

export async function getTrendingTags(limit = 10): Promise<string[]> {
  return redis.zrange("trending:tags", 0, limit - 1, { rev: true });
}

export async function incrementTagScore(tag: string, delta = 1): Promise<void> {
  await redis.zincrby("trending:tags", delta, tag);
}
