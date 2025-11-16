import { gql, GraphQLClient } from 'graphql-request';
import type { WindowedQuotation } from '../../types/WindowedQuotation';

export async function fetchWindowedQuotations(
  client: GraphQLClient,
  symbol: string,
  startTime: number,
  endTime: number
): Promise<WindowedQuotation[]> {
  const variables = {
    symbol,
    startTime,
    endTime
  };

  try {
    const result = await client.request<{ getWindowedQuotationData: WindowedQuotation[] }>(query, variables);
    return result.getWindowedQuotationData;
  } catch (err) {
    console.error('Error fetching WindowedQuotation:', err);
    throw new Error('Error fetching WindowedQuotation: ' + err);
  }
}

const query = gql`
  query GetWindowedQuotationData($symbol: String!, $startTime: Float!, $endTime: Float!) {
    getWindowedQuotationData(input: { symbol: $symbol, startTime: $startTime, endTime: $endTime }) {
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
