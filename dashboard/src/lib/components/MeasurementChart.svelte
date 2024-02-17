<script lang="ts">
    import { makeScaledPoints, type RenderGeometry } from '$lib/helpers/svg/polyline';
    import { makeRulerAndLineString } from '$lib/helpers/svg/prepare';
    import type { VerticalTick } from '$lib/helpers/svg/ticks';
    import type { ScaledPoint } from '$lib/types/ScaledPoint';
    import type { WindowedQuotation } from '$lib/types/WindowedQuotation';

    export let data: WindowedQuotation[];
    export let svgGeometry: RenderGeometry;

    type VisibleScaledPoint = ScaledPoint & { visible: boolean };

    let scaledPoints: ScaledPoint[];
    let visibleScaledPoints: VisibleScaledPoint[];
    let verticalTicks: VerticalTick[];
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

    $: {
        scaledPoints = makeScaledPoints({
            data,
            getMeasurement: (item) => item.sumAskVolume,
            getPrice: (item) => item.askPriceAtWindowEnd,
            getTime: (item) => item.windowEndTime,
            geometry: svgGeometry
        });

        ({ verticalTicks, polylinePointString } = makeRulerAndLineString(svgGeometry, scaledPoints));
    }

    $: visibleScaledPoints = scaledPoints.map((point: ScaledPoint) => ({
        ...point,
        visible: hoveredX !== null && Math.abs(point.x - hoveredX) < 1
    }));
</script>

<div>
    <div class="tooltip" bind:this={tooltip}>Tooltip content</div>

    <svg
        class="ruler"
        width={svgGeometry.width}
        height={svgGeometry.height}
        xmlns="http://www.w3.org/2000/svg"
        on:mousemove={describeDataOnX}
    >
        <line
            id="ruler-scale"
            x1={svgGeometry.offsets.left}
            y1={svgGeometry.offsets.bottom}
            x2={svgGeometry.offsets.left}
            y2={svgGeometry.height - svgGeometry.offsets.top}
        />
        <g id="ruler-ticks">
            {#each verticalTicks as { y, label }}
                {#if y !== undefined && !isNaN(y)}
                    <line x1={svgGeometry.offsets.right - 5} y1={y} x2={svgGeometry.offsets.right + 5} y2={y} />
                    <text x={svgGeometry.offsets.right - 10} y={y + 3}>{label}</text>
                {/if}
            {/each}
        </g>

        <polyline points={polylinePointString} fill="none" stroke="#0faebd" stroke-width="2" />

        {#each visibleScaledPoints as { x, y, visible }}
            {#if y !== undefined && !isNaN(y)}
                <circle cx={x} cy={y} r="5" class="data-point" style="opacity: {visible ? 1 : 0};" />
            {/if}
        {/each}
    </svg>
</div>

<style>
    .data-point {
        fill: #55e4f1;
        opacity: 0;
    }
    .data-point:hover {
        opacity: 1;
    }
    .ruler {
        font: 'Lucida Grande';
        font-size: 12px;
        text-anchor: end;
        stroke: #0c8b97;
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
