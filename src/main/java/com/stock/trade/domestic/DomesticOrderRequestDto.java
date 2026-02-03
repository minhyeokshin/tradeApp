package com.stock.trade.domestic;

import lombok.*;

import java.math.BigDecimal;

/**
 * 국내주식 주문 요청 DTO (API용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomesticOrderRequestDto {
    private String stockCode;
    private int quantity;
    private BigDecimal price;
    private DomesticOrderType orderType;
}
