<script lang="ts">
    import { onMount } from 'svelte';
    import { writable, get } from 'svelte/store';

    import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
    import type { WindowedQuotation } from '../types/CryptoQuotation';
    import type { ScaledPoint } from '../types/ScaledPoint';
    import { scaleTime, scaleTo } from '../helpers/scaling';
    import { getEpoch, getEpochNow } from '../helpers/time';

    import { listStore, addItem, addItems } from '../../store/store';
    import { graphqlStore, websocketStore } from '../../store/resourcesStore';

    const graphql = $graphqlStore;
    const websocket = $websocketStore;

    let maxVolume = 1372138756.745597; // Initial max volume
    const recentValues = writable<number[]>([]);

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

    $: scaledData = scaleData($listStore);

    $: pointsString = scaledData
        .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
        .sort((a, b) => a.x - b.x)
        .map((item) => `${item.x},${item.y}`)
        .join(' ');

    //$: console.log('pointString', pointsString);

    /*
        we need a symbol selector
        but this selection must apply to ingest as well
        when selecting symbols once must be guided to realize the cap of 30 simultaneously active limit from alpaca
        adjusting over this will lead to gaps on some other symbols
        this action should be warned and confirmed
        also the presently active ones ans their total count must be visible
        otherwise free to select within those currently active
        when changing symbol of focus, websocket must be un/subbed to feed only on those
    */
</script>

<svg width="600" height="300" viewBox="0 0 600 300">
    <polyline points={pointsString} fill="none" stroke="blue" stroke-width="1" />
</svg>
