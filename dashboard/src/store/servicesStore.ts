import { initializeWebSocket } from '../lib/services/websocket';
import { graphQLClient } from '../lib/services/graphql/client';
import { writable } from 'svelte/store';

export const graphqlServiceProvider = writable(graphQLClient);

export const websocketServiceProvider = import.meta.env.SSR
    ? writable(null)
    : writable(initializeWebSocket(import.meta.env.VITE_LIVE_FEED_URL, () => {}));
