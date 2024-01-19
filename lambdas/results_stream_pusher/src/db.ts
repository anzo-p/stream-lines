import { DeleteItemCommand, DeleteItemCommandInput, DynamoDBClient, QueryCommand, QueryCommandInput } from '@aws-sdk/client-dynamodb';

const connectionsTable = process.env.WS_CONNS_TABLE_NAME;
const connectionsBySymbolInex = process.env.WS_CONNS_BY_SYMBOL_INDEX;

let dbClient: DynamoDBClient | null = null;
export const getDb = () => {
  if (!dbClient) {
    dbClient = new DynamoDBClient({ region: process.env.AWS_REGION });
  }
  return dbClient;
};

export async function queryConnectionIdsBySymbol(symbol: string) {
  const params: QueryCommandInput = {
    TableName: connectionsTable,
    IndexName: connectionsBySymbolInex,
    KeyConditionExpression: 'symbol = :symbol',
    ExpressionAttributeValues: {
      ':symbol': { S: symbol }
    }
  };

  const result = await getDb()
    .send(new QueryCommand(params))
    .catch((err) => {
      console.error(err);
      return { Items: [] };
    });

  return result.Items || [];
}

export async function removeConnection(connectionId: string): Promise<void> {
  const params: DeleteItemCommandInput = {
    TableName: connectionsTable,
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
