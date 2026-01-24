package com.stock.trade.overseas;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        properties = {
                "external.kis.app-key=PS5XcF36fhCCfcKiAKgiW36TQ13khqQbH46E",
                "external.kis.app-secret=/09BAcePrZyaA/PfQk+1bepz5VVraE512c2c7Srce+p8Xn1ipKSBDTTTqfsXgXS2TY3sMyFrEfLwvrISgEsNb3NPDU/Rv++smhTresoyQuLJmPzNT1+zTBA9Bp7pR3gk6oN1e6iczlmXe2TWYP1imjJabT/5/Q/GHhrfTk4U6xpyS2INh2g=",
                "external.kis.base-url=https://openapivts.koreainvestment.com:29443",  // 모의투자 서버
                "external.kis.account-number=50160641",
                "external.kis.account-product-code=01"
        }
)
class OverseasOrderServiceTest {

    @Autowired
    private OverseasOrderService orderService;

    @Autowired
    private OverseasStockService stockService;

    @BeforeEach
    void setUp() {
        // 모의투자 모드 설정
        orderService.setDemoMode(true);
    }

    @Test
    @DisplayName("모의투자 - AAPL 매수 주문 테스트")
    void buyOrder_AAPL() {
        // given
        OverseasOrderRequest request = OverseasOrderRequest.builder()
                .exchange(OverseasExchange.NASDAQ)
                .symbol("AAPL")
                .quantity(1)
                .price(new BigDecimal("150.00"))
                .orderType(OverseasOrderType.LIMIT)
                .build();

        log.info("===== AAPL 매수 주문 테스트 (모의투자) =====");
        log.info("거래소: {}", request.getExchange());
        log.info("종목: {}", request.getSymbol());
        log.info("수량: {}", request.getQuantity());
        log.info("가격: ${}", request.getPrice());
        log.info("주문유형: {}", request.getOrderType().getDescription());

        // when
        OverseasOrderResult result = orderService.buy(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isNotNull();

        log.info("===== 주문 결과 =====");
        log.info("주문번호: {}", result.orderNumber());
        log.info("주문시각: {}", result.orderTime());
        log.info("조직번호: {}", result.orderOrgNo());
    }

    @Test
    @DisplayName("모의투자 - AAPL 매수 주문 (간편 버전)")
    void buyOrder_AAPL_simple() {
        log.info("===== AAPL 매수 주문 (간편 버전) =====");

        // when
        OverseasOrderResult result = orderService.buy(
                OverseasExchange.NASDAQ,
                "AAPL",
                1,
                new BigDecimal("145.00")
        );

        // then
        assertThat(result).isNotNull();
        log.info("주문번호: {}", result.orderNumber());
        log.info("주문시각: {}", result.orderTime());
    }

    @Test
    @DisplayName("모의투자 - TSLA 매수 주문 테스트")
    void buyOrder_TSLA() {
        log.info("===== TSLA 매수 주문 테스트 =====");

        // when
        OverseasOrderResult result = orderService.buy(
                OverseasExchange.NASDAQ,
                "TSLA",
                1,
                new BigDecimal("250.00")
        );

        // then
        assertThat(result).isNotNull();
        log.info("주문번호: {}", result.orderNumber());
        log.info("주문시각: {}", result.orderTime());
    }

    @Test
    @DisplayName("모의투자 - 현재가 조회 후 매수 주문")
    void buyOrder_withCurrentPrice() {
        log.info("===== 현재가 조회 후 매수 주문 =====");

        // 1. 현재가 조회
        OverseasStockPrice price = stockService.getPrice(OverseasExchange.NASDAQ, "NVDA");
        log.info("NVDA 현재가: ${}", price.currentPrice());

        // 2. 현재가 기준 -5% 가격으로 지정가 매수
        BigDecimal orderPrice = price.currentPrice()
                .multiply(new BigDecimal("0.95"))
                .setScale(2, BigDecimal.ROUND_DOWN);
        log.info("주문가격 (현재가 -5%): ${}", orderPrice);

        // 3. 매수 주문
        OverseasOrderResult result = orderService.buy(
                OverseasExchange.NASDAQ,
                "NVDA",
                1,
                orderPrice
        );

        // then
        assertThat(result).isNotNull();
        log.info("주문번호: {}", result.orderNumber());
    }
}
