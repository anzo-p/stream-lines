import type { ScaledPoint } from '$lib/types/ScaledPoint';
import { scaleTime, scaleTo } from '$lib/helpers/scaling';
import { getDateFromEpoch, normalizeEpoch } from '$lib/helpers/time';

export type Extractor<T> = (item: T) => number;

export type RenderOffsets = {
  top: number;
  bottom: number;
  left: number;
  right: number;
};

export type RenderGeometry = {
  width: number;
  height: number;
  offsets: RenderOffsets;
};

export function makeScaledPoints<T>({
  data,
  getMeasurement,
  getPrice,
  getTime,
  geometry
}: {
  data: T[];
  getMeasurement: Extractor<T>;
  getPrice: Extractor<T>;
  getTime: Extractor<T>;
  geometry: RenderGeometry;
}): ScaledPoint[] {
  const { width, height, offsets } = geometry;

  data = data.filter((item) => getPrice(item) > 0 && getMeasurement(item) > 0);

  const minValue: number = data.reduce((min, item) => Math.min(min, getMeasurement(item)), Infinity);
  const maxValue: number = data.reduce((max, item) => Math.max(max, getMeasurement(item)), -Infinity);
  const sourceRange: [number, number] = [minValue * 0.98, maxValue * 1.02];
  const destinationRange: [number, number] = [0, height - (offsets.top + offsets.bottom)];

  return data.map((point: T) => {
    const price = getPrice(point);
    const measurement = getMeasurement(point);
    const endTime = normalizeEpoch(getTime(point));
    const x = scaleTime(endTime, width - (offsets.left + offsets.right));
    const y = scaleTo(measurement, sourceRange, destinationRange);

    return {
      x: x + offsets.left,
      y: y + offsets.top,
      stringTime: getDateFromEpoch(endTime),
      price,
      measurement
    };
  });
}

export function makePolyLinePoints(points: ScaledPoint[]): string {
  return points
    .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
    .sort((a, b) => a.x - b.x)
    .map((item) => `${item.x},${item.y}`)
    .join(' ');
}
