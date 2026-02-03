package com.stock.trade.domestic;

import lombok.*;

/**
 * 국내주식 주문 취소 요청 DTO (API용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomesticCancelRequestDto {
    private String orderNumber;
    private Integer quantity;  // null이면 전량 취소
}
