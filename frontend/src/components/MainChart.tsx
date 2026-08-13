import React, { useEffect, useRef, useState } from 'react';
import { createChart } from 'lightweight-charts';
import type { IChartApi, ISeriesApi, SeriesMarker, Time } from 'lightweight-charts';
import { Settings } from 'lucide-react';
import { cn } from '../lib/utils';

export interface TradeMarker {
  time: number;
  type: 'BUY' | 'SELL';
  price: number;
}

interface MainChartProps {
  symbol: string;
  tradeMarkers?: TradeMarker[];
}

const TIMEFRAMES = ['1m', '5m', '15m', '1H', '4H', '1D', '1W'];

// Simple Moving Average calculator for historical data
const calculateHistoricalSMA = (data: any[], period: number) => {
  const sma = [];
  let sum = 0;
  for (let i = 0; i < data.length; i++) {
    sum += data[i].close;
    if (i >= period) {
      sum -= data[i - period].close;
      sma.push({ time: data[i].time, value: sum / period });
    } else if (i === period - 1) {
      sma.push({ time: data[i].time, value: sum / period });
    }
  }
  return sma;
};

export const MainChart: React.FC<MainChartProps> = ({ symbol, tradeMarkers = [] }) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<"Histogram"> | null>(null);
  const ma20SeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  const ma50SeriesRef = useRef<ISeriesApi<"Line"> | null>(null);
  
  // Buffers for live SMA calculation
  const historicalClosesRef = useRef<{time: number, close: number}[]>([]);

  const [activeTimeframe, setActiveTimeframe] = useState('1m');

  // Legend State
  const [legendData, setLegendData] = useState<{
    time: string;
    open: number;
    high: number;
    low: number;
    close: number;
    vol: number;
    ma20?: number;
    ma50?: number;
  } | null>(null);

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { color: '#0B0E11' },
        textColor: '#848E9C',
        fontFamily: 'Inter, sans-serif',
      },
      grid: {
        vertLines: { color: '#2B3139', style: 1 },
        horzLines: { color: '#2B3139', style: 1 },
      },
      crosshair: {
        mode: 0,
        vertLine: { width: 1, color: '#848E9C', style: 3, labelBackgroundColor: '#474D57' },
        horzLine: { width: 1, color: '#848E9C', style: 3, labelBackgroundColor: '#474D57' },
      },
      timeScale: {
        borderColor: '#2B3139',
        timeVisible: true,
        secondsVisible: false,
      },
      rightPriceScale: {
        borderColor: '#2B3139',
        autoScale: true, 
      },
    });

    const candleSeries = chart.addCandlestickSeries({
      upColor: '#0ECB81',
      downColor: '#F6465D',
      borderVisible: false,
      wickUpColor: '#0ECB81',
      wickDownColor: '#F6465D',
    });

    const volumeSeries = chart.addHistogramSeries({
      color: '#26a69a',
      priceFormat: { type: 'volume' },
      priceScaleId: '',
    });
    volumeSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.8, bottom: 0 },
    });

    const ma20Series = chart.addLineSeries({
      color: '#F3BA2F',
      lineWidth: 1,
      crosshairMarkerVisible: false,
      lastValueVisible: false,
      priceLineVisible: false,
    });

    const ma50Series = chart.addLineSeries({
      color: '#2962FF',
      lineWidth: 1,
      crosshairMarkerVisible: false,
      lastValueVisible: false,
      priceLineVisible: false,
    });

    chartRef.current = chart;
    seriesRef.current = candleSeries;
    volumeSeriesRef.current = volumeSeries;
    ma20SeriesRef.current = ma20Series;
    ma50SeriesRef.current = ma50Series;

    // Crosshair subscribe for legend
    chart.subscribeCrosshairMove((param) => {
      if (!param.time || !param.seriesData.size) {
        // Fallback to latest candle if available
        const latestData = candleSeries.data();
        if (latestData && latestData.length > 0) {
          const lastCandle = latestData[latestData.length - 1] as any;
          const volData = volumeSeries.data();
          const lastVol = volData[volData.length - 1] as any;
          const ma20Data = ma20Series.data();
          const lastMa20 = ma20Data[ma20Data.length - 1] as any;
          const ma50Data = ma50Series.data();
          const lastMa50 = ma50Data[ma50Data.length - 1] as any;

          setLegendData({
            time: new Date(Number(lastCandle.time) * 1000).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
            open: lastCandle.open,
            high: lastCandle.high,
            low: lastCandle.low,
            close: lastCandle.close,
            vol: lastVol ? lastVol.value : 0,
            ma20: lastMa20 ? lastMa20.value : undefined,
            ma50: lastMa50 ? lastMa50.value : undefined,
          });
        }
        return;
      }

      const candleData = param.seriesData.get(candleSeries) as any;
      const volData = param.seriesData.get(volumeSeries) as any;
      const ma20Val = param.seriesData.get(ma20Series) as any;
      const ma50Val = param.seriesData.get(ma50Series) as any;

      if (candleData) {
        setLegendData({
          time: new Date(Number(param.time) * 1000).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
          open: candleData.open,
          high: candleData.high,
          low: candleData.low,
          close: candleData.close,
          vol: volData ? volData.value : 0,
          ma20: ma20Val ? ma20Val.value : undefined,
          ma50: ma50Val ? ma50Val.value : undefined,
        });
      }
    });

    let ws: WebSocket | null = null;
    let isUnmounted = false;

    // 1. Fetch REST History
    fetch(`https://api.binance.com/api/v3/klines?symbol=${symbol}&interval=${activeTimeframe}&limit=500`)
      .then(res => res.json())
      .then(data => {
        if (isUnmounted) return;

        const klines = data.map((d: any) => ({
          time: (Math.floor(d[0] / 1000)) as Time,
          open: parseFloat(d[1]),
          high: parseFloat(d[2]),
          low: parseFloat(d[3]),
          close: parseFloat(d[4]),
          volume: parseFloat(d[5]),
        }));
        
        candleSeries.setData(klines);
        historicalClosesRef.current = klines.map((k: any) => ({ time: Number(k.time), close: k.close }));
        
        const volumeData = klines.map((k: any) => ({
          time: k.time,
          value: k.volume,
          color: k.close >= k.open ? 'rgba(14, 203, 129, 0.5)' : 'rgba(246, 70, 93, 0.5)'
        }));
        volumeSeries.setData(volumeData);

        const ma20Data = calculateHistoricalSMA(klines, 20);
        ma20Series.setData(ma20Data);
        
        const ma50Data = calculateHistoricalSMA(klines, 50);
        ma50Series.setData(ma50Data);

        if (klines.length > 0) {
          const last = klines[klines.length - 1];
          setLegendData({
            time: new Date(Number(last.time) * 1000).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}),
            open: last.open,
            high: last.high,
            low: last.low,
            close: last.close,
            vol: last.volume,
            ma20: ma20Data.length > 0 ? ma20Data[ma20Data.length - 1].value : undefined,
            ma50: ma50Data.length > 0 ? ma50Data[ma50Data.length - 1].value : undefined,
          });
        }

        // 2. Open WebSocket for live kline updates
        const wsSymbol = symbol.toLowerCase();
        ws = new WebSocket(`wss://stream.binance.com:9443/ws/${wsSymbol}@kline_${activeTimeframe}`);

        ws.onmessage = (event) => {
          if (isUnmounted) return;
          try {
            const msg = JSON.parse(event.data);
            if (msg.e === 'kline' && msg.k) {
              const k = msg.k;
              const candleTime = Math.floor(k.t / 1000) as Time;
              const cClose = parseFloat(k.c);
              const cOpen = parseFloat(k.o);

              candleSeries.update({
                time: candleTime,
                open: cOpen,
                high: parseFloat(k.h),
                low: parseFloat(k.l),
                close: cClose,
              });

              volumeSeries.update({
                time: candleTime,
                value: parseFloat(k.v),
                color: cClose >= cOpen ? 'rgba(14, 203, 129, 0.5)' : 'rgba(246, 70, 93, 0.5)'
              });

              // Dynamic SMA Calculation
              const closes = historicalClosesRef.current;
              if (closes.length > 0) {
                const lastIdx = closes.length - 1;
                if (closes[lastIdx].time === Number(candleTime)) {
                  closes[lastIdx].close = cClose; // update existing candle
                } else if (Number(candleTime) > closes[lastIdx].time) {
                  closes.push({ time: Number(candleTime), close: cClose }); // new candle
                  // Prevent memory leak by capping array
                  if (closes.length > 1000) closes.shift(); 
                }

                // Compute MA20
                if (closes.length >= 20) {
                  const slice = closes.slice(-20);
                  const sum = slice.reduce((acc, val) => acc + val.close, 0);
                  ma20Series.update({ time: candleTime, value: sum / 20 });
                }

                // Compute MA50
                if (closes.length >= 50) {
                  const slice = closes.slice(-50);
                  const sum = slice.reduce((acc, val) => acc + val.close, 0);
                  ma50Series.update({ time: candleTime, value: sum / 50 });
                }
              }
            }
          } catch (e) {
            console.error("Kline parsing error", e);
          }
        };
      })
      .catch(console.error);

    const handleResize = () => {
      if (chartContainerRef.current) {
        chart.applyOptions({ 
          width: chartContainerRef.current.clientWidth, 
          height: chartContainerRef.current.clientHeight 
        });
      }
    };

    window.addEventListener('resize', handleResize);
    setTimeout(handleResize, 100);

    return () => {
      isUnmounted = true;
      if (ws) ws.close();
      window.removeEventListener('resize', handleResize);
      chart.remove();
    };
  }, [symbol, activeTimeframe]);

  // Update markers
  useEffect(() => {
    if (seriesRef.current) {
      const markers: SeriesMarker<Time>[] = tradeMarkers.map(m => ({
        time: m.time as Time,
        position: m.type === 'BUY' ? 'belowBar' : 'aboveBar',
        color: m.type === 'BUY' ? '#0ECB81' : '#F6465D',
        shape: m.type === 'BUY' ? 'arrowUp' : 'arrowDown',
        text: `${m.type} @ ${m.price.toFixed(2)}`
      }));
      // Sort markers by time as required by lightweight-charts
      markers.sort((a, b) => Number(a.time) - Number(b.time));
      seriesRef.current.setMarkers(markers);
    }
  }, [tradeMarkers]);

  return (
    <div className="flex flex-col h-full bg-exchange-panel border-r border-exchange-border relative">
      {/* Chart Toolbar */}
      <div className="flex items-center justify-between px-4 py-2 border-b border-exchange-border bg-exchange-panel">
        <div className="flex items-center space-x-1">
          {TIMEFRAMES.map((tf) => (
            <button
              key={tf}
              onClick={() => setActiveTimeframe(tf)}
              className={cn(
                "px-2 py-1 text-xs font-medium rounded transition-colors",
                activeTimeframe === tf 
                  ? "text-exchange-text bg-exchange-hover" 
                  : "text-exchange-muted hover:text-exchange-text hover:bg-exchange-hover/50"
              )}
            >
              {tf}
            </button>
          ))}
        </div>
        <div className="flex items-center space-x-3 text-exchange-muted">
          <Settings size={16} className="cursor-pointer hover:text-exchange-text transition-colors" />
        </div>
      </div>
      
      {/* OHLC Legend Overlay */}
      {legendData && (
        <div className="absolute top-12 left-4 z-10 flex space-x-3 text-[11px] font-mono pointer-events-none">
          <span className="text-exchange-text font-bold">{symbol} · {activeTimeframe}</span>
          <span className="text-exchange-muted">O <span className={legendData.close >= legendData.open ? 'text-exchange-green' : 'text-exchange-red'}>{legendData.open.toFixed(2)}</span></span>
          <span className="text-exchange-muted">H <span className={legendData.close >= legendData.open ? 'text-exchange-green' : 'text-exchange-red'}>{legendData.high.toFixed(2)}</span></span>
          <span className="text-exchange-muted">L <span className={legendData.close >= legendData.open ? 'text-exchange-green' : 'text-exchange-red'}>{legendData.low.toFixed(2)}</span></span>
          <span className="text-exchange-muted">C <span className={legendData.close >= legendData.open ? 'text-exchange-green' : 'text-exchange-red'}>{legendData.close.toFixed(2)}</span></span>
          <span className="text-exchange-muted">Vol <span className="text-exchange-text">{legendData.vol.toFixed(2)}</span></span>
          {legendData.ma20 && <span className="text-[#F3BA2F]">MA(20) {legendData.ma20.toFixed(2)}</span>}
          {legendData.ma50 && <span className="text-[#2962FF]">MA(50) {legendData.ma50.toFixed(2)}</span>}
        </div>
      )}

      {/* Chart Container */}
      <div ref={chartContainerRef} className="flex-1 w-full min-h-[400px]" />
    </div>
  );
};
