import type { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import jwksRsa from "jwks-rsa";
import { logger } from "../lib/logger.js";

const client = jwksRsa({
  jwksUri:
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
  cache: true,
  cacheMaxAge: 86400000,
  rateLimit: true,
});

const PROJECT_ID = process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID ?? "duskapp26";

async function verifyFirebaseToken(token: string): Promise<{ uid: string }> {
  const decoded = jwt.decode(token, { complete: true });
  if (!decoded?.header?.kid) throw new Error("Invalid token structure");

  const key = await client.getSigningKeyAsync(decoded.header.kid);
  const signingKey = key.getPublicKey();

  const verified = jwt.verify(token, signingKey, {
    algorithms: ["RS256"],
    audience: PROJECT_ID,
    issuer: `https://securetoken.google.com/${PROJECT_ID}`,
  }) as jwt.JwtPayload;

  if (!verified.sub) throw new Error("No subject in token");
  return { uid: verified.sub };
}

declare global {
  namespace Express {
    interface Request {
      user?: { uid: string };
    }
  }
}

export function requireAuth(optional = false) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    const header = req.headers.authorization;
    if (!header?.startsWith("Bearer ")) {
      if (optional) { next(); return; }
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const token = header.slice(7);
    try {
      req.user = await verifyFirebaseToken(token);
      next();
    } catch (err) {
      logger.warn({ err }, "[auth] token verification failed");
      if (optional) { next(); return; }
      res.status(401).json({ error: "Unauthorized" });
    }
  };
}
