import { getEpoch, normalizeEpoch } from './time';

export function minMax(min: number, max: number, multi: number, x: number): number {
    return ((x - min) / (max - min)) * multi;
}

export function scaleTo(x: number, sourceRange: [number, number], targetRange: [number, number]): number {
    const [a, b] = sourceRange;
    const [c, d] = targetRange;

    return c + ((x - b) * (d - c)) / (a - b);
}

export function scaleTime(time: number, scale: number): number {
    const normalizedTime = normalizeEpoch(time);
    return minMax(
        getEpoch({ hours: 0, minutes: 0, seconds: 0, asMilliseconds: true }),
        getEpoch({ hours: 23, minutes: 59, seconds: 59, asMilliseconds: true }),
        scale,
        normalizedTime
    );
}
