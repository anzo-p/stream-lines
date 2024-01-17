import { gql, GraphQLClient } from 'graphql-request';

import type { CryptoQuotation } from '../types/CryptoQuotation';

const graphQLClient = new GraphQLClient('http://127.0.0.1:3030/graphql', {
    mode: `cors`
});

const query = gql`
    query GetWindowedQuotationData($symbol: String!, $startTime: Float!, $endTime: Float!) {
        getWindowedQuotationData(
            input: { symbol: $symbol, startTime: $startTime, endTime: $endTime }
        ) {
            measureId
            measurement
            symbol
            windowStartTime
            windowEndTime
            sumBidVolume
            sumAskVolume
            recordCount
            averageBidPrice
            averageAskPrice
            bidPriceAtWindowEnd
            askPriceAtWindowEnd
        }
    }
`;

export async function fetchCryptoQuotations(
    symbol: string,
    startTime: number,
    endTime: number
): Promise<CryptoQuotation[]> {
    const variables = {
        symbol,
        startTime,
        endTime
    };

    try {
        const result = await graphQLClient.request<{ getWindowedQuotationData: CryptoQuotation[] }>(
            query,
            variables
        );
        return result.getWindowedQuotationData;
    } catch (err) {
        console.error('Error fetching CryptoQuotations:', err);
        throw new Error('Error fetching CryptoQuotations: ' + err);
    }
}
