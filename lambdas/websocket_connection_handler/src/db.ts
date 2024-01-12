import {
  AttributeValue,
  DeleteItemCommand,
  DeleteItemCommandInput,
  DynamoDBClient,
  GetItemCommand,
  GetItemInput,
  PutItemCommand,
  PutItemCommandInput
} from '@aws-sdk/client-dynamodb';

const region = process.env.AWS_REGION;
const connectionsTable = process.env.WS_CONNS_TABLE_NAME;

let dbClient: DynamoDBClient | null = null;

const getDb = () => {
  if (!dbClient) {
    dbClient = new DynamoDBClient({ region });
  }
  return dbClient;
};

export async function getConnection(connectionId: string): Promise<Record<string, AttributeValue> | undefined> {
  const params: GetItemInput = {
    TableName: connectionsTable,
    Key: {
      connectionId: { S: connectionId }
    },
    ConsistentRead: true,
    ProjectionExpression: 'connectionId'
  };

  const result = await getDb()
    .send(new GetItemCommand(params))
    .catch((err) => {
      console.error('Error getting connection:', err);
    });

  return result?.Item;
}

export async function addConnection(connectionId: string): Promise<void> {
  const params: PutItemCommandInput = {
    TableName: connectionsTable,
    Item: {
      connectionId: { S: connectionId }
    }
  };

  const command: PutItemCommand = new PutItemCommand(params);

  await getDb()
    .send(command)
    .catch((err) => {
      console.error('Error adding connection:', err);
    });
}

export async function removeConnection(connectionId: string): Promise<void> {
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
