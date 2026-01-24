package com.stock.trade.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 실시간 주식 체결가 리스너
 * TR ID: H0UNCNT0 (실시간 체결가 통합)
 */
@Slf4j
@Component
public class StockPriceListener implements KisWebSocketListener {

    @Override
    public void onSubscriptionResponse(String trId, String msgCode, String message) {
        log.info("[구독 응답] TR_ID: {}, 코드: {}, 메시지: {}", trId, msgCode, message);
    }

    @Override
    public void onRealtimeData(String trId, String[] fields) {
        // 실시간 체결가 데이터만 처리
        if (!KisTrId.STOCK_CCNL_TOTAL.equals(trId) &&
            !KisTrId.STOCK_CCNL_KRX.equals(trId)) {
            return;
        }

        if (fields.length < 15) {
            log.warn("체결가 데이터 필드 부족: {}", fields.length);
            return;
        }

        StockPrice price = parseStockPrice(fields);
        log.info("[실시간 체결] 종목: {}, 현재가: {}, 등락률: {}%, 거래량: {}",
                price.stockCode(), price.currentPrice(), price.changeRate(), price.volume());

        // TODO: 실제 비즈니스 로직 구현
        // - 가격 알림
        // - 데이터베이스 저장
        // - 다른 서비스로 이벤트 발행 등
    }

    @Override
    public void onError(Throwable error) {
        log.error("[체결가 리스너 오류]", error);
    }

    private StockPrice parseStockPrice(String[] fields) {
        return new StockPrice(
                fields[0],  // 종목코드
                fields[1],  // 체결시간
                parseLong(fields[2]),   // 현재가
                fields[3],  // 전일대비부호
                parseLong(fields[4]),   // 전일대비
                parseDouble(fields[5]), // 전일대비율
                parseLong(fields[7]),   // 시가
                parseLong(fields[8]),   // 고가
                parseLong(fields[9]),   // 저가
                parseLong(fields[12]),  // 체결량
                parseLong(fields[13]),  // 누적거래량
                parseLong(fields[14])   // 누적거래대금
        );
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 실시간 체결가 데이터
     */
    public record StockPrice(
            String stockCode,       // 종목코드
            String tradeTime,       // 체결시간 (HHMMSS)
            long currentPrice,      // 현재가
            String changeSign,      // 전일대비부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
            long priceChange,       // 전일대비
            double changeRate,      // 전일대비율 (%)
            long openPrice,         // 시가
            long highPrice,         // 고가
            long lowPrice,          // 저가
            long volume,            // 체결량
            long accumulatedVolume, // 누적거래량
            long accumulatedAmount  // 누적거래대금
    ) {
    }
}
