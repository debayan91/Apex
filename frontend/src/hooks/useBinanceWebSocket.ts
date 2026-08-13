import { useState, useEffect, useRef } from 'react';

export interface BinanceWebSocketResult {
  currentPrice: number;
  priceTrend: 'up' | 'down' | null;
  isFlashing: 'flash-up' | 'flash-down' | '';
}

export function useBinanceWebSocket(selectedPair: string): BinanceWebSocketResult {
  const [currentPrice, setCurrentPrice] = useState<number>(0);
  const [priceTrend, setPriceTrend] = useState<'up' | 'down' | null>(null);
  const [isFlashing, setIsFlashing] = useState<'flash-up' | 'flash-down' | ''>('');
  
  const lastRenderTimeRef = useRef<number>(0);

  // Fetch Latest Price
  const fetchPrice = async () => {
    try {
      const response = await fetch(`/prices/latest?symbol=${selectedPair}`);
      if (!response.ok) return;
      const data = await response.json();
      let newPrice = 0;
      if (Array.isArray(data)) {
        const found = data.find((p: any) => p.symbol === selectedPair);
        if (found) newPrice = Number(found.price);
      } else if (data && data.price) {
        newPrice = Number(data.price);
      }

      if (newPrice > 0) {
        setCurrentPrice((prevPrice) => {
          if (prevPrice !== 0 && newPrice !== prevPrice) {
            if (newPrice > prevPrice) {
              setPriceTrend('up');
              setIsFlashing('flash-up');
            } else {
              setPriceTrend('down');
              setIsFlashing('flash-down');
            }
            setTimeout(() => setIsFlashing(''), 800);
          }
          return newPrice;
        });
      }
    } catch (err) {
      console.error('Failed to fetch price', err);
    }
  };

  useEffect(() => {
    setCurrentPrice(0);
    setPriceTrend(null);
    setIsFlashing('');
    lastRenderTimeRef.current = 0;
    
    // First, fetch latest price as immediate fallback / starting baseline
    fetchPrice();

    const wsSymbol = selectedPair.toLowerCase();
    const wsUrl = `wss://stream.binance.com:9443/ws/${wsSymbol}@trade`;
    let ws: WebSocket | null = null;
    let fallbackInterval: NodeJS.Timeout | null = null;

    try {
      ws = new WebSocket(wsUrl);

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (data && data.p) {
            const newPrice = parseFloat(data.p);
            
            // Throttle UI renders to ~4 FPS (every 250ms) to prevent massive React re-render cascades
            const now = Date.now();
            if (now - lastRenderTimeRef.current > 250) {
              lastRenderTimeRef.current = now;
              
              setCurrentPrice((prevPrice) => {
                if (prevPrice !== 0 && newPrice !== prevPrice) {
                  if (newPrice > prevPrice) {
                    setPriceTrend('up');
                    setIsFlashing('flash-up');
                  } else {
                    setPriceTrend('down');
                    setIsFlashing('flash-down');
                  }
                  setTimeout(() => setIsFlashing(''), 800);
                }
                return newPrice;
              });
            }
          }
        } catch (e) {
          console.error("WS message parse error", e);
        }
      };

      ws.onerror = (err) => {
        console.warn("WebSocket error, falling back to REST polling", err);
        if (!fallbackInterval) {
          fallbackInterval = setInterval(fetchPrice, 2000);
        }
      };

      ws.onclose = () => {
        console.log("WebSocket closed");
      };
    } catch (e) {
      console.warn("WebSocket failed to initialize, using fallback", e);
      fallbackInterval = setInterval(fetchPrice, 2000);
    }

    return () => {
      if (ws) ws.close();
      if (fallbackInterval) clearInterval(fallbackInterval);
    };
  }, [selectedPair]);

  return {
    currentPrice,
    priceTrend,
    isFlashing
  };
}
