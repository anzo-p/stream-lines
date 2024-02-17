<script lang="ts">
    import { onMount } from 'svelte';

    import { listStore, addItem, addItems, filteredList } from '../../store/store';
    import { graphqlServiceProvider, websocketServiceProvider } from '../../store/servicesStore';
    import type { RenderGeometry, RenderOffsets } from '$lib/helpers/svg/polyline';
    import { makePolyLinePoints, makeScaledPoints } from '$lib/helpers/svg/polyline';
    import type { HorizontalTick, VerticalTick } from '$lib/helpers/svg/ticks';
    import { autoMagnitude, makeHorizontalTicks, makeVerticalTicks } from '$lib/helpers/svg/ticks';
    import { getEpoch, getEpochNow } from '$lib/helpers/time';
    import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
    import type { ScaledPoint } from '$lib/types/ScaledPoint';
    import type { WindowedQuotation } from '$lib/types/WindowedQuotation';

    const graphqlService = $graphqlServiceProvider;
    const websocketService = $websocketServiceProvider;

    const svgGeometry: RenderGeometry = {
        width: 900,
        height: 440,
        offsets: { top: 50, bottom: 50, left: 50, right: 50 } as RenderOffsets
    };

    const horizontalTicks: HorizontalTick[] = makeHorizontalTicks({
        width: svgGeometry.width,
        startOffset: svgGeometry.offsets.left,
        totalUnits: 24,
        tickInterval: 4,
        labelTemplate: ':00'
    });
    let measurementTicks: VerticalTick[];
    let priceTicks: VerticalTick[];

    let dropdownOptions = ['AAPL', 'AMD', 'AMZN', 'COIN', 'GOOG', 'INTC', 'MSFT', 'TSLA', 'BTC/USD', 'ETH/USD'];
    let selectedTicker = 'ETH/USD';

    let hoveredX: number | null = null;
    let tooltip: HTMLDivElement;

    async function loadMeasurements(ticker: string): Promise<WindowedQuotation[]> {
        // this of course should fetch based on user selection
        return await fetchWindowedQuotations(graphqlService, ticker, getEpoch({ hours: 1 }), getEpochNow());
    }

    onMount(async () => {
        try {
            const initialData: WindowedQuotation[] = await loadMeasurements(selectedTicker);
            addItems(initialData);

            websocketService!.setOnNewDataCallback((newData: WindowedQuotation) => {
                addItem(newData);
            });
        } catch (error) {
            console.error('Error fetching initial data:', error);
        }
    });

    async function hydrate(ticker: string) {
        const initialData: WindowedQuotation[] = await loadMeasurements(ticker);
        addItems(initialData);
    }

    function handleSelect(event: any) {
        selectedTicker = event.target.value;
        hydrate(selectedTicker);
        listStore.selectTickers([selectedTicker]);
        websocketService!.subscribeTo([selectedTicker]);
    }

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
    <div>
        <svg
            width={svgGeometry.width}
            height={svgGeometry.height}
            xmlns="http://www.w3.org/2000/svg"
            on:mousemove={snapToPolyLinePoint}
        >
            <line
                id="timeScale"
                x1={svgGeometry.offsets.left}
                y1={svgGeometry.height - svgGeometry.offsets.top}
                x2={svgGeometry.width - svgGeometry.offsets.right}
                y2={svgGeometry.height - svgGeometry.offsets.top}
                stroke="black"
            />
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

            <g id="timeTicks" font-size="10" text-anchor="middle">
                {#each horizontalTicks.slice(1, -1) as { x, label }}
                    <line
                        x1={x}
                        y1={svgGeometry.height - svgGeometry.offsets.top - 5}
                        x2={x}
                        y2={svgGeometry.height - svgGeometry.offsets.top + 5}
                        stroke="black"
                    />
                    <text {x} y={svgGeometry.height - svgGeometry.offsets.top + 15}>{label}</text>
                {/each}
            </g>

            <g id="measurementTicks" font-size="10" text-anchor="end">
                {#each measurementTicks as { y, label }}
                    {#if y !== undefined && !isNaN(y)}
                        <line
                            x1={svgGeometry.offsets.right - 5}
                            y1={y}
                            x2={svgGeometry.offsets.right + 5}
                            y2={y}
                            stroke="black"
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
                            stroke="black"
                        />
                        <text x={svgGeometry.width - svgGeometry.offsets.left + 40} y={y + 3}>{label}</text>
                    {/if}
                {/each}
            </g>

            <polyline points={measurementPointsString} fill="none" stroke="blue" stroke-width="1" />

            <polyline points={pricePointsString} fill="none" stroke="green" stroke-width="1" />

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
