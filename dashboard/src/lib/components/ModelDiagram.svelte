<script lang="ts">
    import { onMount } from 'svelte';
    import { writable, get } from 'svelte/store';

    import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
    import { scaleTime, scaleTo } from '../helpers/scaling';
    import { getEpoch, getEpochNow } from '../helpers/time';
    import { listStore, addItem, addItems, filteredList } from '../../store/store';
    import { graphqlStore, websocketStore } from '../../store/resourcesStore';
    import type { ScaledPoint } from '../types/ScaledPoint';
    import type { WindowedQuotation } from '../types/WindowedQuotation';

    const graphql = $graphqlStore;
    const websocket = $websocketStore;

    let maxVolume = 1372138756.745597; // Initial max volume
    const recentValues = writable<number[]>([]);

    async function hydrate(symbol: string) {
        const initialData = (await fetchWindowedQuotations(
            graphql,
            symbol,
            getEpoch({ hours: 6 }),
            getEpochNow()
        )) as WindowedQuotation[];

        addItems(initialData);
    }

    onMount(async () => {
        try {
            const initialData = (await fetchWindowedQuotations(
                graphql,
                'TSLA',
                getEpoch({ hours: 6 }),
                getEpochNow()
            )) as WindowedQuotation[];

            addItems(initialData);

            websocket!.setOnNewDataCallback((newData: WindowedQuotation) => {
                addItem(newData);
            });
        } catch (error) {
            console.error('Error fetching initial data:', error);
        }
    });

    function updateMaxVolume(newVolume: number) {
        let recentVolumes = get(recentValues);
        recentVolumes.push(newVolume);

        if (recentVolumes.length > 10) {
            recentVolumes = recentVolumes.slice(-10);
        }

        recentValues.set(recentVolumes);

        const newMax = Math.max(...recentVolumes);
        if (newVolume > maxVolume) {
            maxVolume = newVolume;
        } else if (newMax < maxVolume * 0.1) {
            maxVolume = newMax;
        }
    }

    function scaleData(data: WindowedQuotation[]): ScaledPoint[] {
        let maxVolume: number = data.reduce((max, item) => Math.max(max, item.sumAskVolume), 0);
        updateMaxVolume(maxVolume);

        return data.map((point) => ({
            x: scaleTime(point.windowEndTime, 600),
            y: scaleTo(point.sumAskVolume, maxVolume, 300)
        }));
    }

    $: scaledData = scaleData($filteredList);

    $: pointsString = scaledData
        .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
        .sort((a, b) => a.x - b.x)
        .map((item) => `${item.x},${item.y}`)
        .join(' ');

    //$: console.log('pointString', pointsString);

    let dropdownOptions = [
        'AAPL',
        'AMD',
        'AMZN',
        'COIN',
        'GOOG',
        'INTC',
        'MSFT',
        'TSLA',
        'BTC/USD',
        'ETH/USD'
    ];
    let selectedOption = 'ETH/USD';

    function handleSelect(event: any) {
        selectedOption = event.target.value;
        hydrate(selectedOption);
        websocket!.subscribeTo([selectedOption]);
        listStore.selectSymbols([selectedOption]);
    }
</script>

<div class="diagram">
    <svg width="600" height="300" viewBox="0 0 600 300">
        <polyline points={pointsString} fill="none" stroke="blue" stroke-width="1" />
    </svg>

    <select bind:value={selectedOption} on:change={handleSelect}>
        {#each dropdownOptions as option (option)}
            <option value={option}>{option}</option>
        {/each}
    </select>

    <p>You selected: {selectedOption}</p>
</div>
