import React, { useState } from 'react';
import { cn } from '../lib/utils';

interface Holding {
  symbol: string;
  quantity: number;
  averagePrice: number;
}

interface BottomPanelProps {
  holdings: Holding[];
  currentPrice: number;
}

const TABS = ['Positions', 'Open Orders', 'Order History', 'Trade History'];

export const BottomPanel: React.FC<BottomPanelProps> = ({ holdings, currentPrice }) => {
  const [activeTab, setActiveTab] = useState('Positions');

  return (
    <div className="flex flex-col h-[280px] bg-exchange-panel border-t border-exchange-border overflow-hidden">
      <div className="flex px-4 border-b border-exchange-border">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              "py-3 px-4 text-sm font-medium transition-colors border-b-2",
              activeTab === tab
                ? "text-exchange-text border-[#F3BA2F]"
                : "text-exchange-muted border-transparent hover:text-exchange-text"
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-auto">
        {activeTab === 'Positions' && (
          <table className="w-full text-xs text-left">
            <thead className="text-exchange-muted sticky top-0 bg-exchange-panel">
              <tr>
                <th className="font-normal py-2 px-4">Symbol</th>
                <th className="font-normal py-2 px-4">Size</th>
                <th className="font-normal py-2 px-4">Entry Price</th>
                <th className="font-normal py-2 px-4">Mark Price</th>
                <th className="font-normal py-2 px-4">Liq. Price</th>
                <th className="font-normal py-2 px-4 text-right">Margin Ratio</th>
                <th className="font-normal py-2 px-4 text-right">Margin</th>
                <th className="font-normal py-2 px-4 text-right">PNL (ROE%)</th>
                <th className="font-normal py-2 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="text-exchange-text font-medium">
              {holdings.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-8 text-exchange-muted">
                    No open positions
                  </td>
                </tr>
              ) : (
                holdings.map((h, i) => {
                  const isBtc = h.symbol === 'BTCUSDT' || h.symbol === 'BTC';
                  const mark = isBtc ? currentPrice : currentPrice * 0.05; // Mock logic for other coins just to not break if selectedPair isn't BTC
                  const entry = h.averagePrice || mark; // Real entry price!
                  const pnl = (mark - entry) * h.quantity;
                  const pnlPercent = entry > 0 ? ((mark - entry) / entry) * 100 : 0;
                  const isPositive = pnl >= 0;

                  return (
                    <tr key={i} className="hover:bg-exchange-hover border-b border-exchange-border/50 transition-colors">
                      <td className="py-2 px-4 font-bold">
                        <span className="text-exchange-green px-1 bg-exchange-green/10 rounded text-[10px] mr-2">10x</span>
                        {h.symbol}
                      </td>
                      <td className="py-2 px-4">{h.quantity.toFixed(4)}</td>
                      <td className="py-2 px-4">{entry.toFixed(2)}</td>
                      <td className="py-2 px-4">{mark.toFixed(2)}</td>
                      <td className="py-2 px-4 text-exchange-muted">{(entry * 0.9).toFixed(2)}</td>
                      <td className="py-2 px-4 text-right">8.5%</td>
                      <td className="py-2 px-4 text-right">{(entry * h.quantity * 0.1).toFixed(2)}</td>
                      <td className={cn("py-2 px-4 text-right", isPositive ? "text-exchange-green" : "text-exchange-red")}>
                        {isPositive ? '+' : ''}{pnl.toFixed(2)} ({isPositive ? '+' : ''}{pnlPercent.toFixed(2)}%)
                      </td>
                      <td className="py-2 px-4 text-right">
                        <button className="text-exchange-text underline decoration-exchange-muted hover:decoration-exchange-text underline-offset-2 text-[11px] mr-2">
                          Limit
                        </button>
                        <button className="text-exchange-text underline decoration-exchange-muted hover:decoration-exchange-text underline-offset-2 text-[11px]">
                          Market
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        )}
        
        {activeTab !== 'Positions' && (
           <div className="flex items-center justify-center h-full text-exchange-muted text-xs">
             No {activeTab.toLowerCase()} to display.
           </div>
        )}
      </div>
    </div>
  );
};
