import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { KinesisStreamEvent, KinesisStreamHandler } from 'aws-lambda';
import { getActiveConnections, removeStaleConnection } from './db';

const apiGwClient = new ApiGatewayManagementApiClient({
  endpoint: process.env.API_GW_CONNECTIONS_URL
});

export const handler: KinesisStreamHandler = async (event: KinesisStreamEvent) => {
  const activeConnections = await getActiveConnections();

  for (const record of event.Records) {
    const payload = Buffer.from(record.kinesis.data, 'base64').toString('ascii');
    const data = JSON.parse(payload);

    for (const connectionId of activeConnections) {
      await apiGwClient
        .send(
          new PostToConnectionCommand({
            ConnectionId: connectionId,
            Data: JSON.stringify(data)
          })
        )
        .catch(async (err) => {
          if (err.statusCode === 410) {
            console.error(`Stale connection, removing: ${connectionId}`);
            await removeStaleConnection(connectionId);
          } else {
            console.error(`Error sending to connection ${connectionId}:`, err);
          }
        });
    }
  }
};
