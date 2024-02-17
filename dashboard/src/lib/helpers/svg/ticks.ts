export interface ScaleUnit {
    unit: number;
    postfix: string;
}

export type ScaleUnitRule = (range: number) => ScaleUnit;

export type HorizontalTick = { x: number; label: string };
export type VerticalTick = { y: number; label: string };

export const autoMagnitude: ScaleUnitRule = (range: number): ScaleUnit => {
    if (range > 1e9) return { unit: 1e9, postfix: ' b' };
    if (range > 1e6) return { unit: 1e6, postfix: ' m' };
    if (range > 1e3) return { unit: 1e3, postfix: ' k' };
    return { unit: 1, postfix: '' };
};

export function makeHorizontalTicks({
    width,
    startOffset,
    totalUnits = 24,
    tickInterval = 4,
    labelTemplate
}: {
    width: number;
    startOffset: number;
    totalUnits?: number;
    tickInterval?: number;
    labelTemplate: string;
}) {
    const ticks: HorizontalTick[] = [];
    const tickCount = totalUnits / tickInterval;
    const spacing = (width - 2 * startOffset) / tickCount;

    for (let i = 0; i <= tickCount; i++) {
        const x = startOffset + i * spacing;
        const hour = i * tickInterval;
        const label = `${hour}${labelTemplate}`;
        ticks.push({ x, label });
    }

    return ticks;
}

export function makeVerticalTicks({
    height,
    topOffset,
    bottomOffset,
    minValue,
    maxValue,
    makeScale
}: {
    height: number;
    topOffset: number;
    bottomOffset: number;
    minValue: number;
    maxValue: number;
    makeScale: ScaleUnitRule;
}): VerticalTick[] {
    const ticks: VerticalTick[] = [];
    const range = maxValue - minValue;

    const { unit, postfix: tickLabel } = makeScale(range);

    const startValue = Math.ceil(minValue / unit) * unit;
    const tickCount = Math.min(5, range / unit);
    const newUnit = (maxValue - startValue) / tickCount;
    const usableHeight = height - topOffset - bottomOffset;
    const scale = usableHeight / (maxValue - minValue);

    for (let i = 0; i <= tickCount; i++) {
        const value = startValue + i * newUnit;
        const y = height - bottomOffset - (value - minValue) * scale;
        const label = `${(value / unit).toFixed(2)}${tickLabel}`;
        ticks.push({ y, label });
    }

    return ticks;
}
