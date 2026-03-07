import { EventBridgeEvent } from 'aws-lambda';
import { CopyObjectCommand, S3Client } from '@aws-sdk/client-s3';

const FILENAME = requiredEnv('FILENAME');
const SOURCE_PATH = normalizePath(requiredEnv('SOURCE_PATH'));
const TARGET_PATH = normalizePath(requiredEnv('TARGET_PATH'));

type S3ObjectCreatedDetail = {
  bucket: { name: string };
  object: { key: string };
};

const s3 = new S3Client({});

export const handler = async (event: EventBridgeEvent<'Object Created', S3ObjectCreatedDetail>) => {
  if (TARGET_PATH.startsWith(SOURCE_PATH)) {
    console.error(`Source: "${SOURCE_PATH}" and target: "${TARGET_PATH}" paths must not overlap!`);
    return;
  }

  const bucket = event.detail?.bucket?.name;
  const rawKey = event.detail?.object?.key;
  if (!bucket || !rawKey) return;

  const eventS3Key = decodeURIComponent(rawKey.replace(/\+/g, ' '));
  if (!eventS3Key.endsWith(FILENAME)) return;

  await s3.send(
    new CopyObjectCommand({
      Bucket: bucket,
      CopySource: `${bucket}/${eventS3Key}`,
      Key: `${TARGET_PATH}${FILENAME}`
    })
  );
};

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value || value.trim() === '') {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function normalizePath(prefix: string): string {
  const trimmed = prefix.trim().replace(/^\/+|\/+$/g, '');
  return trimmed === '' ? '' : `${trimmed}/`;
}
