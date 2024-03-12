import { GraphQLClient } from 'graphql-request';

let client: GraphQLClient | null = null;
if (!client) {
    client = new GraphQLClient('https://sl-otto.anzop.net/graphql');
}

export const graphQLClient = client;
