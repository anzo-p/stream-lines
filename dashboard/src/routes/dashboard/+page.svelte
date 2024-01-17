<script lang="ts">
    import { writable, get } from 'svelte/store';
    import { onMount } from 'svelte';
    import { listStore, addItem, addItems } from '../../store/store';
    import { initializeWebSocket } from '../../services/websocket';
    import type { CryptoQuotation } from '../../types/CryptoQuotation';
    import type { ScaledPoint } from '../../types/ScaledPoint';
    import { fetchCryptoQuotations } from '../../services/graphql';

    const liveFeedUrl = import.meta.env.VITE_MARKET_DATA_FEED;

    let maxVolume = 1372138756.745597; // Initial max volume
    const recentValues = writable<number[]>([]);

    const currentDate = new Date();
    const currentYear = currentDate.getFullYear();
    const currentMonth = currentDate.getMonth();
    const currentDay = currentDate.getDate();

    const sixAM = new Date(currentYear, currentMonth, currentDay, 6, 0, 0, 0);
    const sixAMUnixEpoch = sixAM.getTime() * 1000 * 1000;
    const currentUnixEpoch = currentDate.getTime() * 1000 * 1000;

    const todayMorning = getTodayMorning();
    const todayEvening = getTodayEvening();

    onMount(async () => {
        try {
            const initialData = (await fetchCryptoQuotations(
                'TSLA',
                sixAMUnixEpoch,
                currentUnixEpoch
            )) as CryptoQuotation[];

            let maxVolume: number = initialData.reduce(
                (max, item) => Math.max(max, item.sumAskVolume),
                0
            );
            updateMaxVolume(maxVolume);
            addItems(initialData);

            const cleanup = initializeWebSocket(liveFeedUrl, (newData) => {
                updateMaxVolume(newData.sumAskVolume);

                let data = newData;
                data.windowStartTime = data.windowStartTime;
                data.windowEndTime = data.windowEndTime;
                addItem(newData);
            });

            return cleanup;
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

    function getTodayMorning(): number {
        const now = new Date();
        const todayMorning = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        return Math.floor(todayMorning.getTime());
    }

    function getTodayEvening(): number {
        const now = new Date();
        const todayEvening = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
        return Math.floor(todayEvening.getTime());
    }

    function scaleData(data: CryptoQuotation[]): ScaledPoint[] {
        return data.map((point) => ({
            x: scaleTime(point.windowEndTime),
            y: scaleVolume(point.sumAskVolume)
        }));
    }

    function scaleTime(time: number): number {
        let magnitude = 1;
        if (time > 1000000000000000000) {
            magnitude = 1000000;
        } else if (time > 1000000000000000) {
            magnitude = 1000;
        } else if (time > 1000000000000) {
            magnitude = 1;
        } else if (time > 1000000000) {
            magnitude = 0.001;
        } else if (time > 1000000) {
            magnitude = 0.0000001;
        }

        return ((time / magnitude - todayMorning) / (todayEvening - todayMorning)) * 600;
    }

    function scaleVolume(volume: number): number {
        return 300 - (volume / maxVolume) * 300;
    }

    $: scaledData = scaleData($listStore);

    $: pointsString = scaledData
        .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
        .sort((a, b) => a.x - b.x)
        .map((item) => `${item.x},${item.y}`)
        .join(' ');

    $: console.log('pointString', pointsString);
</script>

<svg width="600" height="300" viewBox="0 0 600 300">
    <polyline points={pointsString} fill="none" stroke="blue" stroke-width="1" />
</svg>
