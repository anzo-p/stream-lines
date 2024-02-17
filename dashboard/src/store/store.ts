import { derived, writable } from 'svelte/store';
import type { WindowedQuotation } from '../lib/types/WindowedQuotation';

const map = new Map<string, WindowedQuotation>();
const sortedList = writable<WindowedQuotation[]>([]);
const selectedSymbols = writable<string[]>([]);

export const listStore = {
    subscribe: sortedList.subscribe,
    addItems,
    addItem,
    selectTickers: selectedSymbols.set
};

export function addItems(items: WindowedQuotation[]): void {
    items.forEach((item) => {
        map.set(item.measureId, item);
    });
    sortList();
}

export function addItem(item: WindowedQuotation): void {
    addItems([item]);
}

export const filteredList = derived([sortedList, selectedSymbols], ([$sortedList, $selectedSymbols]) => {
    return $sortedList.filter((item) => $selectedSymbols.includes(item.symbol));
});

function sortList() {
    const newItems = Array.from(map.values()).sort((a, b) => b.windowEndTime - a.windowEndTime);
    sortedList.set(newItems);
}
