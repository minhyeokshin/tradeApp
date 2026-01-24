package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 해외주식 잔고 (보유 종목) DTO
 */
public record OverseasStockBalance(
        @JsonProperty("ovrs_pdno") String symbol,                      // 해외상품번호 (종목코드)
        @JsonProperty("ovrs_cblc_qty") Long quantity,                  // 해외잔고수량
        @JsonProperty("ord_psbl_qty") Long orderableQuantity,          // 주문가능수량
        @JsonProperty("pchs_avg_pric") BigDecimal avgBuyPrice,         // 매입평균가격
        @JsonProperty("now_pric2") BigDecimal currentPrice,            // 현재가격
        @JsonProperty("frcr_evlu_pfls_amt") BigDecimal profitLossAmount, // 외화평가손익금액
        @JsonProperty("evlu_pfls_rt") BigDecimal profitLossRate,       // 평가손익율 (%)
        @JsonProperty("ovrs_stck_evlu_amt") BigDecimal evalAmount,     // 해외주식평가금액
        @JsonProperty("frcr_pchs_amt1") BigDecimal buyAmount,          // 외화매입금액
        @JsonProperty("tr_crcy_cd") String currency,                   // 거래통화코드
        @JsonProperty("ovrs_excg_cd") String exchangeCode              // 해외거래소코드
) {
    /**
     * 수익 여부
     */
    public boolean isProfit() {
        return profitLossAmount != null && profitLossAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 손실 여부
     */
    public boolean isLoss() {
        return profitLossAmount != null && profitLossAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 보유 여부
     */
    public boolean hasPosition() {
        return quantity != null && quantity > 0;
    }
}
