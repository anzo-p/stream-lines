import { writable } from 'svelte/store';
import type { WindowedQuotation } from '../lib/types/CryptoQuotation';

const map = new Map<string, WindowedQuotation>();
const sortedList = writable<WindowedQuotation[]>([]);

export const listStore = {
    subscribe: sortedList.subscribe,
    addItems,
    addItem
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

function sortList() {
    const newItems = Array.from(map.values()).sort((a, b) => b.windowEndTime - a.windowEndTime);
    sortedList.set(newItems);
}
