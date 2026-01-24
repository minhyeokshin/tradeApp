package com.stock.trade.websocket;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.stock.trade.config.KisProperties;
import com.stock.trade.token.KisTokenManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class KisWebSocketClient extends TextWebSocketHandler {

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final ObjectMapper objectMapper;

    private WebSocketSession session;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);

    // 구독 중인 종목 관리 (TR_ID -> Set<종목코드>)
    private final ConcurrentMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    // 메시지 리스너
    private final List<KisWebSocketListener> listeners = new CopyOnWriteArrayList<>();

    // 재연결 스케줄러
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private int reconnectAttempts = 0;

    public KisWebSocketClient(KisProperties kisProperties, KisTokenManager tokenManager, ObjectMapper objectMapper) {
        this.kisProperties = kisProperties;
        this.tokenManager = tokenManager;
        this.objectMapper = objectMapper;
    }

    /**
     * WebSocket 연결
     */
    public synchronized void connect() {
        if (isConnected.get() || isConnecting.get()) {
            log.info("WebSocket 이미 연결되어 있거나 연결 중입니다.");
            return;
        }

        isConnecting.set(true);
        String wsUrl = kisProperties.getEffectiveWsUrl();
        log.info("KIS WebSocket 연결 시도 - URL: {}, 모드: {}",
                wsUrl, kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

            CompletableFuture<WebSocketSession> future = client.execute(
                    this,
                    headers,
                    URI.create(wsUrl)
            );

            this.session = future.get(10, TimeUnit.SECONDS);
            isConnected.set(true);
            isConnecting.set(false);
            reconnectAttempts = 0;
            log.info("KIS WebSocket 연결 성공");

        } catch (Exception e) {
            isConnecting.set(false);
            log.error("KIS WebSocket 연결 실패", e);
            scheduleReconnect();
        }
    }

    /**
     * WebSocket 연결 종료
     */
    public synchronized void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.NORMAL);
                log.info("KIS WebSocket 연결 종료");
            } catch (IOException e) {
                log.error("WebSocket 종료 중 오류", e);
            }
        }
        isConnected.set(false);
        subscriptions.clear();
    }

    /**
     * 실시간 데이터 구독
     *
     * @param trId   TR ID (예: H0STCNT0 - 실시간체결가)
     * @param trKey  종목코드 (예: 005930)
     */
    public void subscribe(String trId, String trKey) {
        if (!isConnected.get()) {
            log.warn("WebSocket이 연결되어 있지 않습니다. 먼저 connect()를 호출하세요.");
            connect();
        }

        sendSubscriptionMessage(trId, trKey, "1");
        subscriptions.computeIfAbsent(trId, k -> ConcurrentHashMap.newKeySet()).add(trKey);
        log.info("실시간 구독 요청 - TR_ID: {}, 종목코드: {}", trId, trKey);
    }

    /**
     * 실시간 데이터 구독 해제
     *
     * @param trId   TR ID
     * @param trKey  종목코드
     */
    public void unsubscribe(String trId, String trKey) {
        if (!isConnected.get()) {
            log.warn("WebSocket이 연결되어 있지 않습니다.");
            return;
        }

        sendSubscriptionMessage(trId, trKey, "0");
        Set<String> keys = subscriptions.get(trId);
        if (keys != null) {
            keys.remove(trKey);
        }
        log.info("실시간 구독 해제 - TR_ID: {}, 종목코드: {}", trId, trKey);
    }

    /**
     * 리스너 등록
     */
    public void addListener(KisWebSocketListener listener) {
        listeners.add(listener);
    }

    /**
     * 리스너 해제
     */
    public void removeListener(KisWebSocketListener listener) {
        listeners.remove(listener);
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return isConnected.get() && session != null && session.isOpen();
    }

    // ==================== WebSocketHandler 구현 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 연결 수립됨 - SessionId: {}", session.getId());
        this.session = session;
        isConnected.set(true);

        // 기존 구독 복구
        resubscribeAll();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("WebSocket 메시지 수신: {}", payload);

        try {
            // JSON 형태인지 확인 (구독 응답)
            if (payload.startsWith("{")) {
                handleJsonMessage(payload);
            } else {
                // 실시간 데이터 (파이프 구분)
                handleRealtimeData(payload);
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket 전송 오류", exception);
        isConnected.set(false);
        scheduleReconnect();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결 종료 - 상태: {}", status);
        isConnected.set(false);

        if (status != CloseStatus.NORMAL) {
            scheduleReconnect();
        }
    }

    // ==================== Private Methods ====================

    private void sendSubscriptionMessage(String trId, String trKey, String trType) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket 세션이 유효하지 않습니다.");
            return;
        }

        try {
            Map<String, Object> header = Map.of(
                    "approval_key", tokenManager.getWsApprovalKey(),
                    "custtype", "P",
                    "tr_type", trType,
                    "content-type", "utf-8"
            );

            Map<String, Object> body = Map.of(
                    "input", Map.of(
                            "tr_id", trId,
                            "tr_key", trKey
                    )
            );

            Map<String, Object> message = Map.of(
                    "header", header,
                    "body", body
            );

            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("구독 메시지 전송: {}", json);

        } catch (IOException e) {
            log.error("구독 메시지 전송 실패", e);
        }
    }

    private void handleJsonMessage(String payload) throws JacksonException {
        JsonNode node = objectMapper.readTree(payload);

        // 구독 응답 처리
        if (node.has("header")) {
            String trId = node.path("header").path("tr_id").asText();
            String msgCode = node.path("body").path("msg_cd").asText();
            String msg = node.path("body").path("msg1").asText();

            log.info("구독 응답 - TR_ID: {}, 코드: {}, 메시지: {}", trId, msgCode, msg);

            // 리스너에게 알림
            for (KisWebSocketListener listener : listeners) {
                listener.onSubscriptionResponse(trId, msgCode, msg);
            }
        }
    }

    private void handleRealtimeData(String payload) {
        // 실시간 데이터는 '|' 또는 '^' 로 구분
        // 형식: 암호화여부|TR_ID|데이터건수|데이터
        String[] parts = payload.split("\\|");

        if (parts.length < 4) {
            log.warn("잘못된 실시간 데이터 형식: {}", payload);
            return;
        }

        String encrypted = parts[0];
        String trId = parts[1];
        String dataCount = parts[2];
        String data = parts[3];

        log.debug("실시간 데이터 - 암호화: {}, TR_ID: {}, 건수: {}", encrypted, trId, dataCount);

        // 데이터 파싱 ('^'로 구분된 필드들)
        String[] fields = data.split("\\^");

        // 리스너에게 알림
        for (KisWebSocketListener listener : listeners) {
            listener.onRealtimeData(trId, fields);
        }
    }

    private void resubscribeAll() {
        log.info("기존 구독 복구 시작");
        subscriptions.forEach((trId, keys) -> {
            for (String trKey : keys) {
                sendSubscriptionMessage(trId, trKey, "1");
            }
        });
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log.error("최대 재연결 시도 횟수 초과. 재연결 중단.");
            return;
        }

        reconnectAttempts++;
        log.info("{}초 후 재연결 시도 ({}/{})", RECONNECT_DELAY_SECONDS, reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

        reconnectScheduler.schedule(this::connect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        disconnect();
        reconnectScheduler.shutdown();
    }
}
