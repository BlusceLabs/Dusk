import { Router } from "express";
import { z } from "zod";
import { requireAuth } from "../middleware/auth.js";
import {
  onPostCreated,
  onPostLiked,
  onUserFollowed,
  onUserRegistered,
  onMessageSent,
  onMediaUploaded,
  onContentFlagged,
  onStreamStarted,
} from "../lib/events.js";
import { getQueueStats, QUEUES } from "../lib/rabbitmq.js";

const router = Router();

router.post("/post-created", requireAuth(), async (req, res) => {
  const schema = z.object({
    postId: z.string(),
    authorId: z.string(),
    content: z.string(),
    communityId: z.string().optional(),
    followerIds: z.array(z.string()).optional(),
    authorName: z.string().optional(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onPostCreated(parsed.data);
  res.json({ success: true });
});

router.post("/post-liked", requireAuth(), async (req, res) => {
  const schema = z.object({
    postId: z.string(),
    userId: z.string(),
    postAuthorId: z.string(),
    postAuthorFcmToken: z.string().optional(),
    likerName: z.string(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onPostLiked(parsed.data);
  res.json({ success: true });
});

router.post("/user-followed", requireAuth(), async (req, res) => {
  const schema = z.object({
    followerId: z.string(),
    followingId: z.string(),
    followerName: z.string(),
    followingFcmToken: z.string().optional(),
    followingEmail: z.string().optional(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onUserFollowed(parsed.data);
  res.json({ success: true });
});

router.post("/user-registered", requireAuth(), async (req, res) => {
  const schema = z.object({
    userId: z.string(),
    email: z.string().email(),
    displayName: z.string(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onUserRegistered(parsed.data);
  res.json({ success: true });
});

router.post("/message-sent", requireAuth(), async (req, res) => {
  const schema = z.object({
    conversationId: z.string(),
    messageId: z.string(),
    senderId: z.string(),
    senderName: z.string(),
    recipientIds: z.array(z.string()),
    content: z.string(),
    type: z.enum(["text", "image", "voice"]).optional(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onMessageSent(parsed.data);
  res.json({ success: true });
});

router.post("/media-uploaded", requireAuth(), async (req, res) => {
  const schema = z.object({
    userId: z.string(),
    key: z.string(),
    type: z.enum(["image", "video"]),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onMediaUploaded(parsed.data);
  res.json({ success: true });
});

router.post("/content-flagged", requireAuth(), async (req, res) => {
  const schema = z.object({
    targetId: z.string(),
    targetType: z.string(),
    reporterId: z.string(),
    reason: z.string(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onContentFlagged(parsed.data);
  res.json({ success: true });
});

router.post("/stream-started", requireAuth(), async (req, res) => {
  const schema = z.object({
    streamId: z.string(),
    streamerId: z.string(),
    title: z.string(),
    followerIds: z.array(z.string()).optional(),
    streamerName: z.string().optional(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) { res.status(400).json({ error: parsed.error.flatten() }); return; }
  await onStreamStarted(parsed.data);
  res.json({ success: true });
});

router.get("/queue-stats", async (req, res) => {
  const adminToken = process.env.API_ADMIN_TOKEN;
  const provided = req.headers["x-admin-token"];
  if (!adminToken || provided !== adminToken) {
    res.status(403).json({ error: "Forbidden" });
    return;
  }

  const { TOPICS, getStreamLength } = await import("../lib/kafka.js");

  const [queueStats, streamStats] = await Promise.all([
    Promise.all(
      Object.entries(QUEUES).map(async ([name, queue]) => {
        const s = await getQueueStats(queue as any);
        return { name, queue, ...s };
      })
    ),
    Promise.all(
      Object.entries(TOPICS).map(async ([name, stream]) => ({
        name,
        stream,
        length: await getStreamLength(stream as any),
      }))
    ),
  ]);

  res.json({ queues: queueStats, streams: streamStats });
});

export default router;
