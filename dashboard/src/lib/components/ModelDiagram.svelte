<script lang="ts">
    import { onMount } from 'svelte';

    import { fetchWindowedQuotations } from '$lib/services/graphql/windowedQuotation';
    import { scaleTime, scaleTo } from '../helpers/scaling';
    import { getDateFromEpoch, getEpoch, getEpochNow, normalizeEpoch } from '../helpers/time';
    import { listStore, addItem, addItems, filteredList } from '../../store/store';
    import { graphqlServiceProvider, websocketServiceProvider } from '../../store/servicesStore';
    import type { ScaledPoint } from '../types/ScaledPoint';
    import type { WindowedQuotation } from '../types/WindowedQuotation';

    const graphqlService = $graphqlServiceProvider;
    const websocketService = $websocketServiceProvider;

    let svgWidth = 900;
    let svgHeight = 440;
    let topOffset = 50;
    let bottomOffset = 50;
    let leftOffset = 50;
    let rightOffset = 50;

    type HorizontalTick = { x: number; label: string };
    type VerticalTick = { y: number; label: string };

    let horizontalTickPositions: HorizontalTick[] = makeHorizontalTicks({
        width: svgWidth,
        startOffset: leftOffset,
        labelTemplate: ':00'
    });
    let measurementTicks: VerticalTick[];
    let priceTicks: VerticalTick[];

    let dropdownOptions = ['AAPL', 'AMD', 'AMZN', 'COIN', 'GOOG', 'INTC', 'MSFT', 'TSLA', 'BTC/USD', 'ETH/USD'];
    let selectedSymbol = 'ETH/USD';

    let hoveredX: number | null = null;
    let tooltip: HTMLDivElement;

    function makeHorizontalTicks({
        width,
        startOffset,
        totalUnits = 24,
        tickInterval = 4,
        labelTemplate
    }: {
        width: number;
        startOffset: number;
        totalUnits?: number;
        tickInterval?: number;
        labelTemplate: string;
    }) {
        const ticks: HorizontalTick[] = [];
        const tickCount = totalUnits / tickInterval;
        const spacing = (width - 2 * startOffset) / tickCount;

        for (let i = 0; i <= tickCount; i++) {
            const x = startOffset + i * spacing;
            const hour = i * tickInterval;
            const label = `${hour}${labelTemplate}`;
            ticks.push({ x, label });
        }

        return ticks;
    }

    interface ScaleUnit {
        unit: number;
        postfix: string;
    }

    type ScaleUnitRule = (range: number) => ScaleUnit;

    const autoMagnitude: ScaleUnitRule = (range: number): ScaleUnit => {
        if (range > 1e9) return { unit: 1e9, postfix: ' b' };
        if (range > 1e6) return { unit: 1e6, postfix: ' m' };
        if (range > 1e3) return { unit: 1e3, postfix: ' k' };
        return { unit: 1, postfix: '' };
    };

    function makeVerticalTicks({
        height,
        topOffset,
        bottomOffset,
        minValue,
        maxValue,
        makeScale
    }: {
        height: number;
        topOffset: number;
        bottomOffset: number;
        minValue: number;
        maxValue: number;
        makeScale: ScaleUnitRule;
    }): VerticalTick[] {
        const ticks: VerticalTick[] = [];
        const range = maxValue - minValue;

        const { unit, postfix: tickLabel } = makeScale(range);

        const startValue = Math.ceil(minValue / unit) * unit;
        const tickCount = Math.min(5, range / unit);
        const newUnit = (maxValue - startValue) / tickCount;
        const usableHeight = height - topOffset - bottomOffset;
        const scale = usableHeight / (maxValue - minValue);

        for (let i = 0; i <= tickCount; i++) {
            const value = startValue + i * newUnit;
            const y = height - bottomOffset - (value - minValue) * scale;
            const label = `${(value / unit).toFixed(0)}${tickLabel}`;
            ticks.push({ y, label });
        }

        return ticks;
    }

    async function loadMeasurements(symbol: string): Promise<WindowedQuotation[]> {
        return await fetchWindowedQuotations(graphqlService, symbol, getEpoch({ hours: 1 }), getEpochNow());
    }

    onMount(async () => {
        try {
            const initialData: WindowedQuotation[] = await loadMeasurements(selectedSymbol);
            addItems(initialData);

            websocketService!.setOnNewDataCallback((newData: WindowedQuotation) => {
                addItem(newData);
            });
        } catch (error) {
            console.error('Error fetching initial data:', error);
        }
    });

    async function hydrate(symbol: string) {
        const initialData: WindowedQuotation[] = await loadMeasurements(symbol);
        addItems(initialData);
    }

    function handleSelect(event: any) {
        selectedSymbol = event.target.value;
        hydrate(selectedSymbol);
        listStore.selectSymbols([selectedSymbol]);
        websocketService!.subscribeTo([selectedSymbol]);
    }

    type Extractor<T> = (item: T) => number;

    function generateMeasurementPoints<T>({
        data,
        getPrice,
        getMeasurement,
        getTime
    }: {
        data: T[];
        getPrice: Extractor<T>;
        getMeasurement: Extractor<T>;
        getTime: Extractor<T>;
    }): ScaledPoint[] {
        data = data.filter((item) => getPrice(item) > 0 && getMeasurement(item) > 0);

        const minValue: number = data.reduce((min, item) => Math.min(min, getMeasurement(item)), Infinity);
        const maxValue: number = data.reduce((max, item) => Math.max(max, getMeasurement(item)), -Infinity);
        const sourceRange: [number, number] = [minValue * 0.98, maxValue * 1.02];
        const destinationRange: [number, number] = [0, svgHeight - (topOffset + bottomOffset)];

        return data.map((point: T) => {
            const price = getPrice(point);
            const measurement = getMeasurement(point);
            const endTime = normalizeEpoch(getTime(point));
            const x = scaleTime(endTime, svgWidth - (leftOffset + rightOffset));
            const y = scaleTo(measurement, sourceRange, destinationRange);

            return {
                x: x + leftOffset,
                y: y + topOffset,
                stringTime: getDateFromEpoch(endTime),
                price,
                measurement
            };
        });
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

    $: scaledMeasurements = generateMeasurementPoints({
        data: $filteredList,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.sumAskVolume,
        getTime: (item) => item.windowEndTime
    });

    $: scaledPrice = generateMeasurementPoints({
        data: $filteredList,
        getPrice: (item) => item.askPriceAtWindowEnd,
        getMeasurement: (item) => item.askPriceAtWindowEnd,
        getTime: (item) => item.windowEndTime
    });

    $: measurementPointsString = scaledMeasurements
        .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
        .sort((a, b) => a.x - b.x)
        .map((item) => `${item.x},${item.y}`)
        .join(' ');

    $: pricePointsString = scaledPrice
        .filter((item) => !isNaN(item.x) && !isNaN(item.y) && item.x !== null && item.y !== null)
        .sort((a, b) => a.x - b.x)
        .map((item) => `${item.x},${item.y}`)
        .join(' ');

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
            height: svgHeight,
            topOffset,
            bottomOffset,
            minValue: measuredMinimum * 0.98,
            maxValue: measuredMaximum * 1.02,
            makeScale: autoMagnitude
        });

        priceTicks = makeVerticalTicks({
            height: svgHeight,
            topOffset,
            bottomOffset,
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
        <svg width={svgWidth} height={svgHeight} xmlns="http://www.w3.org/2000/svg" on:mousemove={snapToPolyLinePoint}>
            <line
                id="timeScale"
                x1={leftOffset}
                y1={svgHeight - topOffset}
                x2={svgWidth - rightOffset}
                y2={svgHeight - topOffset}
                stroke="black"
            />
            <line
                id="measurementScale"
                x1={leftOffset}
                y1={bottomOffset}
                x2={leftOffset}
                y2={svgHeight - topOffset}
                stroke="black"
            />
            <line
                id="priceScale"
                x1={svgWidth - leftOffset}
                y1={bottomOffset}
                x2={svgWidth - leftOffset}
                y2={svgHeight - topOffset}
                stroke="black"
            />

            <g id="timeTicks" font-size="10" text-anchor="middle">
                {#each horizontalTickPositions.slice(1, -1) as { x, label }}
                    <line x1={x} y1={svgHeight - topOffset - 5} x2={x} y2={svgHeight - topOffset + 5} stroke="black" />
                    <text {x} y={svgHeight - topOffset + 15}>{label}</text>
                {/each}
            </g>

            <g id="measurementTicks" font-size="10" text-anchor="end">
                {#each measurementTicks as { y, label }}
                    {#if y !== undefined && !isNaN(y)}
                        <line x1={rightOffset - 5} y1={y} x2={rightOffset + 5} y2={y} stroke="black" />
                        <text x={rightOffset - 10} y={y + 3}>{label}</text>
                    {/if}
                {/each}
            </g>

            <g id="priceTicks" font-size="10" text-anchor="end">
                {#each priceTicks.slice(1) as { y, label }}
                    {#if y !== undefined && !isNaN(y)}
                        <line
                            x1={svgWidth - leftOffset - 5}
                            y1={y}
                            x2={svgWidth - leftOffset + 5}
                            y2={y}
                            stroke="black"
                        />
                        <text x={svgWidth - leftOffset + 30} y={y + 3}>{label}</text>
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
    <select id="tickerSelect" bind:value={selectedSymbol} on:change={handleSelect}>
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
