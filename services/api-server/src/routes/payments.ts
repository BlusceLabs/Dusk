import { Router } from "express";
import { requireAuth } from "../middleware/auth.js";
import { z } from "zod";
import { createOrder, captureOrder } from "../lib/paypal.js";
import { sql } from "../lib/neon.js";
import { logger } from "../lib/logger.js";

const router = Router();

const PLAN_PRICES: Record<string, { monthly: number; yearly: number; name: string }> = {
  basic: { monthly: 799, yearly: 6399, name: "Premium" },
  pro: { monthly: 1499, yearly: 11999, name: "Premium+" },
};

const CreateOrderSchema = z.object({
  planId: z.enum(["basic", "pro"]),
  billing: z.enum(["monthly", "yearly"]),
  userId: z.string().min(1),
  returnUrl: z.string().url(),
  cancelUrl: z.string().url(),
});

const CaptureOrderSchema = z.object({
  orderId: z.string().min(1),
  userId: z.string().min(1),
  planId: z.enum(["basic", "pro"]),
  billing: z.enum(["monthly", "yearly"]),
});

router.post("/create-order", requireAuth(), async (req, res) => {
  const parsed = CreateOrderSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: "Invalid request", details: parsed.error.issues });
  }

  const { planId, billing, userId, returnUrl, cancelUrl } = parsed.data;
  const plan = PLAN_PRICES[planId];

  try {
    const { orderId, approveUrl } = await createOrder({
      planId,
      planName: plan.name,
      priceCents: billing === "monthly" ? plan.monthly : plan.yearly,
      billing,
      returnUrl,
      cancelUrl,
    });

    await sql`
      INSERT INTO payment_orders (order_id, user_id, plan_id, billing, status, created_at)
      VALUES (${orderId}, ${userId}, ${planId}, ${billing}, 'pending', now())
      ON CONFLICT (order_id) DO NOTHING
    `.catch(() => {});

    return res.json({ orderId, approveUrl });
  } catch (err) {
    logger.error({ err }, "create-order failed");
    return res.status(500).json({ error: "Failed to create PayPal order" });
  }
});

router.post("/capture-order", requireAuth(), async (req, res) => {
  const parsed = CaptureOrderSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: "Invalid request", details: parsed.error.issues });
  }

  const { orderId, userId, planId, billing } = parsed.data;

  try {
    const capture = await captureOrder(orderId);

    if (capture.status !== "COMPLETED") {
      return res.status(400).json({ error: "Payment not completed", status: capture.status });
    }

    const expiresAt = new Date();
    if (billing === "monthly") {
      expiresAt.setMonth(expiresAt.getMonth() + 1);
    } else {
      expiresAt.setFullYear(expiresAt.getFullYear() + 1);
    }

    await sql`
      UPDATE users SET is_premium = true, updated_at = now() WHERE id = ${userId}
    `.catch(() => {});

    await sql`
      INSERT INTO premium_subscriptions (user_id, plan_id, billing, order_id, payer_email, amount_cents, status, expires_at, created_at)
      VALUES (
        ${userId}, ${planId}, ${billing}, ${orderId},
        ${capture.email}, ${Math.round(parseFloat(capture.amount) * 100)},
        'active', ${expiresAt.toISOString()}, now()
      )
      ON CONFLICT (order_id) DO NOTHING
    `.catch(() => {});

    await sql`
      UPDATE payment_orders SET status = 'completed' WHERE order_id = ${orderId}
    `.catch(() => {});

    logger.info({ userId, planId, billing, amount: capture.amount }, "PayPal payment captured");

    return res.json({
      success: true,
      planId,
      billing,
      expiresAt: expiresAt.toISOString(),
      payerEmail: capture.email,
    });
  } catch (err) {
    logger.error({ err }, "capture-order failed");
    return res.status(500).json({ error: "Failed to capture PayPal payment" });
  }
});

router.get("/status/:userId", async (req, res) => {
  const { userId } = req.params;
  try {
    const rows = await sql`
      SELECT plan_id, billing, expires_at, status
      FROM premium_subscriptions
      WHERE user_id = ${userId} AND status = 'active'
      ORDER BY created_at DESC
      LIMIT 1
    `.catch(() => []);

    if (!rows || rows.length === 0) {
      return res.json({ isPremium: false });
    }

    const sub = rows[0];
    const now = new Date();
    const expired = sub.expires_at && new Date(sub.expires_at) < now;

    if (expired) {
      await sql`
        UPDATE premium_subscriptions SET status = 'expired' WHERE user_id = ${userId} AND status = 'active'
      `.catch(() => {});
      await sql`UPDATE users SET is_premium = false WHERE id = ${userId}`.catch(() => {});
      return res.json({ isPremium: false });
    }

    return res.json({
      isPremium: true,
      planId: sub.plan_id,
      billing: sub.billing,
      expiresAt: sub.expires_at,
    });
  } catch (err) {
    logger.error({ err }, "payment status check failed");
    return res.status(500).json({ error: "Failed to check payment status" });
  }
});

export default router;
