import type { ScaledPoint } from '$lib/types/ScaledPoint';
import { makePolyLinePoints, type RenderGeometry } from './polyline';
import { autoMagnitude, makeVerticalTicks } from './ticks';

export function makeRulerAndLineString(geometry: RenderGeometry, scaledPoints: ScaledPoint[]) {
  let rulerMinimum = Infinity;
  let rulerMaximum = -Infinity;

  scaledPoints.forEach((point) => {
    rulerMinimum = Math.min(rulerMinimum, point.measurement as number);
    rulerMaximum = Math.max(rulerMaximum, point.measurement as number);
  });

  const verticalTicks = makeVerticalTicks({
    height: geometry.height,
    topOffset: geometry.offsets.top,
    bottomOffset: geometry.offsets.bottom,
    minValue: rulerMinimum * 0.98,
    maxValue: rulerMaximum * 1.02,
    makeScale: autoMagnitude
  });

  const polylinePointString = makePolyLinePoints(scaledPoints);

  return { verticalTicks, polylinePointString };
}
