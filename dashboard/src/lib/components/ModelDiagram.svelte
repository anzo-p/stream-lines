<script lang="ts">
    import { onMount } from 'svelte';

    import { listStore, addItem, addItems, filteredList } from '../../store/store';
    import { graphqlServiceProvider, websocketServiceProvider } from '../../store/servicesStore';
    import type { RenderGeometry, RenderOffsets } from '$lib/helpers/svg/polyline';
    import { makePolyLinePoints, makeScaledPoints } from '$lib/helpers/svg/polyline';
    import type { VerticalTick } from '$lib/helpers/svg/ticks';
    import { autoMagnitude, makeVerticalTicks } from '$lib/helpers/svg/ticks';
    import { getEpoch, getEpochNow } from '$lib/helpers/time';
    import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
    import type { ScaledPoint } from '$lib/types/ScaledPoint';
    import type { WindowedQuotation } from '$lib/types/WindowedQuotation';
    import HorizontalRuler from './HorizontalRuler.svelte';

    const graphqlService = $graphqlServiceProvider;
    const websocketService = $websocketServiceProvider;

    const svgGeometry: RenderGeometry = {
        width: 900,
        height: 440,
        offsets: { top: 50, bottom: 50, left: 50, right: 50 } as RenderOffsets
    };

    let measurementTicks: VerticalTick[];
    let priceTicks: VerticalTick[];

    let dropdownOptions = ['AAPL', 'AMD', 'AMZN', 'COIN', 'GOOG', 'INTC', 'MSFT', 'TSLA', 'BTC/USD', 'ETH/USD'];
    let selectedTicker = 'ETH/USD';

    let hoveredX: number | null = null;
    let tooltip: HTMLDivElement;

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
        // this of course should fetch based on user selection
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

    // function does many things, snaps to nearest point, manipulates tooltip
    function snapToPolyLinePoint(event: MouseEvent) {
        if (!scaledMeasurements || scaledMeasurements.length === 0) return;

        const elementArea = (event.currentTarget as SVGElement)?.getBoundingClientRect();
        const mouseX = event.clientX - elementArea.left;

        const nearestPointInX = scaledMeasurements.reduce((nearestInX: ScaledPoint, point) => {
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

    $: scaledMeasurements = makeScaledPoints({
        data: $filteredList,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.sumAskVolume,
        getTime: (item) => item.windowEndTime,
        geometry: svgGeometry
    });

    $: scaledPrice = makeScaledPoints({
        data: $filteredList,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.askPriceAtWindowEnd,
        getTime: (item) => item.windowEndTime,
        geometry: svgGeometry
    });

    $: measurementPointsString = makePolyLinePoints(scaledMeasurements);

    $: pricePointsString = makePolyLinePoints(scaledPrice);

    $: {
        let measuredMinimum = Infinity;
        let measuredMaximum = -Infinity;
        let priceMinimum = Infinity;
        let priceMaximum = -Infinity;

        scaledMeasurements.forEach((point) => {
            measuredMinimum = Math.min(measuredMinimum, point.measurement as number);
            measuredMaximum = Math.max(measuredMaximum, point.measurement as number);
        });

        scaledMeasurements.forEach((point) => {
            priceMinimum = Math.min(priceMinimum, point.price as number);
            priceMaximum = Math.max(priceMaximum, point.price as number);
        });

        measurementTicks = makeVerticalTicks({
            height: svgGeometry.height,
            topOffset: svgGeometry.offsets.top,
            bottomOffset: svgGeometry.offsets.bottom,
            minValue: measuredMinimum * 0.98,
            maxValue: measuredMaximum * 1.02,
            makeScale: autoMagnitude
        });

        priceTicks = makeVerticalTicks({
            height: svgGeometry.height,
            topOffset: svgGeometry.offsets.top,
            bottomOffset: svgGeometry.offsets.bottom,
            minValue: priceMinimum * 0.98,
            maxValue: priceMaximum * 1.02,
            makeScale: autoMagnitude
        });
    }

    $: visibleDataPoints = scaledMeasurements.map((point) => ({
        ...point,
        visible: hoveredX !== null && Math.abs(point.x - hoveredX) < 1
    }));
</script>

<div class="diagram">
    <div class="horizontal-ruler">
        <HorizontalRuler {svgGeometry} totalUnits={24} tickInterval={4} />
    </div>

    <div>
        <svg
            width={svgGeometry.width}
            height={svgGeometry.height}
            xmlns="http://www.w3.org/2000/svg"
            on:mousemove={snapToPolyLinePoint}
        >
            <line
                id="measurementScale"
                x1={svgGeometry.offsets.left}
                y1={svgGeometry.offsets.bottom}
                x2={svgGeometry.offsets.left}
                y2={svgGeometry.height - svgGeometry.offsets.top}
                stroke="black"
            />
            <line
                id="priceScale"
                x1={svgGeometry.width - svgGeometry.offsets.left}
                y1={svgGeometry.offsets.bottom}
                x2={svgGeometry.width - svgGeometry.offsets.left}
                y2={svgGeometry.height - svgGeometry.offsets.top}
                stroke="black"
            />

            <g id="measurementTicks" font-size="10" text-anchor="end">
                {#each measurementTicks as { y, label }}
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

            <g id="priceTicks" font-size="10" text-anchor="end">
                {#each priceTicks.slice(1) as { y, label }}
                    {#if y !== undefined && !isNaN(y)}
                        <line
                            x1={svgGeometry.width - svgGeometry.offsets.left - 5}
                            y1={y}
                            x2={svgGeometry.width - svgGeometry.offsets.left + 5}
                            y2={y}
                            stroke="lightgray"
                        />
                        <text x={svgGeometry.width - svgGeometry.offsets.left + 40} y={y + 3}>{label}</text>
                    {/if}
                {/each}
            </g>

            <polyline points={measurementPointsString} fill="none" stroke="blue" stroke-width="2" />

            <polyline points={pricePointsString} fill="none" stroke="green" stroke-width="2" />

            {#each visibleDataPoints as { x, y, visible }}
                {#if y !== undefined && !isNaN(y)}
                    <circle cx={x} cy={y} r="5" class="data-point" style="opacity: {visible ? 1 : 0};" />
                {/if}
            {/each}
        </svg>
        <div class="tooltip" bind:this={tooltip}>Tooltip content</div>
    </div>

    <label for="tickerSelect">ticker </label>
    <select id="tickerSelect" bind:value={selectedTicker} on:change={handleSelect}>
        {#each dropdownOptions as option (option)}
            <option value={option}>{option}</option>
        {/each}
    </select>
</div>

<style>
    .data-point {
        fill: blue;
        opacity: 0;
    }
    .data-point:hover {
        opacity: 1;
    }
    .diagram {
        display: flex;
        flex-direction: column;
        align-items: center;
    }
    .horizontal-ruler {
        position: absolute;
        flex-direction: column;
        align-items: center;
        pointer-events: none;
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
