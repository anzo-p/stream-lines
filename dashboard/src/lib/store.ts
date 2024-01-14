import { writable } from 'svelte/store';
import type { CryptoQuotation } from './types';

const list = writable<CryptoQuotation[]>([]);
const maxElements = 60 * 6;

export function addItem(item: CryptoQuotation) {
    console.log('Adding item', item);
    list.update((items) => {
        let newItems = [...items, item];
        if (newItems.length > maxElements) {
            newItems = newItems.slice(newItems.length - maxElements);
        }
        return newItems;
    });
}

export const listStore = {
    subscribe: list.subscribe,
    addItem
};
