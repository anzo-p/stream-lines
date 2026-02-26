import { EventBridgeEvent } from 'aws-lambda';
import { S3Client, CopyObjectCommand } from '@aws-sdk/client-s3';

const filename = process.env.FILENAME!;
const latestKey = process.env.LATEST_KEY!;

type S3ObjectCreatedDetail = {
  bucket: { name: string };
  object: { key: string };
};

const s3 = new S3Client({});

export const handler = async (event: EventBridgeEvent<'Object Created', S3ObjectCreatedDetail>) => {
  const bucket = event?.detail?.bucket?.name as string | undefined;
  const rawKey = event?.detail?.object?.key as string | undefined;
  if (!bucket || !rawKey) return;

  const key = decodeURIComponent(event.detail.object.key.replace(/\+/g, ' '));
  if (!key.endsWith(filename)) return;

  await s3.send(
    new CopyObjectCommand({
      Bucket: bucket,
      Key: latestKey,
      CopySource: `${bucket}/${key}`
    })
  );
};
