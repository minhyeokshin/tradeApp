package com.stock.trade.websocket;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KIS 실시간 데이터 서비스
 * WebSocket 클라이언트를 래핑하여 편리한 API 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisRealtimeService {

    private final KisWebSocketClient webSocketClient;
    private final List<KisWebSocketListener> listeners;

    @PostConstruct
    public void init() {
        // 모든 리스너 등록
        listeners.forEach(webSocketClient::addListener);
        log.info("KIS 실시간 서비스 초기화 완료. 등록된 리스너: {}개", listeners.size());
    }

    /**
     * 실시간 서비스 시작 (WebSocket 연결)
     */
    public void start() {
        webSocketClient.connect();
    }

    /**
     * 실시간 서비스 종료 (WebSocket 연결 해제)
     */
    public void stop() {
        webSocketClient.disconnect();
    }

    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return webSocketClient.isConnected();
    }

    // ==================== 실시간 체결가 ====================

    /**
     * 실시간 체결가 구독 (통합)
     *
     * @param stockCode 종목코드 (예: 005930)
     */
    public void subscribePrice(String stockCode) {
        webSocketClient.subscribe(KisTrId.STOCK_CCNL_TOTAL, stockCode);
    }

    /**
     * 실시간 체결가 구독 해제
     *
     * @param stockCode 종목코드
     */
    public void unsubscribePrice(String stockCode) {
        webSocketClient.unsubscribe(KisTrId.STOCK_CCNL_TOTAL, stockCode);
    }

    // ==================== 실시간 호가 ====================

    /**
     * 실시간 호가 구독 (통합)
     *
     * @param stockCode 종목코드
     */
    public void subscribeOrderbook(String stockCode) {
        webSocketClient.subscribe(KisTrId.STOCK_ASKING_PRICE_TOTAL, stockCode);
    }

    /**
     * 실시간 호가 구독 해제
     *
     * @param stockCode 종목코드
     */
    public void unsubscribeOrderbook(String stockCode) {
        webSocketClient.unsubscribe(KisTrId.STOCK_ASKING_PRICE_TOTAL, stockCode);
    }

    // ==================== 실시간 체결통보 ====================

    /**
     * 실시간 체결통보 구독 (주문 체결 알림)
     *
     * @param htsId HTS ID (사용자 ID)
     * @param isDemo 모의투자 여부
     */
    public void subscribeOrderNotice(String htsId, boolean isDemo) {
        String trId = isDemo ? KisTrId.STOCK_CCNL_NOTICE_DEMO : KisTrId.STOCK_CCNL_NOTICE;
        webSocketClient.subscribe(trId, htsId);
    }

    /**
     * 실시간 체결통보 구독 해제
     *
     * @param htsId HTS ID
     * @param isDemo 모의투자 여부
     */
    public void unsubscribeOrderNotice(String htsId, boolean isDemo) {
        String trId = isDemo ? KisTrId.STOCK_CCNL_NOTICE_DEMO : KisTrId.STOCK_CCNL_NOTICE;
        webSocketClient.unsubscribe(trId, htsId);
    }

    // ==================== 지수 ====================

    /**
     * 지수 실시간체결 구독
     *
     * @param indexCode 지수코드 (예: 0001 - 코스피)
     */
    public void subscribeIndex(String indexCode) {
        webSocketClient.subscribe(KisTrId.INDEX_CCNL, indexCode);
    }

    /**
     * 지수 실시간체결 구독 해제
     *
     * @param indexCode 지수코드
     */
    public void unsubscribeIndex(String indexCode) {
        webSocketClient.unsubscribe(KisTrId.INDEX_CCNL, indexCode);
    }

    // ==================== 범용 구독 ====================

    /**
     * 범용 구독 메서드
     *
     * @param trId  TR ID (KisTrId 상수 참조)
     * @param trKey 종목코드 또는 키
     */
    public void subscribe(String trId, String trKey) {
        webSocketClient.subscribe(trId, trKey);
    }

    /**
     * 범용 구독 해제 메서드
     *
     * @param trId  TR ID
     * @param trKey 종목코드 또는 키
     */
    public void unsubscribe(String trId, String trKey) {
        webSocketClient.unsubscribe(trId, trKey);
    }
}
