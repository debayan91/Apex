import { useState, useEffect } from 'react';
import { TopBar } from './components/TopBar';
import { LeftSidebar } from './components/LeftSidebar';
import { MainChart } from './components/MainChart';
import type { TradeMarker } from './components/MainChart';
import { RightSidebar } from './components/RightSidebar';
import { BottomPanel } from './components/BottomPanel';
import { useBinanceWebSocket } from './hooks/useBinanceWebSocket';

interface Holding {
  symbol: string;
  quantity: number;
  averagePrice: number;
}

interface PortfolioSummary {
  cashBalance: number;
  totalValue: number;
  holdings: Holding[];
}

function App() {
  const USER_ID = 1;
  const [walletBalance, setWalletBalance] = useState<number>(0);
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [selectedPair, setSelectedPair] = useState<string>('BTCUSDT');
  
  // Order feedback state
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [orderFeedback, setOrderFeedback] = useState<{msg: string, type: 'success' | 'error'} | null>(null);
  
  // Trade Markers for the chart
  const [tradeMarkers, setTradeMarkers] = useState<TradeMarker[]>([]);

  const { currentPrice, priceTrend, isFlashing } = useBinanceWebSocket(selectedPair);

  // Fetch Wallet and Portfolio
  const fetchPortfolio = async () => {
    try {
      const response = await fetch(`/portfolio/${USER_ID}`);
      if (!response.ok) return;
      const data: PortfolioSummary = await response.json();
      setWalletBalance(data.cashBalance || 0);
      setHoldings(data.holdings || []);
    } catch (err) {
      console.error('Failed to fetch portfolio', err);
    }
  };

  useEffect(() => {
    fetchPortfolio();
    const portfolioInterval = setInterval(fetchPortfolio, 10000);
    return () => clearInterval(portfolioInterval);
  }, []);

  const handleOrderSubmit = async (side: string, type: string, quantity: number, price?: number) => {
    setIsSubmitting(true);
    setOrderFeedback({ msg: 'Processing...', type: 'success' });

    const orderRequest = {
      userId: USER_ID,
      symbol: selectedPair,
      side: side,
      quantity: quantity,
      price: type === 'Limit' ? price : null
    };

    const idempotencyKey = 'id-key-' + Math.random().toString(36).substring(2) + Date.now();

    try {
      const response = await fetch('/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify(orderRequest)
      });
      const result = await response.json();
      
      if (response.ok || response.status === 201) {
        setOrderFeedback({ msg: 'Order submitted successfully', type: 'success' });
        fetchPortfolio(); // Update wallet immediately
        
        // Add marker to chart
        const executedPrice = type === 'Limit' && price ? price : currentPrice;
        setTradeMarkers(prev => [...prev, {
          time: Math.floor(Date.now() / 1000),
          type: side as 'BUY' | 'SELL',
          price: executedPrice
        }]);
      } else {
        setOrderFeedback({ msg: result.message || 'Order failed', type: 'error' });
      }
    } catch (err) {
      setOrderFeedback({ msg: 'Connection failed', type: 'error' });
    } finally {
      setIsSubmitting(false);
      setTimeout(() => setOrderFeedback(null), 5000);
    }
  };

  return (
    <div className="h-screen w-screen overflow-hidden flex flex-col bg-exchange-bg">
      <TopBar
        symbol={selectedPair}
        price={currentPrice}
        trend={priceTrend}
        isFlashing={isFlashing}
      />
      
      <div className="flex-1 flex overflow-hidden">
        <div className="w-[280px] hidden lg:block border-r border-exchange-border flex-shrink-0">
          <LeftSidebar
            selectedSymbol={selectedPair}
            onSelectSymbol={setSelectedPair}
          />
        </div>
        
        <div className="flex-1 flex min-w-0">
          {/* Center Column: Chart + Bottom Panel */}
          <div className="flex-1 flex flex-col min-w-0">
            <div className="flex-1 min-h-0">
              <MainChart 
                symbol={selectedPair} 
                tradeMarkers={tradeMarkers}
              />
            </div>
            <BottomPanel holdings={holdings} currentPrice={currentPrice} />
          </div>
          
          {/* Right Column: Order Panel + Order Book */}
          <div className="hidden xl:block h-full border-l border-exchange-border">
             <RightSidebar
               symbol={selectedPair}
               currentPrice={currentPrice}
               walletBalance={walletBalance}
               onSubmitOrder={handleOrderSubmit}
               isSubmitting={isSubmitting}
               orderFeedback={orderFeedback}
             />
          </div>
        </div>
      </div>
      
      {/* Mobile/Tablet Fallback overlays or adjusted views would go here */}
    </div>
  );
}

export default App;
