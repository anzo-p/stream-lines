import { GraphQLClient } from 'graphql-request';

let client: GraphQLClient | null = null;
if (!client) {
    client = new GraphQLClient(import.meta.env.VITE_DATABASE_URL);
}

export const graphQLClient = client;
