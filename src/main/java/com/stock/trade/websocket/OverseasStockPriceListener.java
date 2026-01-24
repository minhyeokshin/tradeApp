package com.stock.trade.websocket;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * 해외주식 실시간 시세 리스너
 * TR ID: HDFSCNT0 (해외주식 실시간지연체결가)
 */
@Slf4j
public class OverseasStockPriceListener implements KisWebSocketListener {

    // 최신 시세 데이터 저장
    private final ConcurrentMap<String, OverseasRealtimePrice> latestPrices = new ConcurrentHashMap<>();

    // 콜백
    private Consumer<OverseasRealtimePrice> onPriceUpdate;

    /**
     * 시세 업데이트 콜백 설정
     */
    public void setOnPriceUpdate(Consumer<OverseasRealtimePrice> callback) {
        this.onPriceUpdate = callback;
    }

    /**
     * 특정 종목의 최신 시세 조회
     */
    public OverseasRealtimePrice getLatestPrice(String symbol) {
        return latestPrices.get(symbol);
    }

    /**
     * 모든 종목의 최신 시세 조회
     */
    public ConcurrentMap<String, OverseasRealtimePrice> getAllLatestPrices() {
        return latestPrices;
    }

    @Override
    public void onSubscriptionResponse(String trId, String msgCode, String message) {
        if (KisTrId.OVERSEAS_STOCK_DELAYED_CCNL.equals(trId)) {
            log.info("해외주식 실시간 구독 응답 - 코드: {}, 메시지: {}", msgCode, message);
        }
    }

    @Override
    public void onRealtimeData(String trId, String[] fields) {
        if (!KisTrId.OVERSEAS_STOCK_DELAYED_CCNL.equals(trId)) {
            return;
        }

        try {
            OverseasRealtimePrice price = parsePrice(fields);
            latestPrices.put(price.symbol(), price);

            log.info("해외주식 실시간 시세 - 종목: {}, 현재가: ${}, 등락율: {}%, 거래량: {}",
                    price.symbol(), price.currentPrice(), price.changeRate(), price.volume());

            if (onPriceUpdate != null) {
                onPriceUpdate.accept(price);
            }
        } catch (Exception e) {
            log.error("해외주식 실시간 시세 파싱 오류", e);
        }
    }

    private OverseasRealtimePrice parsePrice(String[] fields) {
        // 컬럼 순서:
        // 0:SYMB, 1:ZDIV, 2:TYMD, 3:XYMD, 4:XHMS, 5:KYMD, 6:KHMS,
        // 7:OPEN, 8:HIGH, 9:LOW, 10:LAST, 11:SIGN, 12:DIFF, 13:RATE,
        // 14:PBID, 15:PASK, 16:VBID, 17:VASK, 18:EVOL, 19:TVOL, 20:TAMT,
        // 21:BIVL, 22:ASVL, 23:STRN, 24:MTYP

        return new OverseasRealtimePrice(
                getField(fields, 0),                              // symbol
                parseIntSafe(getField(fields, 1)),               // decimalPlaces
                getField(fields, 3),                              // localDate
                getField(fields, 4),                              // localTime
                getField(fields, 5),                              // koreaDate
                getField(fields, 6),                              // koreaTime
                parseBigDecimalSafe(getField(fields, 7)),        // open
                parseBigDecimalSafe(getField(fields, 8)),        // high
                parseBigDecimalSafe(getField(fields, 9)),        // low
                parseBigDecimalSafe(getField(fields, 10)),       // currentPrice
                getField(fields, 11),                             // changeSign
                parseBigDecimalSafe(getField(fields, 12)),       // change
                parseBigDecimalSafe(getField(fields, 13)),       // changeRate
                parseBigDecimalSafe(getField(fields, 14)),       // bidPrice
                parseBigDecimalSafe(getField(fields, 15)),       // askPrice
                parseLongSafe(getField(fields, 19)),             // volume
                parseBigDecimalSafe(getField(fields, 20)),       // tradingAmount
                parseLongSafe(getField(fields, 18)),             // contractVolume
                parseBigDecimalSafe(getField(fields, 23)),       // contractStrength
                getField(fields, 24)                              // marketType
        );
    }

    private String getField(String[] fields, int index) {
        return (fields != null && index < fields.length) ? fields[index] : "";
    }

    private BigDecimal parseBigDecimalSafe(String value) {
        try {
            return (value != null && !value.isEmpty()) ? new BigDecimal(value) : BigDecimal.ZERO;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private Long parseLongSafe(String value) {
        try {
            return (value != null && !value.isEmpty()) ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Integer parseIntSafe(String value) {
        try {
            return (value != null && !value.isEmpty()) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 해외주식 실시간 시세 데이터
     */
    public record OverseasRealtimePrice(
            String symbol,              // 종목코드
            Integer decimalPlaces,      // 소수점자리수
            String localDate,           // 현지일자
            String localTime,           // 현지시간
            String koreaDate,           // 한국일자
            String koreaTime,           // 한국시간
            BigDecimal open,            // 시가
            BigDecimal high,            // 고가
            BigDecimal low,             // 저가
            BigDecimal currentPrice,    // 현재가
            String changeSign,          // 대비구분 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
            BigDecimal change,          // 전일대비
            BigDecimal changeRate,      // 등락율 (%)
            BigDecimal bidPrice,        // 매수호가
            BigDecimal askPrice,        // 매도호가
            Long volume,                // 거래량
            BigDecimal tradingAmount,   // 거래대금
            Long contractVolume,        // 체결량
            BigDecimal contractStrength,// 체결강도
            String marketType           // 시장구분
    ) {
        public boolean isUp() {
            return "1".equals(changeSign) || "2".equals(changeSign);
        }

        public boolean isDown() {
            return "4".equals(changeSign) || "5".equals(changeSign);
        }
    }
}
