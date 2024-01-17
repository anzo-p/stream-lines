import { writable } from 'svelte/store';
import type { CryptoQuotation } from '../types/CryptoQuotation';

const map = new Map<string, CryptoQuotation>();
const sortedList = writable<CryptoQuotation[]>([]);

export function addItem(item: CryptoQuotation): void {
    addItems([item]);
    sortList();
}

export function addItems(items: CryptoQuotation[]): void {
    items.forEach((item) => {
        map.set(item.measureId, item);
    });
    sortList();
}

function sortList() {
    const newItems = Array.from(map.values()).sort((a, b) => b.windowEndTime - a.windowEndTime);
    sortedList.set(newItems);
}

export const listStore = {
    subscribe: sortedList.subscribe,
    addItems,
    addItem
};
