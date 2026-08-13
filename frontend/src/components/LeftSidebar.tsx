import React, { useState, useEffect } from 'react';
import { Search, Star } from 'lucide-react';
import { cn } from '../lib/utils';

interface WatchlistItem {
  symbol: string;
  price: number;
  change: number;
}

interface LeftSidebarProps {
  selectedSymbol: string;
  onSelectSymbol: (symbol: string) => void;
}

const DEFAULT_SYMBOLS = ['BTCUSDT', 'ETHUSDT', 'SOLUSDT', 'BNBUSDT', 'DOGEUSDT', 'XRPUSDT', 'ADAUSDT'];

export const LeftSidebar: React.FC<LeftSidebarProps> = ({ selectedSymbol, onSelectSymbol }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [watchlist, setWatchlist] = useState<WatchlistItem[]>(
    DEFAULT_SYMBOLS.map(s => ({ symbol: s, price: 0, change: 0 }))
  );

  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    const fetchWatchlist = async () => {
      try {
        // Fetch 24h ticker for all symbols at once (Binance API allows array of symbols or all)
        // To save bandwidth we fetch all and filter, or fetch specific symbols
        const symbolsParam = JSON.stringify(DEFAULT_SYMBOLS);
        const res = await fetch(`https://api.binance.com/api/v3/ticker/24hr?symbols=${symbolsParam}`);
        const data = await res.json();
        
        if (Array.isArray(data)) {
          const updated = data.map((d: any) => ({
            symbol: d.symbol,
            price: parseFloat(d.lastPrice),
            change: parseFloat(d.priceChangePercent)
          }));
          
          // Sort by BTC/ETH first, then rest
          updated.sort((a, b) => {
            if (a.symbol === 'BTCUSDT') return -1;
            if (b.symbol === 'BTCUSDT') return 1;
            if (a.symbol === 'ETHUSDT') return -1;
            if (b.symbol === 'ETHUSDT') return 1;
            return 0;
          });
          
          setWatchlist(updated);
        }
      } catch (err) {
        console.error("Failed to fetch watchlist", err);
      }
    };

    fetchWatchlist();
    interval = setInterval(fetchWatchlist, 5000); // Poll every 5s

    return () => clearInterval(interval);
  }, []);

  const filteredWatchlist = watchlist.filter(item => 
    item.symbol.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex flex-col h-full bg-exchange-panel border-r border-exchange-border overflow-hidden">
      <div className="p-3">
        <div className="relative flex items-center w-full bg-exchange-bg rounded-md border border-exchange-border px-3 py-1.5 focus-within:border-exchange-muted transition-colors">
          <Search size={14} className="text-exchange-muted mr-2" />
          <input
            type="text"
            placeholder="Search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-transparent text-sm text-exchange-text outline-none placeholder:text-exchange-muted"
          />
        </div>
      </div>

      <div className="flex text-xs font-semibold text-exchange-muted px-4 py-2 border-b border-exchange-border">
        <div className="w-1/2">Pair</div>
        <div className="w-1/4 text-right">Price</div>
        <div className="w-1/4 text-right">Change</div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {filteredWatchlist.map((item) => {
          const isSelected = item.symbol === selectedSymbol;
          const isPositive = item.change >= 0;
          return (
            <div
              key={item.symbol}
              onClick={() => onSelectSymbol(item.symbol)}
              className={cn(
                "flex items-center px-4 py-2 cursor-pointer hover:bg-exchange-hover transition-colors text-sm",
                isSelected && "bg-exchange-hover"
              )}
            >
              <div className="w-1/2 flex items-center space-x-2">
                <Star size={14} className="text-exchange-muted hover:text-[#F3BA2F]" />
                <span className="font-medium text-exchange-text">
                  {item.symbol.replace('USDT', '')}<span className="text-exchange-muted text-xs">/USDT</span>
                </span>
              </div>
              <div className="w-1/4 text-right tabular-nums">
                {item.price > 0 ? item.price.toLocaleString(undefined, { maximumFractionDigits: item.price < 1 ? 4 : 2 }) : '---'}
              </div>
              <div className={cn(
                "w-1/4 text-right tabular-nums",
                isPositive ? "text-exchange-green" : "text-exchange-red"
              )}>
                {item.change !== 0 ? `${isPositive ? '+' : ''}${item.change.toFixed(2)}%` : '---'}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
