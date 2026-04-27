import express, { type Express, type Request, type Response, type NextFunction } from "express";
import cors from "cors";
import helmet from "helmet";
import { rateLimit } from "express-rate-limit";
import pinoHttp from "pino-http";
import router from "./routes/index.js";
import { logger } from "./lib/logger.js";
import { initNeonSchema } from "./lib/neon.js";
import { ensureTopics, disconnectKafka } from "./lib/kafka.js";
import { disconnectRabbitMQ } from "./lib/rabbitmq.js";

const app: Express = express();

app.set("trust proxy", 1);

app.use(
  helmet({
    crossOriginEmbedderPolicy: false,
    crossOriginResourcePolicy: { policy: "cross-origin" },
  }),
);

const allowedOrigins = [
  /\.replit\.dev$/,
  /\.replit\.app$/,
  /\.expo\.dev$/,
  /localhost/,
];

app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin) return callback(null, true);
      const allowed = allowedOrigins.some((pattern) =>
        typeof pattern === "string" ? origin === pattern : pattern.test(origin),
      );
      if (allowed) return callback(null, true);
      callback(new Error(`CORS: origin ${origin} not allowed`));
    },
    credentials: true,
  }),
);

const globalLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: "Too many requests, please slow down." },
  skip: (req) => req.method === "OPTIONS",
});

app.use(globalLimiter);

app.use(
  pinoHttp({
    logger,
    serializers: {
      req(req) {
        return {
          id: req.id,
          method: req.method,
          url: req.url?.split("?")[0],
        };
      },
      res(res) {
        return {
          statusCode: res.statusCode,
        };
      },
    },
  }),
);

app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));

app.use("/api", router);

app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  if (err.message?.startsWith("CORS:")) {
    res.status(403).json({ error: "Forbidden" });
    return;
  }
  logger.error({ err }, "Unhandled error");
  res.status(500).json({ error: "Internal server error" });
});

initNeonSchema()
  .then(() => logger.info("Neon schema initialized"))
  .catch((err) => logger.warn({ err }, "Neon schema init failed (may already exist)"));

ensureTopics()
  .then(() => logger.info("Kafka topics ready"))
  .catch((err) => logger.warn({ err }, "Kafka topic setup skipped (credentials not configured)"));

const shutdown = async (signal: string) => {
  logger.info(`${signal} received — shutting down`);
  await Promise.allSettled([disconnectKafka(), disconnectRabbitMQ()]);
  process.exit(0);
};
process.once("SIGTERM", () => shutdown("SIGTERM"));
process.once("SIGINT", () => shutdown("SIGINT"));

export default app;
