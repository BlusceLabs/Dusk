/**
 * Unified event bus — ties together Kafka (stream analytics/fan-out)
 * and RabbitMQ (job queues) for all Dusk platform events.
 */
import { publishEvent, publishBatch, TOPICS, type DuskEvent } from "./kafka.js";
import {
  enqueuePushNotification,
  enqueueMediaProcess,
  enqueueFeedUpdate,
  enqueueMessageDeliver,
  type PushNotificationJob,
  type MediaProcessJob,
  type FeedUpdateJob,
  type MessageDeliverJob,
} from "./rabbitmq.js";
import { sendEmail } from "./resend.js";
import { incrementTagScore } from "./redis.js";

export async function onPostCreated(data: {
  postId: string;
  authorId: string;
  content: string;
  communityId?: string;
  followerIds?: string[];
  authorName?: string;
}): Promise<void> {
  const ts = Date.now();

  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "post.created",
      postId: data.postId,
      authorId: data.authorId,
      content: data.content,
      communityId: data.communityId,
      ts,
    }),
    publishEvent(TOPICS.ANALYTICS, {
      type: "post.created",
      postId: data.postId,
      authorId: data.authorId,
      content: data.content,
      ts,
    }),

    data.followerIds?.length
      ? enqueueFeedUpdate({
          authorId: data.authorId,
          postId: data.postId,
          followerIds: data.followerIds,
          content: data.content,
        })
      : Promise.resolve(false),

    ...extractHashtags(data.content).map((tag) => incrementTagScore(tag)),
  ]);
}

export async function onPostLiked(data: {
  postId: string;
  userId: string;
  postAuthorId: string;
  postAuthorFcmToken?: string;
  likerName: string;
}): Promise<void> {
  const ts = Date.now();
  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "post.liked",
      postId: data.postId,
      userId: data.userId,
      ts,
    }),

    data.postAuthorFcmToken
      ? enqueuePushNotification({
          userId: data.postAuthorId,
          fcmToken: data.postAuthorFcmToken,
          title: "New like",
          body: `${data.likerName} liked your post`,
          data: { postId: data.postId, type: "like" },
          priority: "normal",
        })
      : Promise.resolve(false),
  ]);
}

export async function onUserFollowed(data: {
  followerId: string;
  followingId: string;
  followerName: string;
  followingFcmToken?: string;
  followingEmail?: string;
}): Promise<void> {
  const ts = Date.now();
  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "user.followed",
      followerId: data.followerId,
      followingId: data.followingId,
      ts,
    }),

    data.followingFcmToken
      ? enqueuePushNotification({
          userId: data.followingId,
          fcmToken: data.followingFcmToken,
          title: "New follower",
          body: `${data.followerName} started following you`,
          data: { userId: data.followerId, type: "follow" },
          priority: "normal",
        })
      : Promise.resolve(false),
  ]);
}

export async function onUserRegistered(data: {
  userId: string;
  email: string;
  displayName: string;
}): Promise<void> {
  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "user.registered",
      userId: data.userId,
      email: data.email,
      ts: Date.now(),
    }),

    sendEmail(data.email, "welcome", { displayName: data.displayName }),
  ]);
}

export async function onMessageSent(data: {
  conversationId: string;
  messageId: string;
  senderId: string;
  senderName: string;
  recipientIds: string[];
  content: string;
  type?: "text" | "image" | "voice";
}): Promise<void> {
  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "message.sent",
      conversationId: data.conversationId,
      senderId: data.senderId,
      ts: Date.now(),
    }),

    enqueueMessageDeliver({
      conversationId: data.conversationId,
      messageId: data.messageId,
      senderId: data.senderId,
      recipientIds: data.recipientIds,
      content: data.content,
      type: data.type ?? "text",
    }),
  ]);
}

export async function onMediaUploaded(data: {
  userId: string;
  key: string;
  type: "image" | "video";
}): Promise<void> {
  await enqueueMediaProcess({
    userId: data.userId,
    key: data.key,
    type: data.type,
    operations:
      data.type === "video"
        ? ["compress", "thumbnail", "transcode", "nsfw-check"]
        : ["compress", "thumbnail", "nsfw-check"],
  });
}

export async function onContentFlagged(data: {
  targetId: string;
  targetType: string;
  reporterId: string;
  reason: string;
}): Promise<void> {
  await Promise.allSettled([
    publishEvent(TOPICS.MODERATION, {
      type: "content.flagged",
      targetId: data.targetId,
      targetType: data.targetType,
      reporterId: data.reporterId,
      reason: data.reason,
      ts: Date.now(),
    }),
  ]);
}

export async function onStreamStarted(data: {
  streamId: string;
  streamerId: string;
  title: string;
  followerIds?: string[];
  streamerName?: string;
}): Promise<void> {
  await Promise.allSettled([
    publishEvent(TOPICS.EVENTS, {
      type: "stream.started",
      streamId: data.streamId,
      streamerId: data.streamerId,
      title: data.title,
      ts: Date.now(),
    }),

    publishEvent(TOPICS.NOTIFICATIONS, {
      type: "stream.started",
      streamId: data.streamId,
      streamerId: data.streamerId,
      title: data.title,
      ts: Date.now(),
    }),
  ]);
}

function extractHashtags(content: string): string[] {
  const matches = content.match(/#(\w+)/g) ?? [];
  return [...new Set(matches.map((t) => t.slice(1).toLowerCase()))].slice(0, 5);
}
