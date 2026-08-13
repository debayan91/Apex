import React, { useEffect, useState } from 'react';
import { Settings, Maximize } from 'lucide-react';

interface TopBarProps {
  symbol: string;
  price: number;
  trend: 'up' | 'down' | null;
  isFlashing: string;
}

interface TickerData {
  priceChangePercent: number;
  highPrice: number;
  lowPrice: number;
  volume: number;
  quoteVolume: number;
}

export const TopBar: React.FC<TopBarProps> = ({
  symbol,
  price,
  trend,
  isFlashing
}) => {
  const [ticker, setTicker] = useState<TickerData | null>(null);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    const fetchTicker = async () => {
      try {
        const res = await fetch(`https://api.binance.com/api/v3/ticker/24hr?symbol=${symbol}`);
        const data = await res.json();
        setTicker({
          priceChangePercent: parseFloat(data.priceChangePercent),
          highPrice: parseFloat(data.highPrice),
          lowPrice: parseFloat(data.lowPrice),
          volume: parseFloat(data.volume),
          quoteVolume: parseFloat(data.quoteVolume)
        });
      } catch (err) {
        console.error("Failed to fetch 24h ticker", err);
      }
    };
    
    fetchTicker();
    interval = setInterval(fetchTicker, 5000); // Poll every 5 seconds
    
    return () => clearInterval(interval);
  }, [symbol]);

  const isPositive = ticker ? ticker.priceChangePercent >= 0 : true;
  const colorClass = isPositive ? 'text-exchange-green' : 'text-exchange-red';
  
  return (
    <div className="flex items-center justify-between px-4 py-2 border-b border-exchange-border bg-exchange-bg text-sm">
      <div className="flex items-center space-x-6">
        <div className="flex items-center space-x-2">
          {/* Mock logo for BTC or ETH */}
          <div className="w-6 h-6 rounded-full bg-[#F7931A] flex items-center justify-center text-white font-bold text-xs">
            {symbol.substring(0, 1)}
          </div>
          <h1 className="text-xl font-semibold text-exchange-text m-0 p-0 leading-none">{symbol}</h1>
        </div>

        <div className="flex flex-col">
          <span className={`text-lg font-bold transition-colors duration-300 ${isFlashing === 'flash-up' ? 'text-exchange-green bg-exchange-green/20' : isFlashing === 'flash-down' ? 'text-exchange-red bg-exchange-red/20' : trend === 'up' ? 'text-exchange-green' : trend === 'down' ? 'text-exchange-red' : 'text-exchange-text'}`}>
            {price > 0 ? price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '---'}
          </span>
          <span className="text-xs text-exchange-muted">
            ${price > 0 ? price.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '---'}
          </span>
        </div>

        <div className="flex flex-col">
          <span className="text-xs text-exchange-muted">24h Change</span>
          <span className={`text-sm ${colorClass}`}>
            {ticker ? `${isPositive ? '+' : ''}${ticker.priceChangePercent.toFixed(2)}%` : '---'}
          </span>
        </div>

        <div className="flex flex-col hidden md:flex">
          <span className="text-xs text-exchange-muted">24h High</span>
          <span className="text-sm">{ticker ? ticker.highPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '---'}</span>
        </div>

        <div className="flex flex-col hidden md:flex">
          <span className="text-xs text-exchange-muted">24h Low</span>
          <span className="text-sm">{ticker ? ticker.lowPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '---'}</span>
        </div>

        <div className="flex flex-col hidden lg:flex">
          <span className="text-xs text-exchange-muted">24h Vol ({symbol.replace('USDT', '')})</span>
          <span className="text-sm">{ticker ? ticker.volume.toLocaleString(undefined, { maximumFractionDigits: 2 }) : '---'}</span>
        </div>
      </div>

      <div className="flex items-center space-x-4 text-exchange-muted">
        <button className="hover:text-exchange-text transition-colors">
          <Settings size={18} />
        </button>
        <button className="hover:text-exchange-text transition-colors">
          <Maximize size={18} />
        </button>
      </div>
    </div>
  );
};
