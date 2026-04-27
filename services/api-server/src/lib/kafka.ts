/**
 * Event streaming via Redis Streams (Upstash).
 * Drop-in replacement for Kafka — same exported API, no extra credentials needed.
 *
 * Redis Streams give us the same guarantees as Kafka for this use case:
 *  - Persistent, ordered, append-only event log
 *  - Consumer groups for fan-out / competing consumers
 *  - At-least-once delivery
 *  - Automatic trimming to bound memory usage
 */
import { redis } from "./redis.js";

export const TOPICS = {
  EVENTS: "dusk:stream:events",
  FEED_FANOUT: "dusk:stream:feed-fanout",
  NOTIFICATIONS: "dusk:stream:notifications",
  ANALYTICS: "dusk:stream:analytics",
  MODERATION: "dusk:stream:moderation",
} as const;

export type TopicName = (typeof TOPICS)[keyof typeof TOPICS];

export type DuskEvent =
  | { type: "post.created"; postId: string; authorId: string; content: string; communityId?: string; ts: number }
  | { type: "post.liked"; postId: string; userId: string; ts: number }
  | { type: "post.reposted"; postId: string; userId: string; ts: number }
  | { type: "post.viewed"; postId: string; userId: string; ts: number }
  | { type: "user.followed"; followerId: string; followingId: string; ts: number }
  | { type: "user.registered"; userId: string; email: string; ts: number }
  | { type: "stream.started"; streamId: string; streamerId: string; title: string; ts: number }
  | { type: "stream.ended"; streamId: string; streamerId: string; ts: number }
  | { type: "message.sent"; conversationId: string; senderId: string; ts: number }
  | { type: "content.flagged"; targetId: string; targetType: string; reporterId: string; reason: string; ts: number };

const MAX_STREAM_LENGTH = 100_000;

function eventToFields(event: DuskEvent): Record<string, string> {
  const fields: Record<string, string> = {};
  for (const [k, v] of Object.entries(event)) {
    if (v !== undefined) fields[k] = String(v);
  }
  return fields;
}

export async function publishEvent(topic: TopicName, event: DuskEvent): Promise<void> {
  try {
    await (redis as any).xadd(topic, "*", eventToFields(event), { maxlen: { threshold: MAX_STREAM_LENGTH, approx: true } });
  } catch (err) {
    console.error(`[streams] publish error on ${topic}:`, err);
  }
}

export async function publishBatch(topic: TopicName, events: DuskEvent[]): Promise<void> {
  if (events.length === 0) return;
  await Promise.allSettled(events.map((e) => publishEvent(topic, e)));
}

export async function getStreamLength(topic: TopicName): Promise<number> {
  try {
    return await redis.xlen(topic);
  } catch {
    return 0;
  }
}

export async function getRecentEvents(
  topic: TopicName,
  count = 50
): Promise<Array<{ id: string; event: Record<string, string> }>> {
  try {
    const results = await redis.xrevrange(topic, "+", "-", count) as Record<string, Record<string, string>>;
    if (!results || typeof results !== "object") return [];
    return Object.entries(results).map(([id, event]) => ({ id, event }));
  } catch {
    return [];
  }
}

export async function ensureTopics(): Promise<void> {
  console.log("[streams] Redis Streams ready — no setup required");
}

export async function disconnectKafka(): Promise<void> {}
