package com.stock.trade.scheduler;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 정기 매수 스케줄러 설정
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scheduler.purchase")
public class ScheduledPurchaseProperties {

    /**
     * 스케줄러 활성화 여부
     */
    private boolean enabled = false;

    /**
     * 주간 매수 종목 목록
     */
    private List<StockPurchaseConfig> stocks = new ArrayList<>();

    /**
     * 월간 매수 종목 목록 (매달 첫번째 월요일)
     */
    private List<StockPurchaseConfig> monthlyStocks = new ArrayList<>();

    /**
     * 기본 환율 (USD/KRW)
     * 실제 환율 조회 실패 시 사용
     */
    private BigDecimal defaultExchangeRate = new BigDecimal("1450");

    /**
     * 월간 리밸런싱 설정
     */
    private RebalanceConfig rebalance = new RebalanceConfig();

    /**
     * 리밸런싱 설정
     */
    @Getter
    @Setter
    public static class RebalanceConfig {
        /**
         * 리밸런싱 활성화 여부
         */
        private boolean enabled = false;

        /**
         * 기준 종목 (수익률 확인 대상)
         */
        private String sourceSymbol = "QLD";

        /**
         * 기준 종목 거래소
         */
        private String sourceExchange = "NASDAQ";

        /**
         * 매수 대상 종목
         */
        private String targetSymbol = "JEPQ";

        /**
         * 매수 대상 거래소
         */
        private String targetExchange = "NASDAQ";

        /**
         * 리밸런싱 트리거 수익률 (예: 0.25 = 25%)
         */
        private BigDecimal triggerProfitRate = new BigDecimal("0.25");

        /**
         * 매도 비율 (예: 0.10 = 10%)
         */
        private BigDecimal sellRate = new BigDecimal("0.10");

        /**
         * 지정가 주문 할인율 (예: 0.02 = 2% 할인)
         */
        private BigDecimal discountRate = new BigDecimal("0.02");
    }

    /**
     * 개별 종목 매수 설정
     */
    @Getter
    @Setter
    public static class StockPurchaseConfig {
        /**
         * 거래소 코드 (NASDAQ, NYSE, AMEX 등)
         */
        private String exchange;

        /**
         * 종목 코드 (예: AAPL, TSLA)
         */
        private String symbol;

        /**
         * 매수 수량 (budgetKrw가 설정되지 않은 경우 사용)
         */
        private int quantity = 0;

        /**
         * 원화 예산 (예: 250000 = 25만원)
         * 설정 시 예산 기준으로 수량 자동 계산
         */
        private BigDecimal budgetKrw;

        /**
         * 주문 방식: LIMIT(지정가), MARKET_ON_CLOSE(장마감시장가)
         */
        private String orderType = "LIMIT";

        /**
         * 지정가 주문 시 현재가 대비 할인율 (예: 0.03 = 3% 할인)
         * LIMIT 주문에서만 사용
         */
        private BigDecimal discountRate = new BigDecimal("0.03");

        /**
         * 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 미체결 시 시장가 전환 여부
         * true: 장마감 1시간 전 미체결 시 취소 후 시장가로 재주문
         */
        private boolean marketFallback = true;

        /**
         * 예산 기반 매수 여부
         */
        public boolean isBudgetBased() {
            return budgetKrw != null && budgetKrw.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
