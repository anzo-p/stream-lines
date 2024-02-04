export type Measurement = {
  measure_id: string;
  measurement: string;
  symbol: string;
};

export function isMeasurement(obj: any): obj is Measurement {
  return obj && typeof obj.measure_id === 'string' && typeof obj.measurement === 'string' && typeof obj.symbol === 'string';
}
