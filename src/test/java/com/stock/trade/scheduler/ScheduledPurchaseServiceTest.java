package com.stock.trade.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 정기 매수 스케줄러 테스트
 * 모의투자 서버에서 테스트합니다.
 */
@Slf4j
@SpringBootTest(
        properties = {
                // 모의투자 서버 설정
                "external.kis.demo-mode=true",
                // 모의투자 인증 정보
                "external.kis.demo-app-key=PS5XcF36fhCCfcKiAKgiW36TQ13khqQbH46E",
                "external.kis.demo-app-secret=/09BAcePrZyaA/PfQk+1bepz5VVraE512c2c7Srce+p8Xn1ipKSBDTTTqfsXgXS2TY3sMyFrEfLwvrISgEsNb3NPDU/Rv++smhTresoyQuLJmPzNT1+zTBA9Bp7pR3gk6oN1e6iczlmXe2TWYP1imjJabT/5/Q/GHhrfTk4U6xpyS2INh2g=",
                "external.kis.demo-account-number=50160641",
                "external.kis.demo-account-product-code=01",
                // 스케줄러 설정
                "scheduler.purchase.enabled=true",
                // 기본 환율
                "scheduler.purchase.default-exchange-rate=1450",
                // 주간 매수 종목 (예산 기반)
                "scheduler.purchase.stocks[0].exchange=NASDAQ",
                "scheduler.purchase.stocks[0].symbol=QLD",
                "scheduler.purchase.stocks[0].budget-krw=250000",
                "scheduler.purchase.stocks[0].order-type=LIMIT",
                "scheduler.purchase.stocks[0].discount-rate=0.02",
                "scheduler.purchase.stocks[0].enabled=true",
                // 월간 리밸런싱 설정
                "scheduler.purchase.rebalance.enabled=true",
                "scheduler.purchase.rebalance.source-symbol=QLD",
                "scheduler.purchase.rebalance.source-exchange=NASDAQ",
                "scheduler.purchase.rebalance.target-symbol=JEPQ",
                "scheduler.purchase.rebalance.target-exchange=NASDAQ",
                "scheduler.purchase.rebalance.trigger-profit-rate=0.25",
                "scheduler.purchase.rebalance.sell-rate=0.10",
                "scheduler.purchase.rebalance.discount-rate=0.02"
        }
)
class ScheduledPurchaseServiceTest {

    @Autowired
    private ScheduledPurchaseService scheduledPurchaseService;

    @Autowired
    private ScheduledPurchaseProperties properties;

    @Autowired
    private com.stock.trade.config.KisProperties kisProperties;

    @Test
    @DisplayName("스케줄러 설정 확인")
    void checkConfiguration() {
        log.info("===== 스케줄러 설정 확인 =====");
        log.info("활성화: {}", properties.isEnabled());
        log.info("모의투자 모드: {}", kisProperties.isDemoMode());

        log.info("주간 매수 종목 수: {}", properties.getStocks().size());
        for (ScheduledPurchaseProperties.StockPurchaseConfig stock : properties.getStocks()) {
            log.info("  [주간] {} {} 예산: {}원 (할인율: {}%)",
                    stock.getExchange(),
                    stock.getSymbol(),
                    stock.getBudgetKrw(),
                    stock.getDiscountRate().multiply(java.math.BigDecimal.valueOf(100)));
        }

        ScheduledPurchaseProperties.RebalanceConfig rebalance = properties.getRebalance();
        log.info("월간 리밸런싱 설정:");
        log.info("  활성화: {}", rebalance.isEnabled());
        log.info("  기준 종목: {} ({})", rebalance.getSourceSymbol(), rebalance.getSourceExchange());
        log.info("  대상 종목: {} ({})", rebalance.getTargetSymbol(), rebalance.getTargetExchange());
        log.info("  트리거 수익률: {}%", rebalance.getTriggerProfitRate().multiply(java.math.BigDecimal.valueOf(100)));
        log.info("  매도 비율: {}%", rebalance.getSellRate().multiply(java.math.BigDecimal.valueOf(100)));

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getStocks()).isNotEmpty();
        assertThat(rebalance.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("주간 매수 수동 실행 테스트 (모의투자)")
    void executeWeeklyManually() {
        log.info("===== 주간 매수 수동 실행 테스트 =====");

        // when
        List<AbstractPurchaseScheduler.PurchaseResult> results = scheduledPurchaseService.executeWeeklyManually();

        // then
        assertThat(results).isNotEmpty();
        logResults(results);
    }

    @Test
    @DisplayName("월간 리밸런싱 수동 실행 테스트 (모의투자)")
    void executeMonthlyManually() {
        log.info("===== 월간 리밸런싱 수동 실행 테스트 =====");
        log.info("QLD 수익률 25% 이상 시 10% 매도 → JEPQ 매수");

        // when
        List<AbstractPurchaseScheduler.PurchaseResult> results = scheduledPurchaseService.executeMonthlyManually();

        // then
        assertThat(results).isNotEmpty();
        logResults(results);
    }

    private void logResults(List<AbstractPurchaseScheduler.PurchaseResult> results) {
        for (AbstractPurchaseScheduler.PurchaseResult result : results) {
            log.info("결과: {} - 성공: {}, 주문번호: {}, 가격: ${}, 수량: {}주, 에러: {}",
                    result.symbol(),
                    result.success(),
                    result.orderNumber(),
                    result.price(),
                    result.quantity(),
                    result.errorMessage());
        }
    }
}
