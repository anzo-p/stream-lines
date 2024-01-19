import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';
import { APIGatewayProxyWebsocketEventV2, APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import { removeConnection, subscribeToFeeds } from './db';
import { isReceivedMessage } from './types';

export const handler: APIGatewayProxyWebsocketHandlerV2 = async (event: APIGatewayProxyWebsocketEventV2) => {
  const apiGwClient = new ApiGatewayManagementApiClient({
    endpoint: `https://${event.requestContext.domainName}/${event.requestContext.stage}`
  });

  const connectionId = event.requestContext.connectionId;

  if (event.requestContext.eventType === 'MESSAGE') {
    const body = event?.body;
    if (body) {
      try {
        const parsedBody = JSON.parse(body);
        if (isReceivedMessage(parsedBody)) {
          await subscribeToFeeds(connectionId, parsedBody.subscribeTo).catch((err: string) => {
            console.log('Error subscribing to symbols', err);
          });

          await apiGwClient
            .send(
              new PostToConnectionCommand({
                ConnectionId: connectionId,
                Data: Buffer.from(
                  JSON.stringify({
                    message: `You are connected for live feed for symbols: ${parsedBody.subscribeTo.join(', ')}`
                  })
                )
              })
            )
            .catch(console.log);
        }
      } catch (err) {
        console.log('Message is not JSON or typeof ReceivedMessage', err);
      }
    }
  } else if (event.requestContext.eventType === 'DISCONNECT') {
    await removeConnection(connectionId).catch(console.log);
  }

  return { statusCode: 200 };
};
