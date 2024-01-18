import type { WindowedQuotation } from '../types/CryptoQuotation';
import { readCryptoQuotationFromJson } from '../types/CryptoQuotation';

export function initializeWebSocket(url: string, onDataCallback: (data: any) => void) {
    if (import.meta.env.SSR) {
        return {
            setOnNewDataCallback: (callback: (data: any) => void) => {},
            closeWebSocket: () => {}
        };
    }

    const ws = new WebSocket(url);

    let onNewData: (data: any) => void = onDataCallback;

    ws.onopen = () => {
        setTimeout(() => {
            ws.send(JSON.stringify({ type: 'Hello from client' }));
        }, 3000);
    };

    ws.onmessage = (event: MessageEvent) => {
        try {
            const jsonData = JSON.parse(event.data);
            const potentialQuotation = readCryptoQuotationFromJson(jsonData);
            if (potentialQuotation !== null) {
                onNewData(potentialQuotation as unknown as WindowedQuotation);
            } else {
                console.log('Received data is not a CryptoQuotation:', potentialQuotation);
            }
        } catch (error) {
            console.error('Error parsing WebSocket data:', error);
        }
    };

    ws.onerror = (error: Event) => {
        console.error('WebSocket Error:', error);
    };

    ws.onclose = () => {
        console.log('WebSocket connection closed');
    };

    return {
        setOnNewDataCallback: (callback: (data: any) => void) => {
            onNewData = callback;
        },
        closeWebSocket: () => {
            ws.close();
        }
    };
}
