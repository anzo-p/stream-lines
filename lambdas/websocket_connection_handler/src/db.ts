import { DeleteItemCommand, DeleteItemCommandInput, DynamoDBClient, PutItemCommand, PutItemCommandInput } from '@aws-sdk/client-dynamodb';

const region = process.env.AWS_REGION;
const connectionTable = process.env.WS_CONNS_TABLE_NAME;

let dbClient: DynamoDBClient | null = null;
const getDb = () => {
  if (!dbClient) {
    dbClient = new DynamoDBClient({ region });
  }
  return dbClient;
};

export async function subscribeToFeeds(connectionId: string, symbols: string[]): Promise<void> {
  await removeConnection(connectionId);

  for (const symbol of symbols) {
    const params: PutItemCommandInput = {
      TableName: connectionTable,
      Item: {
        symbol: { S: symbol },
        connectionId: { S: connectionId }
      }
    };

    await getDb()
      .send(new PutItemCommand(params))
      .catch((err) => {
        console.error(`Error subscribing to symbol: $symbol`, err);
      });
  }
}

export async function removeConnection(connectionId: string): Promise<void> {
  const params: DeleteItemCommandInput = {
    TableName: connectionTable,
    Key: {
      connectionId: { S: connectionId }
    }
  };

  await getDb()
    .send(new DeleteItemCommand(params))
    .catch((err) => {
      console.error('Error removing connection:', err);
    });
}
