import _ from 'lodash';
import camelCase from 'lodash.camelcase';

export type WindowedQuotation = {
  askPriceAtWindowEnd: number;
  averageAskPrice: number;
  averageBidPrice: number;
  bidPriceAtWindowEnd: number;
  measureId: string;
  measurement: string;
  recordCount: number;
  sumAskVolume: number;
  sumBidVolume: number;
  symbol: string;
  tags: {
    symbol: string;
  };
  windowEndTime: number;
  windowStartTime: number;
};

export function readQuotationFromJson(data: any): WindowedQuotation | null {
  const camelCaseData = _.mapKeys(data, (_, key) => camelCase(key));
  if (validateQuotation(camelCaseData)) {
    return camelCaseData;
  } else {
    return null;
  }
}

function validateQuotation(data: any): data is WindowedQuotation {
  return (
    typeof data === 'object' &&
    typeof data.askPriceAtWindowEnd === 'number' &&
    typeof data.averageAskPrice === 'number' &&
    typeof data.averageBidPrice === 'number' &&
    typeof data.bidPriceAtWindowEnd === 'number' &&
    typeof data.measureId === 'string' &&
    typeof data.measurement === 'string' &&
    typeof data.recordCount === 'number' &&
    typeof data.sumAskVolume === 'number' &&
    typeof data.sumBidVolume === 'number' &&
    typeof data.symbol === 'string' &&
    typeof data.tags === 'object' &&
    typeof data.tags.symbol === 'string' &&
    typeof data.windowEndTime === 'number' &&
    typeof data.windowStartTime === 'number'
  );
}
