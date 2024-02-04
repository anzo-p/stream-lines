import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { KinesisStreamEvent, KinesisStreamHandler } from 'aws-lambda';
import { queryConnectionIdsBySymbol, removeConnection } from './db';
import { Measurement, isMeasurement } from './types';

const apiGwClient = new ApiGatewayManagementApiClient({
  endpoint: process.env.API_GW_CONNECTIONS_URL
});

const subscribersMessages: Record<string, Measurement[]> = {};

export const handler: KinesisStreamHandler = async (event: KinesisStreamEvent) => {
  for (const record of event.Records) {
    const payload = Buffer.from(record.kinesis.data, 'base64').toString('ascii');

    try {
      const data = JSON.parse(payload);
      await processMessage(data);
    } catch (err) {
      console.error(`Error parsing payload: ${payload}`);
      continue;
    }
  }
};

const processMessage = async (message: any) => {
  if (!isMeasurement(message)) {
    console.error(`Invalid message from kinesis: ${JSON.stringify(message)}`);
    return;
  }

  const connectionIds = await queryConnectionIdsBySymbol(message.symbol);
  connectionIds.forEach(async (item) => {
    const connectionId = item.connectionId.S;
    if (connectionId) {
      await poolOrFlushMessages(connectionId, message);
    }
  });

  for (const [connectionId, messages] of Object.entries(subscribersMessages)) {
    await sendMessage(connectionId, messages);
  }
};

const poolOrFlushMessages = async (connectionId: string, message: Measurement) => {
  if (!subscribersMessages[connectionId]) {
    subscribersMessages[connectionId] = [];
  }

  subscribersMessages[connectionId].push(message);

  if (subscribersMessages[connectionId].length >= 3) {
    await sendMessage(connectionId, subscribersMessages[connectionId]);
    subscribersMessages[connectionId] = [];
  }
};

const sendMessage = async (connectionId: string, messages: Measurement[]) => {
  try {
    await apiGwClient
      .send(
        new PostToConnectionCommand({
          ConnectionId: connectionId,
          Data: JSON.stringify(messages)
        })
      )
      .catch(async (err) => {
        if (err.statusCode === 410) {
          console.error(`Messages not sent due to stale connection, removing: ${connectionId}`);
          await removeConnection(connectionId);
        }
      });
  } catch (err) {
    console.error(`Error sending to connection ${connectionId}:`, err);
  }
};
