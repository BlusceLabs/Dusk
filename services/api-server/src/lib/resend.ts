/**
 * Resend email integration — uses RESEND_API_KEY + RESEND_FROM_EMAIL env vars.
 */
import { Resend } from "resend";

function getCredentials(): { apiKey: string; fromEmail: string } {
  const apiKey = process.env["RESEND_API_KEY"];
  const fromEmail = process.env["RESEND_FROM_EMAIL"] ?? "onboarding@resend.dev";
  if (!apiKey) throw new Error("RESEND_API_KEY not set");
  return { apiKey, fromEmail };
}

export function getUncachableResendClient() {
  const { apiKey, fromEmail } = getCredentials();
  return { client: new Resend(apiKey), fromEmail };
}

export type EmailTemplate = "welcome" | "password-reset" | "subscription-confirmed" | "new-follower";

const TEMPLATES: Record<EmailTemplate, (vars: Record<string, string>) => { subject: string; html: string }> = {
  welcome: (v) => ({
    subject: "Welcome to Dusk 🌅",
    html: `
      <div style="font-family:sans-serif;max-width:560px;margin:0 auto;background:#0d0b10;color:#f8f7f4;padding:40px;border-radius:12px">
        <h1 style="color:#ff8c5a;margin-bottom:8px">Welcome to Dusk, ${v["displayName"] ?? "there"}! 🌅</h1>
        <p style="color:#a0a0b0;font-size:16px;line-height:1.6">
          You're now part of the Dusk community — the social super-app where creators, communities, and conversations come alive.
        </p>
        <div style="margin:32px 0">
          <a href="https://dusk.app" style="background:#ff8c5a;color:#fff;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:600">
            Open Dusk
          </a>
        </div>
        <p style="color:#606070;font-size:13px">You received this because you signed up for Dusk.</p>
      </div>
    `,
  }),

  "password-reset": (v) => ({
    subject: "Reset your Dusk password",
    html: `
      <div style="font-family:sans-serif;max-width:560px;margin:0 auto;background:#0d0b10;color:#f8f7f4;padding:40px;border-radius:12px">
        <h1 style="color:#ff8c5a">Reset your password</h1>
        <p style="color:#a0a0b0;font-size:16px">Click the button below to reset your Dusk password. This link expires in 1 hour.</p>
        <div style="margin:32px 0">
          <a href="${v["resetUrl"] ?? "#"}" style="background:#ff8c5a;color:#fff;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:600">
            Reset Password
          </a>
        </div>
        <p style="color:#606070;font-size:13px">If you didn't request this, ignore this email.</p>
      </div>
    `,
  }),

  "subscription-confirmed": (v) => ({
    subject: `You're now subscribed to ${v["creatorName"] ?? "a creator"}`,
    html: `
      <div style="font-family:sans-serif;max-width:560px;margin:0 auto;background:#0d0b10;color:#f8f7f4;padding:40px;border-radius:12px">
        <h1 style="color:#f4d03f">Subscription confirmed ✨</h1>
        <p style="color:#a0a0b0;font-size:16px">
          You're now subscribed to <strong style="color:#f8f7f4">${v["creatorName"] ?? "this creator"}</strong> 
          on the <strong style="color:#ff8c5a">${v["tierName"] ?? "Creator"}</strong> tier.
        </p>
        <p style="color:#a0a0b0;font-size:16px">You now have access to all their exclusive content.</p>
        <div style="margin:32px 0">
          <a href="https://dusk.app" style="background:#f4d03f;color:#0d0b10;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:600">
            View Content
          </a>
        </div>
      </div>
    `,
  }),

  "new-follower": (v) => ({
    subject: `${v["followerName"] ?? "Someone"} is now following you on Dusk`,
    html: `
      <div style="font-family:sans-serif;max-width:560px;margin:0 auto;background:#0d0b10;color:#f8f7f4;padding:40px;border-radius:12px">
        <h1 style="color:#ff8c5a">New follower! 🎉</h1>
        <p style="color:#a0a0b0;font-size:16px">
          <strong style="color:#f8f7f4">${v["followerName"] ?? "Someone"}</strong> started following you on Dusk.
        </p>
        <div style="margin:32px 0">
          <a href="https://dusk.app" style="background:#ff8c5a;color:#fff;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:600">
            Check it out
          </a>
        </div>
      </div>
    `,
  }),
};

export async function sendEmail(
  to: string,
  template: EmailTemplate,
  variables: Record<string, string> = {}
): Promise<{ id?: string; error?: string }> {
  try {
    const { client, fromEmail } = await getUncachableResendClient();
    const { subject, html } = TEMPLATES[template](variables);

    const { data, error } = await client.emails.send({
      from: `Dusk <${fromEmail}>`,
      to,
      subject,
      html,
    });

    if (error) {
      console.error("[resend] send error:", error);
      return { error: error.message };
    }

    return { id: data?.id };
  } catch (err: any) {
    console.error("[resend] fatal error:", err.message);
    return { error: err.message };
  }
}
