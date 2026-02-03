package com.stock.trade.domestic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 국내주식 현재가 응답 DTO
 * API: /uapi/domestic-stock/v1/quotations/inquire-price
 * TR ID: FHKST01010100
 */
public record DomesticStockPrice(
        @JsonProperty("stck_prpr") BigDecimal currentPrice,           // 주식 현재가
        @JsonProperty("prdy_vrss") BigDecimal change,                 // 전일 대비
        @JsonProperty("prdy_vrss_sign") String changeSign,            // 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
        @JsonProperty("prdy_ctrt") BigDecimal changeRate,             // 전일 대비율 (%)
        @JsonProperty("acml_vol") Long volume,                        // 누적 거래량
        @JsonProperty("acml_tr_pbmn") BigDecimal tradingAmount,       // 누적 거래대금
        @JsonProperty("stck_oprc") BigDecimal openPrice,              // 시가
        @JsonProperty("stck_hgpr") BigDecimal highPrice,              // 고가
        @JsonProperty("stck_lwpr") BigDecimal lowPrice,               // 저가
        @JsonProperty("stck_mxpr") BigDecimal upperLimitPrice,        // 상한가
        @JsonProperty("stck_llam") BigDecimal lowerLimitPrice,        // 하한가
        @JsonProperty("per") BigDecimal per,                          // PER
        @JsonProperty("pbr") BigDecimal pbr,                          // PBR
        @JsonProperty("hts_avls") BigDecimal marketCap,               // 시가총액 (HTS)
        @JsonProperty("w52_hgpr") BigDecimal week52High,              // 52주 최고가
        @JsonProperty("w52_lwpr") BigDecimal week52Low                // 52주 최저가
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
}
