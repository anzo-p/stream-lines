import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { KinesisStreamEvent, KinesisStreamHandler } from 'aws-lambda';
import { queryConnectionIdsBySymbol, removeConnection } from './db';
import { Measurement, isMeasurement } from './types';
import { compress } from './helper';

const apiGwClient = new ApiGatewayManagementApiClient({
  endpoint: process.env.API_GW_CONNECTIONS_URL
});

export const handler: KinesisStreamHandler = async (event: KinesisStreamEvent) => {
  const subscribersMessages: Record<string, Measurement[]> = {};

  async function processMessage(message: any) {
    (await queryConnectionIdsBySymbol(message.symbol)).forEach(async (item) => {
      const connectionId = item.connectionId.S;
      if (connectionId) {
        if (!subscribersMessages[connectionId]) {
          subscribersMessages[connectionId] = [];
        }
        subscribersMessages[connectionId].push(message);
      }
    });

    for (const [connectionId, messages] of Object.entries(subscribersMessages)) {
      await sendMessage(connectionId, messages);
      delete subscribersMessages[connectionId];
    }
  }

  for (const record of event.Records) {
    const payload = Buffer.from(record.kinesis.data, 'base64').toString('ascii');

    try {
      const message = JSON.parse(payload);
      if (isMeasurement(message)) {
        await processMessage(message);
      } else {
        console.error(`Invalid message from kinesis: ${JSON.stringify(message)}`);
        return;
      }
    } catch (err) {
      console.error(`Error parsing payload: ${payload}`);
      continue;
    }
  }
};

const sendMessage = async (connectionId: string, messages: Measurement[]) => {
  try {
    const batches: Measurement[][] = [];
    while (messages.length > 0) {
      batches.push(messages.splice(0, 7));
    }

    batches.forEach(async (batch) => {
      const compressed = compress(JSON.stringify(batch));

      await apiGwClient
        .send(
          new PostToConnectionCommand({
            ConnectionId: connectionId,
            Data: compressed
          })
        )
        .catch(async (err) => {
          if (err.statusCode === 410) {
            console.error(`Messages not sent due to stale connection, removing: ${connectionId}`);
            await removeConnection(connectionId);
          }
        });
    });
  } catch (err) {
    console.error(`Error sending to connection ${connectionId}:`, err);
  }
};
