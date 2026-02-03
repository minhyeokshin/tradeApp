package com.stock.trade.domestic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 국내주식 주문 결과 DTO
 */
public record DomesticOrderResult(
        @JsonProperty("KRX_FWDG_ORD_ORGNO") String orderOrgNo,  // 한국거래소전송주문조직번호
        @JsonProperty("ODNO") String orderNumber,               // 주문번호
        @JsonProperty("ORD_TMD") String orderTime               // 주문시각
) {
    /**
     * 주문 성공 여부
     */
    public boolean isSuccess() {
        return orderNumber != null && !orderNumber.isEmpty();
    }
}
