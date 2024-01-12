import { APIGatewayProxyWebsocketEventV2, APIGatewayProxyWebsocketHandlerV2 } from 'aws-lambda';
import { ApiGatewayManagementApiClient, PostToConnectionCommand } from '@aws-sdk/client-apigatewaymanagementapi';

import { getConnection, addConnection, removeConnection } from './db';

export const handler: APIGatewayProxyWebsocketHandlerV2 = async (event: APIGatewayProxyWebsocketEventV2) => {
  const apiGwClient = new ApiGatewayManagementApiClient({
    endpoint: `https://${event.requestContext.domainName}/${event.requestContext.stage}`
  });

  const connectionId = event.requestContext.connectionId;

  switch (event.requestContext.eventType) {
    case 'MESSAGE':
      const connected = await getConnection(connectionId);
      if (!connected) {
        await addConnection(connectionId).catch((err) => {
          console.log(err);
        });

        await apiGwClient
          .send(
            new PostToConnectionCommand({
              ConnectionId: connectionId,
              Data: Buffer.from('You are connected for live feed')
            })
          )
          .catch((err) => {
            console.log(err);
          });
      }

      break;

    case 'DISCONNECT':
      await removeConnection(connectionId).catch((err) => {
        console.log('Error sending message to apigw mgmt client', err);
      });
      break;

    default:
  }

  return {
    statusCode: 200
  };
};
