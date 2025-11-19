<script lang="ts">
  import { makePolyLinePoints, makeScaledPoints, type RenderGeometry } from '$lib/helpers/svg/polyline';
  import { makeRulerAndLineString } from '$lib/helpers/svg/prepare';
  import type { VerticalTick } from '$lib/helpers/svg/ticks';
  import type { ScaledPoint } from '$lib/types/ScaledPoint';
  import type { WindowedQuotation } from '$lib/types/WindowedQuotation';

  export let data: WindowedQuotation[];
  export let svgGeometry: RenderGeometry;

  type VisibleScaledPoint = ScaledPoint & { visible: boolean };

  let askScaledPoints: ScaledPoint[];
  let bidScaledPoints: ScaledPoint[];
  let visibleAskScaledPoints: VisibleScaledPoint[];
  let visibleBidScaledPoints: VisibleScaledPoint[];
  let verticalTicks: VerticalTick[];
  let askPolylinePointString: string;
  let bidPolylinePointString: string;
  let hoveredAsk: number | null = null;
  let hoveredBid: number | null = null;

  $: {
    askScaledPoints = makeScaledPoints({
      data,
      getMeasurement: (item) => item.sumAskVolume,
      getPrice: (item) => item.sumAskVolume,
      getTime: (item) => item.windowEndTime,
      geometry: svgGeometry
    });

    askPolylinePointString = makePolyLinePoints(askScaledPoints);
  }

  $: {
    bidScaledPoints = makeScaledPoints({
      data,
      getMeasurement: (item) => item.sumBidVolume,
      getPrice: (item) => item.sumBidVolume,
      getTime: (item) => item.windowEndTime,
      geometry: svgGeometry
    });

    bidPolylinePointString = makePolyLinePoints(bidScaledPoints);
  }

  $: {
    verticalTicks = makeRulerAndLineString(
        svgGeometry,
        [...askScaledPoints, ...bidScaledPoints].sort((a, b) => a.measurement!! - b.measurement!!)
    ).verticalTicks;
  }

  $: visibleBidScaledPoints = bidScaledPoints.map((point: ScaledPoint) => ({
    ...point,
    visible: hoveredAsk !== null && Math.abs(point.x - hoveredAsk) < 1
  }));

  $: visibleAskScaledPoints = askScaledPoints.map((point: ScaledPoint) => ({
    ...point,
    visible: hoveredBid !== null && Math.abs(point.x - hoveredBid) < 1
  }));
</script>

<div>
  <svg
    role="img"
    class="ruler"
    width={svgGeometry.width}
    height={svgGeometry.height}
    xmlns="http://www.w3.org/2000/svg"
  >
    <line
      id="left-ruler-scale"
      x1={svgGeometry.offsets.left}
      y1={svgGeometry.offsets.bottom}
      x2={svgGeometry.offsets.left}
      y2={svgGeometry.height - svgGeometry.offsets.top}
    />
    <g id="left-ruler-ticks">
      {#each verticalTicks as { y, label }}
        {#if y !== undefined && !isNaN(y)}
          <line x1={svgGeometry.offsets.right - 5} y1={y} x2={svgGeometry.offsets.right + 5} y2={y} />
          <text x={svgGeometry.offsets.right - 10} y={y + 3}>{label}</text>
        {/if}
      {/each}
    </g>

    <line
      id="right-ruler-scale"
      x1={svgGeometry.width - svgGeometry.offsets.left}
      y1={svgGeometry.offsets.bottom}
      x2={svgGeometry.width - svgGeometry.offsets.left}
      y2={svgGeometry.height - svgGeometry.offsets.top}
    />
    <g id="right-ruler-ticks">
      {#each verticalTicks.slice(1) as { y, label }}
        {#if y !== undefined && !isNaN(y)}
          <line
            x1={svgGeometry.width - svgGeometry.offsets.left - 5}
            y1={y}
            x2={svgGeometry.width - svgGeometry.offsets.left + 5}
            y2={y}
          />
          <text
              x={svgGeometry.width - svgGeometry.offsets.left + 40}
              y={y + 3}>{label}
          </text>
        {/if}
      {/each}
    </g>

    <polyline points={askPolylinePointString} fill="none" stroke="#3399ee" stroke-width="2" />

    <polyline points={bidPolylinePointString} fill="none" stroke="#33ee99" stroke-width="2" />

    {#each visibleAskScaledPoints as { x, y, visible }}
      {#if y !== undefined && !isNaN(y)}
        <circle cx={x} cy={y} r="5" class="ask-data-point" style="opacity: {visible ? 1 : 0};" />
      {/if}
    {/each}

    {#each visibleBidScaledPoints as { x, y, visible }}
      {#if y !== undefined && !isNaN(y)}
        <circle cx={x} cy={y} r="5" class="bid-data-point" style="opacity: {visible ? 1 : 0};" />
      {/if}
    {/each}
  </svg>
</div>

<style>
  .ask-data-point {
    fill: #3399ee;
    opacity: 0;
  }
  .ask-data-point:hover {
    opacity: 1;
  }
  .bid-data-point {
    fill: #33ee99;
    opacity: 0;
  }
  .bid-data-point:hover {
    opacity: 1;
  }
  .ruler {
    font: 'Lucida Grande';
    font-size: 12px;
    text-anchor: end;
    stroke: #bbbb33;
  }
</style>
