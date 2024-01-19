import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { KinesisStreamEvent, KinesisStreamHandler } from 'aws-lambda';
import { queryConnectionIdsBySymbol, removeConnection } from './db';

const apiGwClient = new ApiGatewayManagementApiClient({
  endpoint: process.env.API_GW_CONNECTIONS_URL
});

export const handler: KinesisStreamHandler = async (event: KinesisStreamEvent) => {
  const messages = [];
  const messagesOut: { [key: string]: any } = {};

  for (const record of event.Records) {
    const payload = Buffer.from(record.kinesis.data, 'base64').toString('ascii');

    let data;
    try {
      data = JSON.parse(payload);
      messages.push(data);
    } catch (err) {
      console.error(`Error parsing payload: ${payload}`);
      continue;
    }

    const symbol: string = data?.symbol;
    if (symbol && !messagesOut[symbol]) {
      messagesOut[symbol] = {};
    }
  }

  for (const symbol of Object.keys(messagesOut)) {
    const connectionIds = await queryConnectionIdsBySymbol(symbol);

    connectionIds.forEach((item) => {
      const connectionId = item.connectionId.S;
      if (connectionId) {
        messagesOut[symbol][connectionId] = [];
      }
    });
  }

  for (const message of messages) {
    const symbol = message?.symbol;
    if (symbol) {
      for (const connectionId of Object.keys(messagesOut[symbol])) {
        messagesOut[symbol][connectionId].push(message);
      }
    }
  }

  for (const symbol of Object.keys(messagesOut)) {
    for (const connectionId of Object.keys(messagesOut[symbol])) {
      const messages = messagesOut[symbol][connectionId];

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
          } else {
            console.error(`Error sending to connection ${connectionId}:`, err);
          }
        });
    }
  }
};
