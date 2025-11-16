<script lang="ts">
  import { makeScaledPoints, type RenderGeometry } from '$lib/helpers/svg/polyline';
  import { makeRulerAndLineString } from '$lib/helpers/svg/prepare';
  import type { VerticalTick } from '$lib/helpers/svg/ticks';
  import type { ScaledPoint } from '$lib/types/ScaledPoint';
  import type { WindowedQuotation } from '$lib/types/WindowedQuotation';

  export let data: WindowedQuotation[];
  export let svgGeometry: RenderGeometry;

  let verticalTicks: VerticalTick[];
  let polylinePointString: string;

  $: {
    const scaledPoints: ScaledPoint[] = makeScaledPoints({
      data,
      getMeasurement: (item) => item.askPriceAtWindowEnd,
      getPrice: (item) => item.askPriceAtWindowEnd,
      getTime: (item) => item.windowEndTime,
      geometry: svgGeometry
    });

    ({ verticalTicks, polylinePointString } = makeRulerAndLineString(svgGeometry, scaledPoints));
  }
</script>

<svg class="ruler" width={svgGeometry.width} height={svgGeometry.height} xmlns="http://www.w3.org/2000/svg">
  <line
    id="ruler-scale"
    x1={svgGeometry.width - svgGeometry.offsets.left}
    y1={svgGeometry.offsets.bottom}
    x2={svgGeometry.width - svgGeometry.offsets.left}
    y2={svgGeometry.height - svgGeometry.offsets.top}
  />

  <g id="ruler-ticks">
    {#each verticalTicks.slice(1) as { y, label }}
      {#if y !== undefined && !isNaN(y)}
        <line
          x1={svgGeometry.width - svgGeometry.offsets.left - 5}
          y1={y}
          x2={svgGeometry.width - svgGeometry.offsets.left + 5}
          y2={y}
        />
        <text x={svgGeometry.width - svgGeometry.offsets.left + 40} y={y + 3}>{label}</text>
      {/if}
    {/each}
  </g>

  <polyline points={polylinePointString} fill="none" stroke="#E0CA3C" stroke-width="2" />
</svg>

<style>
  .ruler {
    font: 'Lucida Grande';
    font-size: 12px;
    text-anchor: end;
    stroke: #b09d1c;
  }
</style>
