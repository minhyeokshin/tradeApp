package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 해외주식 현재체결가 응답 DTO
 * API: /uapi/overseas-price/v1/quotations/price
 * TR ID: HHDFS00000300
 */
public record OverseasStockPrice(
        @JsonProperty("rsym") String realtimeSymbol,           // 실시간조회종목코드
        @JsonProperty("zdiv") Integer decimalPlaces,           // 소수점자리수
        @JsonProperty("base") BigDecimal previousClose,        // 전일종가
        @JsonProperty("pvol") Long previousVolume,             // 전일거래량
        @JsonProperty("last") BigDecimal currentPrice,         // 현재가
        @JsonProperty("sign") String changeSign,               // 대비기호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
        @JsonProperty("diff") BigDecimal change,               // 대비
        @JsonProperty("rate") BigDecimal changeRate,           // 등락율 (%)
        @JsonProperty("tvol") Long volume,                     // 거래량
        @JsonProperty("tamt") BigDecimal tradingAmount,        // 거래대금
        @JsonProperty("ordy") String tradable                  // 매수가능여부 (Y/N)
) {
    /**
     * 상승 여부
     */
    public boolean isUp() {
        return "1".equals(changeSign) || "2".equals(changeSign);
    }

    /**
     * 하락 여부
     */
    public boolean isDown() {
        return "4".equals(changeSign) || "5".equals(changeSign);
    }

    /**
     * 보합 여부
     */
    public boolean isFlat() {
        return "3".equals(changeSign);
    }

    /**
     * 매수 가능 여부
     */
    public boolean isTradable() {
        return "Y".equals(tradable);
    }
}
