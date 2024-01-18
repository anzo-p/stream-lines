import { minMax } from './numbers';
import { getEpoch, normalizeEpoch } from './time';

export function scaleTo(value: number, ceil: number, scale: number): number {
    return scale - (value / ceil) * scale;
}

export function scaleTime(time: number, scale: number): number {
    const normalizedTime = normalizeEpoch(time);
    return minMax(
        normalizedTime,
        getEpoch({ hours: 0, minutes: 0, seconds: 0, asMilliseconds: true }),
        getEpoch({ hours: 23, minutes: 59, seconds: 59, asMilliseconds: true }),
        scale
    );
}
