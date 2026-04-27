/**
 * Expo Push Notification sender.
 * Uses the Expo Push API (https://exp.host/--/api/v2/push/send)
 * which handles both FCM (Android) and APNs (iOS) under the hood.
 */

export interface ExpoPushMessage {
  to: string | string[];
  title?: string;
  body?: string;
  data?: Record<string, string>;
  sound?: "default" | null;
  badge?: number;
  priority?: "default" | "normal" | "high";
}

export interface ExpoPushTicket {
  status: "ok" | "error";
  id?: string;
  message?: string;
  details?: Record<string, unknown>;
}

const EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

export async function sendExpoPushNotification(
  messages: ExpoPushMessage | ExpoPushMessage[]
): Promise<ExpoPushTicket[]> {
  const payload = Array.isArray(messages) ? messages : [messages];

  const res = await fetch(EXPO_PUSH_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      "Accept-Encoding": "gzip, deflate",
    },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    throw new Error(`Expo Push API error: ${res.status} ${await res.text()}`);
  }

  const json = await res.json() as { data: ExpoPushTicket[] };
  return json.data;
}

export function isExpoPushToken(token: string): boolean {
  return token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
}
