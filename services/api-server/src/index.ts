import app from "./app";
import { logger } from "./lib/logger";
import { consume, QUEUES, type PushNotificationJob } from "./lib/rabbitmq.js";
import { sendExpoPushNotification, isExpoPushToken } from "./lib/push.js";
import { getPushToken } from "./routes/push.js";

const rawPort = process.env["PORT"];

if (!rawPort) {
  throw new Error(
    "PORT environment variable is required but was not provided.",
  );
}

const port = Number(rawPort);

if (Number.isNaN(port) || port <= 0) {
  throw new Error(`Invalid PORT value: "${rawPort}"`);
}

async function startPushConsumer() {
  try {
    await consume<PushNotificationJob>(
      QUEUES.PUSH_NOTIFICATIONS,
      async (job, ack, nack) => {
        try {
          let token = job.fcmToken;

          if (!token || !isExpoPushToken(token)) {
            token = await getPushToken(job.userId) ?? undefined;
          }

          if (!token || !isExpoPushToken(token)) {
            logger.warn({ userId: job.userId }, "[push] no valid Expo token — skipping");
            ack();
            return;
          }

          const [ticket] = await sendExpoPushNotification({
            to: token,
            title: job.title,
            body: job.body,
            data: job.data ?? {},
            sound: "default",
            priority: job.priority === "high" ? "high" : "normal",
          });

          if (ticket.status === "error") {
            logger.error({ ticket, userId: job.userId }, "[push] delivery failed");
          } else {
            logger.info({ ticketId: ticket.id, userId: job.userId }, "[push] sent");
          }

          ack();
        } catch (err) {
          logger.error({ err, userId: job.userId }, "[push] consumer error");
          nack(false);
        }
      },
      3
    );
    logger.info("[push] consumer started");
  } catch (err) {
    logger.warn({ err }, "[push] consumer failed to start — continuing without it");
  }
}

app.listen(port, async (err) => {
  if (err) {
    logger.error({ err }, "Error listening on port");
    process.exit(1);
  }

  logger.info({ port }, "Server listening");
  startPushConsumer();
});
