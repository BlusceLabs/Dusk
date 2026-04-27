import { Router } from "express";
import { z } from "zod";
import { sendEmail, type EmailTemplate } from "../lib/resend.js";

const router = Router();

const templateSchema = z.enum(["welcome", "password-reset", "subscription-confirmed", "new-follower"]);

router.post("/send", async (req, res) => {
  const schema = z.object({
    to: z.string().email(),
    template: templateSchema,
    variables: z.record(z.string()).optional().default({}),
  });

  const parsed = schema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.flatten() });
    return;
  }

  const { to, template, variables } = parsed.data;
  const result = await sendEmail(to, template as EmailTemplate, variables);

  if (result.error) {
    res.status(500).json({ error: result.error });
    return;
  }

  res.json({ success: true, id: result.id });
});

export default router;
