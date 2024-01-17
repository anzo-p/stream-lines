import _ from 'lodash';
import camelCase from 'lodash.camelcase';

export type CryptoQuotation = {
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

export function readCryptoQuotationFromJson(data: any): CryptoQuotation | null {
    const camelCaseData = _.mapKeys(data, (_, key) => camelCase(key));
    if (validateCryptoQuotation(camelCaseData)) {
        return camelCaseData;
    } else {
        return null;
    }
}

function validateCryptoQuotation(data: any): data is CryptoQuotation {
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
        typeof data.windowStartTime === 'number' &&
        data.measureId !== null &&
        data.measurement !== null &&
        data.symbol !== null
    );
}
