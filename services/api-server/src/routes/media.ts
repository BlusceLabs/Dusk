import { Router } from "express";
import { z } from "zod";
import { getPresignedUploadUrl, buildMediaKey, getPublicUrl, deleteObject } from "../lib/r2.js";
import { rateLimitCheck } from "../lib/redis.js";

const router = Router();

const uploadRequestSchema = z.object({
  userId: z.string().min(1),
  type: z.enum(["avatar", "post", "story", "banner"]),
  contentType: z.enum(["image/jpeg", "image/png", "image/webp", "video/mp4", "video/webm"]),
  filename: z.string().min(1).max(200),
});

router.post("/upload-url", async (req, res) => {
  const parsed = uploadRequestSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid request", details: parsed.error.flatten() });
    return;
  }

  const { userId, type, contentType, filename } = parsed.data;

  const rateLimit = await rateLimitCheck(userId, "upload", 20, 3600);
  if (!rateLimit.allowed) {
    res.status(429).json({ error: "Upload rate limit exceeded. Try again later." });
    return;
  }

  const ext = filename.split(".").pop() ?? "bin";
  const key = buildMediaKey(type, userId, `${Date.now()}.${ext}`);

  const uploadUrl = await getPresignedUploadUrl(key, contentType);
  const publicUrl = getPublicUrl(key);

  res.json({ uploadUrl, key, publicUrl });
});

router.delete("/object", async (req, res) => {
  const { key, userId } = req.body as { key: string; userId: string };
  if (!key || !userId) {
    res.status(400).json({ error: "key and userId are required" });
    return;
  }

  if (!key.includes(`/${userId}/`)) {
    res.status(403).json({ error: "Unauthorized" });
    return;
  }

  await deleteObject(key);
  res.json({ success: true });
});

export default router;
