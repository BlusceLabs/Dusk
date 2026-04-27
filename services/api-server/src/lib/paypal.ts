const PAYPAL_BASE =
  process.env["PAYPAL_ENV"] === "live"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";

const CLIENT_ID = process.env["PAYPAL_CLIENT_ID"]!;
const CLIENT_SECRET = process.env["PAYPAL_CLIENT_SECRET"]!;

let _token: string | null = null;
let _tokenExpiry = 0;

export async function getAccessToken(): Promise<string> {
  if (_token && Date.now() < _tokenExpiry) return _token;

  const res = await fetch(`${PAYPAL_BASE}/v1/oauth2/token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Basic ${Buffer.from(`${CLIENT_ID}:${CLIENT_SECRET}`).toString("base64")}`,
    },
    body: "grant_type=client_credentials",
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`PayPal auth failed: ${err}`);
  }

  const data = await res.json() as { access_token: string; expires_in: number };
  _token = data.access_token;
  _tokenExpiry = Date.now() + (data.expires_in - 60) * 1000;
  return _token;
}

export async function createOrder(opts: {
  planId: string;
  planName: string;
  priceCents: number;
  billing: "monthly" | "yearly";
  returnUrl: string;
  cancelUrl: string;
}): Promise<{ orderId: string; approveUrl: string }> {
  const token = await getAccessToken();
  const amount = (opts.priceCents / 100).toFixed(2);

  const res = await fetch(`${PAYPAL_BASE}/v2/checkout/orders`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "PayPal-Request-Id": `dusk-${opts.planId}-${Date.now()}`,
    },
    body: JSON.stringify({
      intent: "CAPTURE",
      purchase_units: [
        {
          reference_id: opts.planId,
          description: `Dusk ${opts.planName} — ${opts.billing} subscription`,
          amount: {
            currency_code: "USD",
            value: amount,
          },
        },
      ],
      payment_source: {
        paypal: {
          experience_context: {
            brand_name: "Dusk",
            user_action: "PAY_NOW",
            return_url: opts.returnUrl,
            cancel_url: opts.cancelUrl,
          },
        },
      },
    }),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`PayPal createOrder failed: ${err}`);
  }

  const data = await res.json() as {
    id: string;
    links: { rel: string; href: string }[];
  };

  const approveUrl = data.links.find((l) => l.rel === "payer-action")?.href ?? "";
  return { orderId: data.id, approveUrl };
}

export async function captureOrder(orderId: string): Promise<{
  status: string;
  payerId: string;
  email: string;
  amount: string;
}> {
  const token = await getAccessToken();

  const res = await fetch(`${PAYPAL_BASE}/v2/checkout/orders/${orderId}/capture`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`PayPal capture failed: ${err}`);
  }

  const data = await res.json() as any;
  const payer = data.payer ?? {};
  const capture = data.purchase_units?.[0]?.payments?.captures?.[0] ?? {};

  return {
    status: data.status,
    payerId: payer.payer_id ?? "",
    email: payer.email_address ?? "",
    amount: capture.amount?.value ?? "0",
  };
}
