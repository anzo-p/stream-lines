import { decompress } from '$lib/helpers/bitwise';
import { starterTickers } from '$lib/mockData';
import type { WindowedQuotation } from '../types/WindowedQuotation';
import { readQuotationFromJson } from '../types/WindowedQuotation';

export function initializeWebSocket(url: string, onDataCallback: (data: any) => void) {
  if (import.meta.env.SSR) {
    return {
      setOnNewDataCallback: (_: (data: any) => void) => {},
      subscribeTo: (_: string[]) => {},
      closeWebSocket: () => {}
    };
  }

  let onNewData: (data: any) => void = onDataCallback;
  let ws: WebSocket;
  let reconnectInterval = import.meta.env.VITE_WEBSOCKET_RECONNECT_INTERVAL || 10000;

  const reConnect = () => {
    ws = new WebSocket(url);

    ws.onopen = () => {
      setTimeout(() => {
        subscribeTo(starterTickers);
      }, 3000);
    };

    ws.onmessage = (event: MessageEvent) => {
      try {
        handleMessage(event, onNewData);
      } catch (error) {
        console.error('Error parsing WebSocket data:', error);
      }
    };

    ws.onerror = (error: Event) => {
      console.error('WebSocket Error:', error);
    };

    ws.onclose = () => {
      console.log(`WebSocket attempting reconnect in ${reconnectInterval}.`);
      setTimeout(reConnect, reconnectInterval);
    };
  };

  reConnect();

  const subscribeTo = (symbols: string[]) => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ subscribeTo: symbols }));
    }
  };

  return {
    setOnNewDataCallback: (callback: (data: any) => void) => {
      onNewData = callback;
    },
    subscribeTo,
    closeWebSocket: () => {
      ws.close();
    }
  };
}

function handleMessage(event: MessageEvent, cb: (data: any) => void): void {
  try {
    const message = decompress(event.data);
    const jsonData = JSON.parse(message);

    if (Array.isArray(jsonData)) {
      for (const data of jsonData) {
        const potentialQuotation = readQuotationFromJson(data);
        if (potentialQuotation !== null) {
          cb(potentialQuotation as unknown as WindowedQuotation);
        } else {
          console.log('Received data is not a CryptoQuotation:', potentialQuotation);
        }
      }
    }
  } catch (err) {
    throw new Error(`Error parsing WebSocket data:, ${err}`);
  }
}
