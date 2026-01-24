package com.stock.trade.overseas;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 해외주식 주문 요청 DTO
 */
@Getter
@Builder
public class OverseasOrderRequest {

    /**
     * 거래소 (필수)
     */
    private final OverseasExchange exchange;

    /**
     * 종목코드 (필수, 예: AAPL)
     */
    private final String symbol;

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
    private final OverseasOrderType orderType = OverseasOrderType.LIMIT;

    /**
     * 유효성 검증
     */
    public void validate() {
        if (exchange == null) {
            throw new IllegalArgumentException("거래소는 필수입니다");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("종목코드는 필수입니다");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문수량은 1 이상이어야 합니다");
        }
        if (orderType == OverseasOrderType.LIMIT && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("지정가 주문 시 주문단가는 필수입니다");
        }
    }

    /**
     * 간편 매수 주문 생성 (지정가)
     */
    public static OverseasOrderRequest buyLimit(OverseasExchange exchange, String symbol, int quantity, BigDecimal price) {
        return OverseasOrderRequest.builder()
                .exchange(exchange)
                .symbol(symbol)
                .quantity(quantity)
                .price(price)
                .orderType(OverseasOrderType.LIMIT)
                .build();
    }

    /**
     * 간편 매도 주문 생성 (지정가)
     */
    public static OverseasOrderRequest sellLimit(OverseasExchange exchange, String symbol, int quantity, BigDecimal price) {
        return OverseasOrderRequest.builder()
                .exchange(exchange)
                .symbol(symbol)
                .quantity(quantity)
                .price(price)
                .orderType(OverseasOrderType.LIMIT)
                .build();
    }

    /**
     * 간편 매도 주문 생성 (시장가 - 장마감시장가 MOC)
     */
    public static OverseasOrderRequest sellMarket(OverseasExchange exchange, String symbol, int quantity) {
        return OverseasOrderRequest.builder()
                .exchange(exchange)
                .symbol(symbol)
                .quantity(quantity)
                .price(BigDecimal.ZERO)
                .orderType(OverseasOrderType.MARKET_ON_CLOSE)
                .build();
    }
}
