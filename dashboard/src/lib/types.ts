export interface CryptoQuotation {
    measureId: string;
    measurementType: string;
    symbol: string;
    windowStartTime: number;
    windowEndTime: number;
    sumBidVolume: number;
    sumAskVolume: number;
}

export function isCryptoQuotation(obj: any): obj is CryptoQuotation {
    return (
        typeof obj.measureId === 'string' &&
        typeof obj.measurementType === 'string' &&
        typeof obj.symbol === 'string' &&
        typeof obj.windowStartTime === 'number' &&
        typeof obj.windowEndTime === 'number' &&
        typeof obj.sumBidVolume === 'number' &&
        typeof obj.sumAskVolume === 'number'
    );
}

export interface ScaledPoint {
    x: number;
    y: number;
}
