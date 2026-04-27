import { Router } from "express";
import { z } from "zod";
import { requireAuth } from "../middleware/auth.js";
import {
  incrementLiveViewers,
  getLiveViewers,
  rateLimitCheck,
  getTrendingTags,
  incrementTagScore,
  cacheGetOrSet,
} from "../lib/redis.js";
import {
  recordPostInteraction,
  getPostInteractions,
  recordActivity,
} from "../lib/astra.js";
import {
  addFollow,
  removeFollow,
  isFollowing,
  getFollowingIds,
  getUserById,
  upsertUser,
} from "../lib/neon.js";

const router = Router();

router.post("/interactions", requireAuth(), async (req, res) => {
  const schema = z.object({
    postId: z.string(),
    userId: z.string(),
    action: z.enum(["like", "repost", "bookmark", "view"]),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { postId, userId, action } = parsed.data;

  const rl = await rateLimitCheck(userId, `interaction:${action}`, 100, 60);
  if (!rl.allowed) {
    res.status(429).json({ error: "Too many interactions" });
    return;
  }

  await recordPostInteraction(postId, userId, action);
  await recordActivity(userId, action, { postId });

  res.json({ success: true });
});

router.get("/interactions/:postId/:action", async (req, res) => {
  const { postId, action } = req.params;
  const userIds = await getPostInteractions(postId, action);
  res.json({ postId, action, userIds, count: userIds.length });
});

router.post("/follow", requireAuth(), async (req, res) => {
  const schema = z.object({
    followerId: z.string(),
    followingId: z.string(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { followerId, followingId } = parsed.data;
  if (followerId === followingId) {
    res.status(400).json({ error: "Cannot follow yourself" });
    return;
  }

  const rl = await rateLimitCheck(followerId, "follow", 50, 3600);
  if (!rl.allowed) {
    res.status(429).json({ error: "Follow rate limit exceeded" });
    return;
  }

  await addFollow(followerId, followingId);
  await recordActivity(followerId, "follow", { followingId });
  res.json({ success: true });
});

router.post("/unfollow", requireAuth(), async (req, res) => {
  const schema = z.object({
    followerId: z.string(),
    followingId: z.string(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { followerId, followingId } = parsed.data;
  await removeFollow(followerId, followingId);
  res.json({ success: true });
});

router.get("/follow-status", async (req, res) => {
  const { followerId, followingId } = req.query as Record<string, string>;
  if (!followerId || !followingId) {
    res.status(400).json({ error: "followerId and followingId required" });
    return;
  }
  const following = await isFollowing(followerId, followingId);
  res.json({ following });
});

router.get("/user/:id", async (req, res) => {
  const user = await cacheGetOrSet(
    `user:${req.params.id}`,
    () => getUserById(req.params.id),
    120
  );
  if (!user) {
    res.status(404).json({ error: "User not found" });
    return;
  }
  res.json(user);
});

router.post("/user", async (req, res) => {
  const schema = z.object({
    id: z.string(),
    email: z.string().email(),
    username: z.string(),
    displayName: z.string(),
    avatar: z.string().optional(),
  });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  await upsertUser(parsed.data);
  res.json({ success: true });
});

router.get("/following/:userId", async (req, res) => {
  const ids = await getFollowingIds(req.params.userId);
  res.json({ followingIds: ids });
});

router.get("/trending-tags", async (req, res) => {
  const limit = Number(req.query["limit"] ?? 10);
  const tags = await getTrendingTags(limit);
  res.json({ tags });
});

router.post("/tag-score", async (req, res) => {
  const { tag } = req.body as { tag: string };
  if (!tag) {
    res.status(400).json({ error: "tag required" });
    return;
  }
  await incrementTagScore(tag);
  res.json({ success: true });
});

export default router;
