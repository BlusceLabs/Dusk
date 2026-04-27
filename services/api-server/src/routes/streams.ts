import { Router } from "express";
import { z } from "zod";
import {
  incrementLiveViewers,
  getLiveViewers,
  cacheSet,
  cacheGet,
  cacheDel,
} from "../lib/redis.js";

const router = Router();

router.post("/join", async (req, res) => {
  const schema = z.object({ streamId: z.string(), userId: z.string() });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const { streamId, userId } = parsed.data;
  const viewers = await incrementLiveViewers(streamId, 1);
  await cacheSet(`stream:viewer:${streamId}:${userId}`, true, 120);
  res.json({ streamId, viewers });
});

router.post("/leave", async (req, res) => {
  const schema = z.object({ streamId: z.string(), userId: z.string() });
  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }
  const { streamId, userId } = parsed.data;
  const wasViewing = await cacheGet(`stream:viewer:${streamId}:${userId}`);
  if (wasViewing) {
    await cacheDel(`stream:viewer:${streamId}:${userId}`);
    const viewers = await incrementLiveViewers(streamId, -1);
    res.json({ streamId, viewers });
  } else {
    const viewers = await getLiveViewers(streamId);
    res.json({ streamId, viewers });
  }
});

router.get("/:streamId/viewers", async (req, res) => {
  const viewers = await getLiveViewers(req.params.streamId);
  res.json({ streamId: req.params.streamId, viewers });
});

export default router;
