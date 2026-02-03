package com.stock.trade.domestic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 국내주식 매수가능금액 조회 결과 DTO
 * API: /uapi/domestic-stock/v1/trading/inquire-psbl-order (매수가능조회)
 */
public record DomesticPurchasableAmount(
        @JsonProperty("ord_psbl_cash") BigDecimal orderableCash,       // 주문가능현금
        @JsonProperty("ord_psbl_sbst") BigDecimal orderableSubstitute, // 주문가능대용
        @JsonProperty("ruse_psbl_amt") BigDecimal reuseAmount,         // 재사용가능금액
        @JsonProperty("fund_rpch_chgs") BigDecimal fundRepurchase,     // 펀드환매대금
        @JsonProperty("psbl_qty_calc_unpr") BigDecimal calcPrice,      // 가능수량계산단가
        @JsonProperty("nrcvb_buy_amt") BigDecimal notReceivableAmt,    // 미수없는매수금액
        @JsonProperty("nrcvb_buy_qty") Long notReceivableQty,          // 미수없는매수수량
        @JsonProperty("max_buy_amt") BigDecimal maxBuyAmount,          // 최대매수금액
        @JsonProperty("max_buy_qty") Long maxBuyQuantity,              // 최대매수수량
        @JsonProperty("cma_evlu_amt") BigDecimal cmaEvalAmount,        // CMA평가금액
        @JsonProperty("ovrs_re_use_amt_wcrc") BigDecimal overseasReuseAmount, // 해외재사용금액(원화)
        @JsonProperty("ord_psbl_frcr_amt_wcrc") BigDecimal orderableForeignAmount // 주문가능외화금액(원화)
) {
    /**
     * 매수 가능 여부
     */
    public boolean canPurchase() {
        return maxBuyQuantity != null && maxBuyQuantity > 0;
    }
}
