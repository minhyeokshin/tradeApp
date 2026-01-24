package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 해외주식 미체결 주문 DTO
 */
public record OverseasUnfilledOrder(
        @JsonProperty("odno") String orderNumber,              // 주문번호
        @JsonProperty("orgn_odno") String originalOrderNumber, // 원주문번호
        @JsonProperty("pdno") String symbol,                   // 종목코드
        @JsonProperty("prdt_name") String productName,         // 상품명
        @JsonProperty("sll_buy_dvsn_cd") String sellBuyCode,   // 매도매수구분 (01:매도, 02:매수)
        @JsonProperty("ord_qty") Long orderQuantity,           // 주문수량
        @JsonProperty("ft_ccld_qty") Long filledQuantity,      // 체결수량
        @JsonProperty("nccs_qty") Long unfilledQuantity,       // 미체결수량
        @JsonProperty("ft_ord_unpr3") BigDecimal orderPrice,   // 주문단가
        @JsonProperty("ovrs_excg_cd") String exchangeCode,     // 해외거래소코드
        @JsonProperty("ord_dt") String orderDate,              // 주문일자
        @JsonProperty("ord_tmd") String orderTime              // 주문시각
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
