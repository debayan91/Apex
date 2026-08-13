import React, { useState, useEffect } from 'react';
import { cn } from '../lib/utils';

interface RightSidebarProps {
  symbol: string;
  currentPrice: number;
  walletBalance: number;
  onSubmitOrder: (side: string, type: string, quantity: number, price?: number) => void;
  isSubmitting: boolean;
  orderFeedback: { msg: string, type: 'success' | 'error' } | null;
}

export const RightSidebar: React.FC<RightSidebarProps> = ({
  symbol,
  currentPrice,
  walletBalance,
  onSubmitOrder,
  isSubmitting,
  orderFeedback
}) => {
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [orderType, setOrderType] = useState<'Limit' | 'Market'>('Limit');
  const [priceStr, setPriceStr] = useState<string>('');
  const [quantityStr, setQuantityStr] = useState<string>('');
  
  // Real OrderBook state
  const [asks, setAsks] = useState<{p: number, q: number}[]>([]);
  const [bids, setBids] = useState<{p: number, q: number}[]>([]);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    const fetchDepth = async () => {
      try {
        const res = await fetch(`https://api.binance.com/api/v3/depth?symbol=${symbol}&limit=10`);
        const data = await res.json();
        
        if (data.asks && data.bids) {
          // data.asks is [[price, qty], ...]
          // We reverse asks so the lowest ask is at the bottom of the top half
          setAsks(data.asks.map((a: string[]) => ({ p: parseFloat(a[0]), q: parseFloat(a[1]) })).reverse());
          // bids are already highest to lowest
          setBids(data.bids.map((b: string[]) => ({ p: parseFloat(b[0]), q: parseFloat(b[1]) })));
        }
      } catch (err) {
        console.error("Failed to fetch depth", err);
      }
    };

    fetchDepth();
    interval = setInterval(fetchDepth, 2000); // Poll every 2s to not hit rate limits

    return () => clearInterval(interval);
  }, [symbol]);

  const handleOrder = (e: React.FormEvent) => {
    e.preventDefault();
    const qty = parseFloat(quantityStr);
    const p = priceStr ? parseFloat(priceStr) : undefined;
    if (isNaN(qty)) return;
    
    onSubmitOrder(side, orderType, qty, orderType === 'Limit' ? p : undefined);
  };

  const isBuy = side === 'BUY';

  return (
    <div className="flex flex-col h-full bg-exchange-panel w-[320px] flex-shrink-0">
      {/* Order Entry Panel */}
      <div className="p-4 border-b border-exchange-border">
        <div className="flex space-x-1 bg-[#1E2329] p-1 rounded-md mb-4">
          <button
            onClick={() => setSide('BUY')}
            className={cn(
              "flex-1 py-1.5 text-sm font-semibold rounded transition-colors",
              isBuy ? "bg-exchange-green text-white" : "text-exchange-muted hover:text-exchange-text"
            )}
          >
            Buy
          </button>
          <button
            onClick={() => setSide('SELL')}
            className={cn(
              "flex-1 py-1.5 text-sm font-semibold rounded transition-colors",
              !isBuy ? "bg-exchange-red text-white" : "text-exchange-muted hover:text-exchange-text"
            )}
          >
            Sell
          </button>
        </div>

        <div className="flex space-x-4 mb-4 text-sm font-medium">
          <button 
            onClick={() => setOrderType('Limit')}
            className={cn(orderType === 'Limit' ? "text-exchange-text" : "text-exchange-muted")}
          >
            Limit
          </button>
          <button 
            onClick={() => setOrderType('Market')}
            className={cn(orderType === 'Market' ? "text-exchange-text" : "text-exchange-muted")}
          >
            Market
          </button>
        </div>

        <form onSubmit={handleOrder} className="space-y-4">
          <div className="flex justify-between text-xs text-exchange-muted">
            <span>Avail</span>
            <span className="text-exchange-text font-medium">{walletBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })} USDT</span>
          </div>

          {orderType === 'Limit' && (
            <div className="relative flex items-center bg-[#1E2329] rounded border border-transparent focus-within:border-[#F3BA2F] transition-colors">
              <span className="text-exchange-muted text-xs pl-3 w-16">Price</span>
              <input
                type="number"
                step="0.01"
                value={priceStr}
                onChange={(e) => setPriceStr(e.target.value)}
                placeholder={currentPrice.toFixed(2)}
                className="w-full bg-transparent text-sm text-right text-exchange-text py-2 pr-3 outline-none"
              />
              <span className="text-exchange-muted text-xs pr-3">USDT</span>
            </div>
          )}

          <div className="relative flex items-center bg-[#1E2329] rounded border border-transparent focus-within:border-[#F3BA2F] transition-colors">
            <span className="text-exchange-muted text-xs pl-3 w-16">Amount</span>
            <input
              type="number"
              step="0.001"
              value={quantityStr}
              onChange={(e) => setQuantityStr(e.target.value)}
              className="w-full bg-transparent text-sm text-right text-exchange-text py-2 pr-3 outline-none"
            />
            <span className="text-exchange-muted text-xs pr-3">{symbol.replace('USDT', '')}</span>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className={cn(
              "w-full py-3 rounded text-white font-bold text-sm mt-2 transition-opacity hover:opacity-90",
              isBuy ? "bg-exchange-green" : "bg-exchange-red",
              isSubmitting && "opacity-50 cursor-not-allowed"
            )}
          >
            {isSubmitting ? 'Processing...' : `${isBuy ? 'Buy' : 'Sell'} ${symbol.replace('USDT', '')}`}
          </button>
        </form>

        {orderFeedback && (
          <div className={cn(
            "mt-3 text-xs p-2 rounded",
            orderFeedback.type === 'success' ? "bg-exchange-green/20 text-exchange-green" : "bg-exchange-red/20 text-exchange-red"
          )}>
            {orderFeedback.msg}
          </div>
        )}
      </div>

      {/* Order Book Mockup */}
      <div className="flex-1 flex flex-col min-h-[300px]">
        <div className="px-4 py-2 border-b border-exchange-border flex justify-between text-xs font-medium text-exchange-text">
          <span>Order Book</span>
        </div>
        <div className="flex text-[11px] text-exchange-muted px-4 pt-2 pb-1">
          <div className="w-1/3">Price(USDT)</div>
          <div className="w-1/3 text-right">Amount</div>
          <div className="w-1/3 text-right">Total</div>
        </div>
        
        <div className="flex-1 flex flex-col justify-between overflow-hidden text-[11px] tabular-nums font-medium">
          {/* Asks */}
          <div className="flex flex-col-reverse px-4">
            {asks.map((ask, i) => (
              <div key={i} className="flex relative py-0.5 cursor-pointer hover:bg-exchange-hover">
                <div className="absolute top-0 right-0 h-full bg-exchange-red/10" style={{ width: `${Math.min(100, ask.q * 50)}%` }} />
                <div className="w-1/3 text-exchange-red z-10">{ask.p.toFixed(2)}</div>
                <div className="w-1/3 text-right z-10 text-exchange-text">{ask.q.toFixed(3)}</div>
                <div className="w-1/3 text-right z-10 text-exchange-text">{(ask.p * ask.q).toFixed(2)}</div>
              </div>
            ))}
          </div>
          
          {/* Spread / Current Price */}
          <div className="py-2 px-4 flex items-center space-x-2">
            <span className="text-lg font-bold text-exchange-green">{currentPrice.toFixed(2)}</span>
            <span className="text-xs text-exchange-text line-through">${currentPrice.toFixed(2)}</span>
          </div>

          {/* Bids */}
          <div className="flex flex-col px-4">
            {bids.map((bid, i) => (
              <div key={i} className="flex relative py-0.5 cursor-pointer hover:bg-exchange-hover">
                <div className="absolute top-0 right-0 h-full bg-exchange-green/10" style={{ width: `${Math.min(100, bid.q * 50)}%` }} />
                <div className="w-1/3 text-exchange-green z-10">{bid.p.toFixed(2)}</div>
                <div className="w-1/3 text-right z-10 text-exchange-text">{bid.q.toFixed(3)}</div>
                <div className="w-1/3 text-right z-10 text-exchange-text">{(bid.p * bid.q).toFixed(2)}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
