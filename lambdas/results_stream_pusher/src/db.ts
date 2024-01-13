import { DeleteItemCommand, DeleteItemCommandInput, DynamoDBClient, ScanCommand, ScanCommandOutput } from '@aws-sdk/client-dynamodb';

const connectionsTable = process.env.WS_CONNS_TABLE_NAME;

let dbClient: DynamoDBClient | null = null;
const getDb = () => {
  if (!dbClient) {
    dbClient = new DynamoDBClient({ region: process.env.AWS_REGION });
  }
  return dbClient;
};

export async function getActiveConnections(): Promise<string[]> {
  const params = {
    TableName: connectionsTable,
    ProjectionExpression: 'connectionId'
  };

  const fetch: ScanCommandOutput | void = await getDb()
    .send(new ScanCommand(params))
    .catch((err) => {
      console.log('Error scanning connections', err);
      return undefined;
    });

  const result: string[] = fetch?.Items?.map((item) => item.connectionId.S).filter((id): id is string => id !== undefined) ?? [];

  return result;
}

export async function removeStaleConnection(connectionId: string): Promise<void> {
  const params: DeleteItemCommandInput = {
    TableName: connectionsTable,
    Key: {
      connectionId: { S: connectionId }
    }
  };

  const command: DeleteItemCommand = new DeleteItemCommand(params);

  await getDb()
    .send(command)
    .catch((err) => {
      console.error('Error removing connection:', err);
    });
}
