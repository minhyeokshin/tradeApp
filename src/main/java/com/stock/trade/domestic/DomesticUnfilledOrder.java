package com.stock.trade.domestic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 국내주식 미체결 주문 DTO
 * API: /uapi/domestic-stock/v1/trading/inquire-psbl-order
 */
public record DomesticUnfilledOrder(
        @JsonProperty("odno") String orderNumber,              // 주문번호
        @JsonProperty("orgn_odno") String originalOrderNumber, // 원주문번호
        @JsonProperty("pdno") String stockCode,                // 종목코드
        @JsonProperty("prdt_name") String stockName,           // 종목명
        @JsonProperty("sll_buy_dvsn_cd") String sellBuyCode,   // 매도매수구분 (01:매도, 02:매수)
        @JsonProperty("sll_buy_dvsn_cd_name") String sellBuyName, // 매도매수구분명
        @JsonProperty("ord_qty") Long orderQuantity,           // 주문수량
        @JsonProperty("ord_unpr") BigDecimal orderPrice,       // 주문단가
        @JsonProperty("tot_ccld_qty") Long filledQuantity,     // 총체결수량
        @JsonProperty("rmn_qty") Long unfilledQuantity,        // 잔여수량 (미체결수량)
        @JsonProperty("tot_ccld_amt") BigDecimal filledAmount, // 총체결금액
        @JsonProperty("ord_dt") String orderDate,              // 주문일자
        @JsonProperty("ord_tmd") String orderTime,             // 주문시각
        @JsonProperty("ord_dvsn_cd") String orderTypeCode,     // 주문구분코드
        @JsonProperty("ord_dvsn_name") String orderTypeName    // 주문구분명
) {
    /**
     * 매수 주문 여부
     */
    public boolean isBuyOrder() {
        return "02".equals(sellBuyCode);
    }

    /**
     * 매도 주문 여부
     */
    public boolean isSellOrder() {
        return "01".equals(sellBuyCode);
    }

    /**
     * 미체결 잔량이 있는지 여부
     */
    public boolean hasUnfilledQuantity() {
        return unfilledQuantity != null && unfilledQuantity > 0;
    }
}
