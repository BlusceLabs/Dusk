import { Router } from "express";
import { z } from "zod";
import { sql } from "../lib/neon.js";
import { rateLimitCheck } from "../lib/redis.js";
import { logger } from "../lib/logger.js";
import { requireAuth } from "../middleware/auth.js";

const router = Router();

const RegisterSchema = z.object({
  userId: z.string().min(1),
  token: z.string().min(1).regex(/^ExponentPushToken\[/, "Invalid Expo push token format"),
});

router.post("/register", requireAuth(), async (req, res) => {
  const parsed = RegisterSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid request", details: parsed.error.flatten() });
    return;
  }
  const { userId, token } = parsed.data;

  const rl = await rateLimitCheck(userId, "push:register", 5, 3600);
  if (!rl.allowed) {
    res.status(429).json({ error: "Too many token registrations" });
    return;
  }

  try {
    await sql`
      INSERT INTO push_tokens (user_id, token, platform, updated_at)
      VALUES (${userId}, ${token}, 'expo', NOW())
      ON CONFLICT (user_id) DO UPDATE
        SET token = EXCLUDED.token,
            updated_at = NOW()
    `;
    logger.info({ userId }, "[push] token registered");
    res.json({ success: true });
    return;
  } catch (err) {
    logger.error({ err, userId }, "[push] failed to register token");
    res.status(500).json({ error: "Failed to save token" });
    return;
  }
});

export async function getPushToken(userId: string): Promise<string | null> {
  try {
    const rows = await sql<{ token: string }[]>`
      SELECT token FROM push_tokens WHERE user_id = ${userId} LIMIT 1
    `;
    return rows[0]?.token ?? null;
  } catch {
    return null;
  }
}

export default router;
