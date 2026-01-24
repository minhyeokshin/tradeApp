package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 해외주식 매수가능금액 조회 결과 DTO
 */
public record OverseasPurchasableAmount(
        @JsonProperty("tr_crcy_cd") String currencyCode,           // 거래통화코드
        @JsonProperty("ord_psbl_frcr_amt") BigDecimal availableAmount, // 주문가능외화금액
        @JsonProperty("frcr_ord_psbl_amt1") BigDecimal maxOrderAmount, // 외화주문가능금액
        @JsonProperty("ovrs_ord_psbl_qty") Long purchasableQuantity,   // 해외주문가능수량
        @JsonProperty("exrt") BigDecimal exchangeRate,              // 환율
        @JsonProperty("frcr_evlu_amt2") BigDecimal evalAmount       // 외화평가금액
) {
    /**
     * 매수 가능 여부
     */
    public boolean canPurchase() {
        return purchasableQuantity != null && purchasableQuantity > 0;
    }
}
