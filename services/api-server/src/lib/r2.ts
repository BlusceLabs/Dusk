import { S3Client, PutObjectCommand, DeleteObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";

const accountId = process.env["R2_ACCOUNT_ID"]!;
const accessKeyId = process.env["R2_ACCESS_KEY_ID"]!;
const secretAccessKey = process.env["R2_SECRET_ACCESS_KEY"]!;
const bucket = process.env["R2_BUCKET_NAME"]!;
const publicUrl = process.env["R2_PUBLIC_URL"]!;

export const r2 = new S3Client({
  region: "auto",
  endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
  credentials: { accessKeyId, secretAccessKey },
});

export async function getPresignedUploadUrl(
  key: string,
  contentType: string,
  expiresIn = 300
): Promise<string> {
  const cmd = new PutObjectCommand({ Bucket: bucket, Key: key, ContentType: contentType });
  return getSignedUrl(r2, cmd, { expiresIn });
}

export async function getPresignedDownloadUrl(key: string, expiresIn = 3600): Promise<string> {
  const cmd = new GetObjectCommand({ Bucket: bucket, Key: key });
  return getSignedUrl(r2, cmd, { expiresIn });
}

export async function deleteObject(key: string): Promise<void> {
  await r2.send(new DeleteObjectCommand({ Bucket: bucket, Key: key }));
}

export function getPublicUrl(key: string): string {
  return `${publicUrl}/${key}`;
}

export function buildMediaKey(
  type: "avatar" | "post" | "story" | "banner",
  userId: string,
  filename: string
): string {
  return `${type}/${userId}/${filename}`;
}
