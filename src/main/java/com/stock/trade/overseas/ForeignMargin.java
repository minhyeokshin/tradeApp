package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 해외증거금 통화별 조회 결과 DTO
 */
public record ForeignMargin(
        @JsonProperty("crcy_cd") String currencyCode,           // 통화코드 (KRW, USD 등)
        @JsonProperty("frcr_dncl_amt") BigDecimal depositAmount, // 외화예수금
        @JsonProperty("frcr_drwg_psbl_amt") BigDecimal withdrawableAmount, // 외화출금가능금액
        @JsonProperty("frcr_evlu_amt") BigDecimal evalAmount,    // 외화평가금액
        @JsonProperty("exrt") BigDecimal exchangeRate            // 환율
) {
}
