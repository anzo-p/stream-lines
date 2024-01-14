<script lang="ts">
    import { writable, get } from 'svelte/store';
    import { onMount } from 'svelte';
    import { listStore, addItem } from '$lib/store';
    import { initializeWebSocket } from '$lib/websocket';
    import type { CryptoQuotation, ScaledPoint } from '$lib/types';

    const liveFeedUrl = import.meta.env.VITE_MARKET_DATA_FEED;

    let maxVolume = 1372138756.745597; // Initial max volume
    const recentValues = writable<number[]>([]);

    onMount(() => {
        const cleanup = initializeWebSocket(liveFeedUrl, (newData: CryptoQuotation) => {
            addItem(newData);
            updateMaxVolume(newData.sumAskVolume);
        });
        return cleanup;
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

    function getTodayMorning(): number {
        const now = new Date();
        const todayMorning = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        return Math.floor(todayMorning.getTime() / 1000);
    }

    function getTodayEvening(): number {
        const now = new Date();
        const todayEvening = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
        return Math.floor(todayEvening.getTime() / 1000);
    }

    function scaleData(data: CryptoQuotation[]): ScaledPoint[] {
        return data.map((point) => ({
            x: scaleTime(point.windowEndTime),
            y: scaleVolume(point.sumAskVolume)
        }));
    }

    const todayMorning = getTodayMorning();
    const todayEvening = getTodayEvening();

    function scaleTime(time: number): number {
        return ((time - todayMorning) / (todayEvening - todayMorning)) * 600;
    }

    function scaleVolume(volume: number): number {
        return 300 - (volume / maxVolume) * 300;
    }

    $: scaledData = scaleData($listStore);
    $: pointsString = scaledData.map((item) => `${item.x},${item.y}`).join(' ');

    $: {
        console.log('Updated pointsString:', pointsString);
    }
</script>

<svg width="600" height="300" viewBox="0 0 600 300">
    <polyline points={pointsString} fill="none" stroke="blue" stroke-width="1" />
</svg>
