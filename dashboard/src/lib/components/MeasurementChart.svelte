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

    let hoveredX: number | null = null;
    let tooltip: HTMLDivElement;

    // function does many things, snaps to nearest point, manipulates tooltip
    function describeDataOnX(event: MouseEvent) {
        if (!scaledPoints || scaledPoints.length === 0) return;

        const elementArea = (event.currentTarget as SVGElement)?.getBoundingClientRect();
        const mouseX = event.clientX - elementArea.left;

        const nearestPointInX = scaledPoints.reduce((nearestInX: ScaledPoint, point) => {
            if (!nearestInX || Math.abs(point.x - mouseX) < Math.abs(nearestInX.x - mouseX)) {
                return point;
            }
            return nearestInX;
        });

        if (nearestPointInX && Math.abs(nearestPointInX.x - mouseX) <= 5) {
            hoveredX = mouseX;
            tooltip.style.display = 'block';
            tooltip.style.left = `${event.clientX + 10}px`;
            tooltip.style.top = `${event.clientY + 10}px`;
            tooltip.textContent = `ask volume: $${nearestPointInX.measurement}price: $${nearestPointInX.price}, ${nearestPointInX.stringTime}`;
        } else {
            tooltip.style.display = 'none';
        }
    }

    $: scaledPoints = makeScaledPoints({
        data: data,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.sumAskVolume,
        getTime: (item) => item.windowEndTime,
        geometry: svgGeometry
    });

    $: polylinePointString = makePolyLinePoints(scaledPoints);

    $: {
        let rulerMinimum = Infinity;
        let rulerMaximum = -Infinity;

        scaledPoints.forEach((point) => {
            rulerMinimum = Math.min(rulerMinimum, point.measurement as number);
            rulerMaximum = Math.max(rulerMaximum, point.measurement as number);
        });

        verticalTicks = makeVerticalTicks({
            height: svgGeometry.height,
            topOffset: svgGeometry.offsets.top,
            bottomOffset: svgGeometry.offsets.bottom,
            minValue: rulerMinimum * 0.98,
            maxValue: rulerMaximum * 1.02,
            makeScale: autoMagnitude
        });
    }

    $: visibleDataPoints = scaledPoints.map((point) => ({
        ...point,
        visible: hoveredX !== null && Math.abs(point.x - hoveredX) < 1
    }));
</script>

<div>
    <div class="tooltip" bind:this={tooltip}>Tooltip content</div>

    <svg
        width={svgGeometry.width}
        height={svgGeometry.height}
        xmlns="http://www.w3.org/2000/svg"
        on:mousemove={describeDataOnX}
    >
        <line
            id="measurementScale"
            x1={svgGeometry.offsets.left}
            y1={svgGeometry.offsets.bottom}
            x2={svgGeometry.offsets.left}
            y2={svgGeometry.height - svgGeometry.offsets.top}
            stroke="lightgray"
        />
        <g id="measurementTicks" font-size="10" text-anchor="end">
            {#each verticalTicks as { y, label }}
                {#if y !== undefined && !isNaN(y)}
                    <line
                        x1={svgGeometry.offsets.right - 5}
                        y1={y}
                        x2={svgGeometry.offsets.right + 5}
                        y2={y}
                        stroke="lightgray"
                    />
                    <text x={svgGeometry.offsets.right - 10} y={y + 3}>{label}</text>
                {/if}
            {/each}
        </g>

        <polyline points={polylinePointString} fill="none" stroke="blue" stroke-width="2" />

        {#each visibleDataPoints as { x, y, visible }}
            {#if y !== undefined && !isNaN(y)}
                <circle cx={x} cy={y} r="5" class="data-point" style="opacity: {visible ? 1 : 0};" />
            {/if}
        {/each}
    </svg>
</div>

<style>
    .data-point {
        fill: blue;
        opacity: 0;
    }
    .data-point:hover {
        opacity: 1;
    }
    .tooltip {
        background-color: black;
        border-radius: 5px;
        color: white;
        display: none;
        padding: 5px;
        pointer-events: none;
        position: absolute;
        z-index: 1000;
    }
</style>
