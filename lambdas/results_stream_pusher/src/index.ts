import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { KinesisStreamEvent, KinesisStreamHandler } from 'aws-lambda';
import { queryConnectionIdsBySymbol, removeConnection } from './db';

const apiGwClient = new ApiGatewayManagementApiClient({
  endpoint: process.env.API_GW_CONNECTIONS_URL
});

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
  const symbol: string = message?.symbol;
  if (!symbol) return;

  const connectionIds = await queryConnectionIdsBySymbol(symbol);
  connectionIds.forEach(async (item) => {
    const connectionId = item.connectionId.S;
    if (connectionId) {
      const messages = [message];
      await sendMessage(connectionId, messages);
    }
  });
};

const sendMessage = async (connectionId: string, messages: any[]) => {
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
