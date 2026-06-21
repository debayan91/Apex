package com.example.Apex.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;

/**
 * Production-grade Binance WebSocket streaming client.
 *
 * Connects to the Binance public trade stream for BTCUSDT, parses each trade
 * event,
 * and delegates persistence to {@link MarketTickPersistenceHandler}.
 *
 * Features:
 * - Fully asynchronous and non-blocking (Reactor Netty)
 * - Automatic reconnection with exponential backoff (max 5 attempts, up to 32s)
 * - Structured logging for CONNECT, DISCONNECT, ERROR, RECONNECT events
 * - Configurable via apex.websocket.enabled (set to false in tests)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BinanceWebSocketClient implements ApplicationListener<ApplicationReadyEvent> {

    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(60);

    private final MarketTickPersistenceHandler persistenceHandler;
    private final ObjectMapper objectMapper;

    @Value("${apex.websocket.enabled:true}")
    private boolean enabled;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!enabled) {
            log.info("[WEBSOCKET] WebSocket client is disabled via apex.websocket.enabled=false");
            return;
        }
        startStreaming();
    }

    /**
     * Initiates the WebSocket connection with exponential backoff retry.
     * Runs entirely on Reactor scheduler threads — does not block application
     * threads.
     */
    private void startStreaming() {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();

        client.execute(URI.create(BINANCE_WS_URL), session -> {
            log.info("[CONNECT] Connected to Binance WebSocket: {}", BINANCE_WS_URL);

            return session.receive()
                    .filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(this::handleMessage)
                    .doOnError(err -> log.error("[ERROR] WebSocket stream error: {}", err.getMessage(), err))
                    .doFinally(signal -> log.warn("[DISCONNECT] WebSocket session ended. Signal: {}", signal))
                    .then();
        })
                .retryWhen(
                        Retry.backoff(MAX_RECONNECT_ATTEMPTS, INITIAL_BACKOFF)
                                .maxBackoff(MAX_BACKOFF)
                                .doBeforeRetry(retrySignal -> log.warn(
                                        "[RECONNECT_ATTEMPT] Attempt #{} after error: {}",
                                        retrySignal.totalRetries() + 1,
                                        retrySignal.failure().getMessage())))
                .doOnError(err -> log.error(
                        "[FATAL] WebSocket client exhausted all reconnect attempts. Manual restart required. Error: {}",
                        err.getMessage(), err))
                .subscribe();
    }

    /**
     * Parses a raw JSON text frame from Binance into a {@link BinanceTradeEvent}
     * and delegates it to the persistence handler.
     */
    private Mono<Void> handleMessage(String rawJson) {
        return Mono.fromRunnable(() -> {
            try {
                BinanceTradeEvent event = objectMapper.readValue(rawJson, BinanceTradeEvent.class);
                persistenceHandler.persist(event);
            } catch (Exception e) {
                log.error("[ERROR] Failed to parse trade event: {}. Raw: {}", e.getMessage(), rawJson);
            }
        });
    }
}
