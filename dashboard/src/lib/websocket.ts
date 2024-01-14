import type { CryptoQuotation } from '$lib/types';
import { isCryptoQuotation } from '$lib/types';

export function initializeWebSocket(url: string, onNewData: (data: any) => void) {
    const ws = new WebSocket(url);

    ws.onopen = () => {
        setTimeout(() => {
            ws.send(JSON.stringify({ type: 'Hello from client' }));
        }, 3000);
    };

    ws.onmessage = (event: MessageEvent) => {
        const newElement: any = event.data;

        try {
            const potentialQuotation = JSON.parse(event.data);
            if (isCryptoQuotation(potentialQuotation)) {
                onNewData(potentialQuotation as unknown as CryptoQuotation);
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

    return () => {
        ws.close();
    };
}
