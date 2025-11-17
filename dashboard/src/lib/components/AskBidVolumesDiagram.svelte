<script lang="ts">
  import { onMount } from 'svelte';
  import _ from 'lodash';
  import { listStore, addItem, addItems, filteredList } from '../../store/store';
  import { graphqlServiceProvider, websocketServiceProvider } from '../../store/servicesStore';
  import type { RenderGeometry, RenderOffsets } from '$lib/helpers/svg/polyline';
  import { getEpoch, getEpochNow } from '$lib/helpers/time';
  import { starterTickers } from '$lib/mockData';
  import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
  import type { WindowedQuotation } from '$lib/types/WindowedQuotation';
  import HorizontalRuler from './HorizontalRuler.svelte';
  import MeasurementChart from './MeasurementChart.svelte';
  import QuotationChart from './QuotationChart.svelte';
  import AskBidVolumesChart from '$lib/components/AskBidVolumesChart.svelte';

  const graphqlService = $graphqlServiceProvider;
  const websocketService = $websocketServiceProvider;

  const svgGeometry: RenderGeometry = {
    width: 900,
    height: 440,
    offsets: { top: 50, bottom: 50, left: 50, right: 50 } as RenderOffsets
  };

  let dropdownOptions = starterTickers;
  let selectedTicker = _.shuffle(starterTickers)[0];

  onMount(async () => {
    try {
      const data: WindowedQuotation[] = await loadMeasurements(selectedTicker);
      addItems(data);

      websocketService!.setOnNewDataCallback((newData: WindowedQuotation) => {
        addItem(newData);
      });
    } catch (error) {
      console.error('Error fetching data:', error);
    }
  });

  async function loadMeasurements(ticker: string): Promise<WindowedQuotation[]> {
    return await fetchWindowedQuotations(graphqlService, ticker, getEpoch({ hours: 1 }), getEpochNow());
  }

  async function hydrate(ticker: string) {
    const data: WindowedQuotation[] = await loadMeasurements(ticker);
    addItems(data);
  }

  function handleSelect(event: any) {
    selectedTicker = event.target.value;
    hydrate(selectedTicker);
    listStore.selectTickers([selectedTicker]);
    websocketService!.subscribeTo([selectedTicker]);
  }
</script>

<div class="diagram">
  <div class="non-interactive">
    <HorizontalRuler {svgGeometry} totalUnits={24} tickInterval={4} />
  </div>
  <div class="measurement-chart">
    <AskBidVolumesChart data={$filteredList} {svgGeometry} />
  </div>

  <div>
    <svg width={svgGeometry.width} height={svgGeometry.height} xmlns="http://www.w3.org/2000/svg" />
  </div>

  <label for="tickerSelect">ticker </label>
  <select id="tickerSelect" bind:value={selectedTicker} on:change={handleSelect}>
    {#each dropdownOptions as option (option)}
      <option value={option}>{option}</option>
    {/each}
  </select>
</div>

<style>
  .diagram {
    align-items: center;
    background-color: #01161e;
    display: flex;
    flex-direction: column;
  }
  .non-interactive {
    align-items: center;
    flex-direction: column;
    pointer-events: none;
    position: absolute;
  }
  .measurement-chart {
    align-items: center;
    flex-direction: column;
    position: absolute;
  }
</style>
