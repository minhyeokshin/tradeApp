package com.stock.trade.domestic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 국내주식 잔고 (보유 종목) DTO
 * API: /uapi/domestic-stock/v1/trading/inquire-balance
 */
public record DomesticStockBalance(
        @JsonProperty("pdno") String stockCode,                       // 종목코드
        @JsonProperty("prdt_name") String stockName,                  // 종목명
        @JsonProperty("hldg_qty") Long quantity,                      // 보유수량
        @JsonProperty("ord_psbl_qty") Long orderableQuantity,         // 주문가능수량
        @JsonProperty("pchs_avg_pric") BigDecimal avgBuyPrice,        // 매입평균가격
        @JsonProperty("pchs_amt") BigDecimal buyAmount,               // 매입금액
        @JsonProperty("prpr") BigDecimal currentPrice,                // 현재가
        @JsonProperty("evlu_amt") BigDecimal evalAmount,              // 평가금액
        @JsonProperty("evlu_pfls_amt") BigDecimal profitLossAmount,   // 평가손익금액
        @JsonProperty("evlu_pfls_rt") BigDecimal profitLossRate,      // 평가손익율 (%)
        @JsonProperty("evlu_erng_rt") BigDecimal returnRate,          // 수익률 (%)
        @JsonProperty("loan_dt") String loanDate,                     // 대출일자
        @JsonProperty("loan_amt") BigDecimal loanAmount,              // 대출금액
        @JsonProperty("stln_slng_chgs") BigDecimal stockLoanCharge,   // 대주매각대금
        @JsonProperty("expd_dt") String expirationDate                // 만기일자
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
