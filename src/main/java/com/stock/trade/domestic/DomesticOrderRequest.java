package com.stock.trade.domestic;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 국내주식 주문 요청 DTO
 */
@Getter
@Builder
public class DomesticOrderRequest {

    /**
     * 종목코드 (필수, 예: 005930)
     */
    private final String stockCode;

    /**
     * 주문수량 (필수)
     */
    private final int quantity;

    /**
     * 주문단가 (지정가 주문 시 필수, 시장가는 0)
     */
    @Builder.Default
    private final BigDecimal price = BigDecimal.ZERO;

    /**
     * 주문유형 (기본값: 지정가)
     */
    @Builder.Default
    private final DomesticOrderType orderType = DomesticOrderType.LIMIT;

    /**
     * 유효성 검증
     */
    public void validate() {
        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문수량은 1 이상이어야 합니다");
        }
        if (orderType == DomesticOrderType.LIMIT && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("지정가 주문 시 주문단가는 필수입니다");
        }
    }

    /**
     * 간편 매수 주문 생성 (지정가)
     */
    public static DomesticOrderRequest buyLimit(String stockCode, int quantity, BigDecimal price) {
        return DomesticOrderRequest.builder()
                .stockCode(stockCode)
                .quantity(quantity)
                .price(price)
                .orderType(DomesticOrderType.LIMIT)
                .build();
    }

    /**
     * 간편 매도 주문 생성 (지정가)
     */
    public static DomesticOrderRequest sellLimit(String stockCode, int quantity, BigDecimal price) {
        return DomesticOrderRequest.builder()
                .stockCode(stockCode)
                .quantity(quantity)
                .price(price)
                .orderType(DomesticOrderType.LIMIT)
                .build();
    }

    /**
     * 간편 매수 주문 생성 (시장가)
     */
    public static DomesticOrderRequest buyMarket(String stockCode, int quantity) {
        return DomesticOrderRequest.builder()
                .stockCode(stockCode)
                .quantity(quantity)
                .price(BigDecimal.ZERO)
                .orderType(DomesticOrderType.MARKET)
                .build();
    }

    /**
     * 간편 매도 주문 생성 (시장가)
     */
    public static DomesticOrderRequest sellMarket(String stockCode, int quantity) {
        return DomesticOrderRequest.builder()
                .stockCode(stockCode)
                .quantity(quantity)
                .price(BigDecimal.ZERO)
                .orderType(DomesticOrderType.MARKET)
                .build();
    }
}
