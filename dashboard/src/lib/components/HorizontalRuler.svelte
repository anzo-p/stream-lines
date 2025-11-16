<script lang="ts">
  import type { RenderGeometry } from '$lib/helpers/svg/polyline';
  import { makeHorizontalTicks, type HorizontalTick } from '$lib/helpers/svg/ticks';

  export let svgGeometry: RenderGeometry;
  export let totalUnits: number;
  export let tickInterval: number;

  const horizontalTicks: HorizontalTick[] = makeHorizontalTicks({
    width: svgGeometry.width,
    startOffset: svgGeometry.offsets.left,
    totalUnits,
    tickInterval,
    labelTemplate: ':00'
  });
</script>

<svg class="ruler" width={svgGeometry.width} height={svgGeometry.height} xmlns="http://www.w3.org/2000/svg">
  <line
    id="time-scale"
    x1={svgGeometry.offsets.left}
    y1={svgGeometry.height - svgGeometry.offsets.top}
    x2={svgGeometry.width - svgGeometry.offsets.right}
    y2={svgGeometry.height - svgGeometry.offsets.top}
  />

  <g id="time-ticks">
    {#each horizontalTicks.slice(1, -1) as { x, label }}
      <line
        x1={x}
        y1={svgGeometry.height - svgGeometry.offsets.top - 5}
        x2={x}
        y2={svgGeometry.height - svgGeometry.offsets.top + 5}
      />
      <text {x} y={svgGeometry.height - svgGeometry.offsets.top + 15}>{label}</text>
    {/each}
  </g>
</svg>

<style>
  .ruler {
    font: 'Lucida Grande';
    font-size: 12px;
    text-anchor: middle;
    stroke: grey;
  }
</style>
