import amqplib, { type Connection, type Channel, type ConfirmChannel } from "amqplib";

const RABBITMQ_URL = process.env["RABBITMQ_URL"] ?? "";
const isConfigured = Boolean(RABBITMQ_URL);

export const QUEUES = {
  PUSH_NOTIFICATIONS: "dusk.push.notifications",
  EMAIL_TRANSACTIONAL: "dusk.email.transactional",
  MEDIA_PROCESS: "dusk.media.process",
  MESSAGES_DELIVER: "dusk.messages.deliver",
  FEED_UPDATE: "dusk.feed.update",
} as const;

export const EXCHANGES = {
  DUSK_EVENTS: "dusk.events",
  DUSK_FANOUT: "dusk.fanout",
} as const;

export type QueueName = (typeof QUEUES)[keyof typeof QUEUES];

export type PushNotificationJob = {
  userId: string;
  fcmToken?: string;
  title: string;
  body: string;
  data?: Record<string, string>;
  priority: "high" | "normal";
};

export type EmailJob = {
  to: string;
  template: "welcome" | "password-reset" | "subscription-confirmed" | "new-follower";
  variables: Record<string, string>;
};

export type MediaProcessJob = {
  userId: string;
  key: string;
  type: "image" | "video";
  operations: Array<"compress" | "thumbnail" | "transcode" | "nsfw-check">;
};

export type MessageDeliverJob = {
  conversationId: string;
  messageId: string;
  senderId: string;
  recipientIds: string[];
  content: string;
  type: "text" | "image" | "voice";
};

export type FeedUpdateJob = {
  authorId: string;
  postId: string;
  followerIds: string[];
  content: string;
};

let connection: Connection | null = null;
let publishChannel: ConfirmChannel | null = null;

async function getConnection(): Promise<Connection> {
  if (!connection) {
    connection = await amqplib.connect(RABBITMQ_URL);
    connection.on("error", (err) => {
      console.error("[rabbitmq] connection error:", err.message);
      connection = null;
      publishChannel = null;
    });
    connection.on("close", () => {
      connection = null;
      publishChannel = null;
    });
  }
  return connection;
}

async function getPublishChannel(): Promise<ConfirmChannel> {
  if (!publishChannel) {
    const conn = await getConnection();
    publishChannel = await conn.createConfirmChannel();
    await setupExchangesAndQueues(publishChannel);
  }
  return publishChannel;
}

async function setupExchangesAndQueues(ch: Channel): Promise<void> {
  await ch.assertExchange(EXCHANGES.DUSK_EVENTS, "topic", { durable: true });
  await ch.assertExchange(EXCHANGES.DUSK_FANOUT, "fanout", { durable: true });

  for (const queue of Object.values(QUEUES)) {
    await ch.assertQueue(queue, {
      durable: true,
      arguments: {
        "x-message-ttl": 86400000,
        "x-dead-letter-exchange": `${queue}.dlx`,
        "x-max-length": 100000,
      },
    });
  }

  await ch.bindQueue(QUEUES.PUSH_NOTIFICATIONS, EXCHANGES.DUSK_EVENTS, "notification.*");
  await ch.bindQueue(QUEUES.FEED_UPDATE, EXCHANGES.DUSK_FANOUT, "");
}

async function enqueue<T>(queue: QueueName, job: T, options?: { priority?: number; delay?: number }): Promise<boolean> {
  if (!isConfigured) {
    console.warn(`[rabbitmq] not configured — dropped job to ${queue}`);
    return false;
  }
  try {
    const ch = await getPublishChannel();
    return new Promise((resolve, reject) => {
      const sent = ch.sendToQueue(
        queue,
        Buffer.from(JSON.stringify(job)),
        {
          persistent: true,
          contentType: "application/json",
          timestamp: Date.now(),
          priority: options?.priority ?? 5,
        },
        (err) => (err ? reject(err) : resolve(true))
      );
      if (!sent) resolve(false);
    });
  } catch (err) {
    console.error(`[rabbitmq] enqueue error on ${queue}:`, err);
    return false;
  }
}

export async function enqueuePushNotification(job: PushNotificationJob): Promise<boolean> {
  return enqueue(QUEUES.PUSH_NOTIFICATIONS, job, { priority: job.priority === "high" ? 9 : 5 });
}

export async function enqueueEmail(job: EmailJob): Promise<boolean> {
  return enqueue(QUEUES.EMAIL_TRANSACTIONAL, job);
}

export async function enqueueMediaProcess(job: MediaProcessJob): Promise<boolean> {
  return enqueue(QUEUES.MEDIA_PROCESS, job, { priority: 3 });
}

export async function enqueueMessageDeliver(job: MessageDeliverJob): Promise<boolean> {
  return enqueue(QUEUES.MESSAGES_DELIVER, job, { priority: 9 });
}

export async function enqueueFeedUpdate(job: FeedUpdateJob): Promise<boolean> {
  return enqueue(QUEUES.FEED_UPDATE, job);
}

export async function consume<T>(
  queue: QueueName,
  handler: (job: T, ack: () => void, nack: (requeue?: boolean) => void) => Promise<void>,
  concurrency = 5
): Promise<void> {
  if (!isConfigured) return;

  const conn = await getConnection();
  const ch = await conn.createChannel();
  await setupExchangesAndQueues(ch);
  ch.prefetch(concurrency);

  await ch.consume(queue, async (msg) => {
    if (!msg) return;
    const ack = () => ch.ack(msg);
    const nack = (requeue = false) => ch.nack(msg, false, requeue);
    try {
      const job = JSON.parse(msg.content.toString()) as T;
      await handler(job, ack, nack);
    } catch (err) {
      console.error(`[rabbitmq] consumer error on ${queue}:`, err);
      nack(false);
    }
  });
}

export async function getQueueStats(queue: QueueName): Promise<{
  messageCount: number;
  consumerCount: number;
} | null> {
  if (!isConfigured) return null;
  try {
    const conn = await getConnection();
    const ch = await conn.createChannel();
    const info = await ch.checkQueue(queue);
    await ch.close();
    return { messageCount: info.messageCount, consumerCount: info.consumerCount };
  } catch {
    return null;
  }
}

export async function disconnectRabbitMQ(): Promise<void> {
  try {
    if (publishChannel) await publishChannel.close();
    if (connection) await connection.close();
  } catch {}
}
