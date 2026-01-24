package com.stock.trade.websocket;

/**
 * KIS WebSocket 실시간 데이터 리스너 인터페이스
 */
public interface KisWebSocketListener {

    /**
     * 구독 응답 수신 시 호출
     *
     * @param trId    TR ID
     * @param msgCode 응답 코드
     * @param message 응답 메시지
     */
    default void onSubscriptionResponse(String trId, String msgCode, String message) {
    }

    /**
     * 실시간 데이터 수신 시 호출
     *
     * @param trId   TR ID
     * @param fields 데이터 필드 배열 ('^' 구분자로 분리된 값들)
     */
    void onRealtimeData(String trId, String[] fields);

    /**
     * 연결 종료 시 호출
     */
    default void onDisconnected() {
    }

    /**
     * 오류 발생 시 호출
     *
     * @param error 오류 정보
     */
    default void onError(Throwable error) {
    }
}
