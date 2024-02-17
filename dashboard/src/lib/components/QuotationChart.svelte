<script lang="ts">
    import { makePolyLinePoints, makeScaledPoints, type RenderGeometry } from '$lib/helpers/svg/polyline';
    import { autoMagnitude, makeVerticalTicks, type VerticalTick } from '$lib/helpers/svg/ticks';
    import type { ScaledPoint } from '$lib/types/ScaledPoint';
    import type { WindowedQuotation } from '$lib/types/WindowedQuotation';

    export let data: WindowedQuotation[];
    export let svgGeometry: RenderGeometry;

    let verticalTicks: VerticalTick[];
    let scaledPoints: ScaledPoint[];
    let polylinePointString: string;

    $: scaledPoints = makeScaledPoints({
        data,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.askPriceAtWindowEnd,
        getTime: (item) => item.windowEndTime,
        geometry: svgGeometry
    });

    $: polylinePointString = makePolyLinePoints(scaledPoints);

    $: {
        let priceMinimum = Infinity;
        let priceMaximum = -Infinity;

        scaledPoints.forEach((point) => {
            priceMinimum = Math.min(priceMinimum, point.measurement as number);
            priceMaximum = Math.max(priceMaximum, point.measurement as number);
        });

        verticalTicks = makeVerticalTicks({
            height: svgGeometry.height,
            topOffset: svgGeometry.offsets.top,
            bottomOffset: svgGeometry.offsets.bottom,
            minValue: priceMinimum * 0.98,
            maxValue: priceMaximum * 1.02,
            makeScale: autoMagnitude
        });
    }
</script>

<svg width={svgGeometry.width} height={svgGeometry.height} xmlns="http://www.w3.org/2000/svg">
    <line
        id="priceScale"
        x1={svgGeometry.width - svgGeometry.offsets.left}
        y1={svgGeometry.offsets.bottom}
        x2={svgGeometry.width - svgGeometry.offsets.left}
        y2={svgGeometry.height - svgGeometry.offsets.top}
        stroke="lightgray"
    />

    <g id="priceTicks" font-size="10" text-anchor="end">
        {#each verticalTicks.slice(1) as { y, label }}
            {#if y !== undefined && !isNaN(y)}
                <line
                    x1={svgGeometry.width - svgGeometry.offsets.left - 5}
                    y1={y}
                    x2={svgGeometry.width - svgGeometry.offsets.left + 5}
                    y2={y}
                    stroke="lightgray"
                />
                <text x={svgGeometry.width - svgGeometry.offsets.left + 40} y={y + 3}>{label}</text>
            {/if}
        {/each}
    </g>

    <polyline points={polylinePointString} fill="none" stroke="green" stroke-width="2" />
</svg>
