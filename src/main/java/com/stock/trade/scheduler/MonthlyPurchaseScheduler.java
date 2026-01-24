package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.notification.SlackNotificationService;
import com.stock.trade.overseas.*;
import com.stock.trade.scheduler.ScheduledPurchaseProperties.RebalanceConfig;
import com.stock.trade.scheduler.ScheduledPurchaseProperties.StockPurchaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 월간 리밸런싱 스케줄러
 * 매달 첫번째 월요일 미국 장 시작 시간에 실행
 *
 * 로직:
 * 1. 기준 종목(QLD) 잔고 및 수익률 조회
 * 2. 수익률이 목표 이상이면 일부 매도
 * 3. 매도 금액으로 대상 종목(JEPQ) 매수
 */
@Slf4j
@Service
public class MonthlyPurchaseScheduler extends AbstractPurchaseScheduler {

    private final SlackNotificationService slackNotificationService;

    public MonthlyPurchaseScheduler(ScheduledPurchaseProperties properties,
                                    KisProperties kisProperties,
                                    OverseasOrderService orderService,
                                    OverseasStockService stockService,
                                    SlackNotificationService slackNotificationService) {
        super(properties, kisProperties, orderService, stockService);
        this.slackNotificationService = slackNotificationService;
    }

    @Override
    protected String getScheduleType() {
        return "월간 리밸런싱";
    }

    @Override
    protected List<StockPurchaseConfig> getStockConfigs() {
        return properties.getMonthlyStocks();
    }

    /**
     * 매달 첫번째 월요일 오후 11시 30분 (KST) 실행
     */
    @Scheduled(cron = "${scheduler.purchase.monthly-cron:0 30 23 ? * MON#1}", zone = "Asia/Seoul")
    public void execute() {
        List<PurchaseResult> results = executeRebalance();
        slackNotificationService.notifyMonthlyRebalanceResult(results);
    }

    /**
     * 수동 실행 (테스트용)
     */
    public List<PurchaseResult> executeManually() {
        log.info("========== 월간 리밸런싱 수동 실행 ==========");
        return executeRebalance();
    }

    /**
     * 리밸런싱 실행
     */
    private List<PurchaseResult> executeRebalance() {
        if (!properties.isEnabled()) {
            log.debug("정기 매수 스케줄러가 비활성화 상태입니다");
            return List.of();
        }

        RebalanceConfig config = properties.getRebalance();
        if (!config.isEnabled()) {
            log.debug("월간 리밸런싱이 비활성화 상태입니다");
            return List.of();
        }

        log.info("========== {} 스케줄러 시작 ==========", getScheduleType());
        log.info("실행 시각: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        log.info("모드: {}", kisProperties.isDemoMode() ? "모의투자" : "실전투자");
        log.info("기준 종목: {} (수익률 {}% 이상 시 {}% 매도)",
                config.getSourceSymbol(),
                config.getTriggerProfitRate().multiply(BigDecimal.valueOf(100)),
                config.getSellRate().multiply(BigDecimal.valueOf(100)));
        log.info("매수 대상: {}", config.getTargetSymbol());

        orderService.setDemoMode(kisProperties.isDemoMode());
        stockService.setDemoMode(kisProperties.isDemoMode());

        List<PurchaseResult> results = new ArrayList<>();

        try {
            // 1. 기준 종목 잔고 조회
            OverseasStockBalance sourceBalance = stockService.getBalanceBySymbol(config.getSourceSymbol());

            if (sourceBalance == null || !sourceBalance.hasPosition()) {
                log.info("{} 보유 잔고 없음. 리밸런싱 스킵", config.getSourceSymbol());
                results.add(new PurchaseResult(config.getSourceSymbol(), false, null, null, 0,
                        "보유 잔고 없음"));
                logPurchaseSummary(results);
                return results;
            }

            log.info("{} 잔고 현황 - 보유수량: {}주, 평균단가: ${}, 현재가: ${}, 평가손익: ${}, 수익률: {}%",
                    config.getSourceSymbol(),
                    sourceBalance.quantity(),
                    sourceBalance.avgBuyPrice(),
                    sourceBalance.currentPrice(),
                    sourceBalance.profitLossAmount(),
                    sourceBalance.profitLossRate());

            // 2. 수익률 체크 (API는 % 단위로 반환: 25% = 25.00)
            BigDecimal profitRatePercent = sourceBalance.profitLossRate();
            BigDecimal triggerRatePercent = config.getTriggerProfitRate().multiply(BigDecimal.valueOf(100));

            if (profitRatePercent == null || profitRatePercent.compareTo(triggerRatePercent) < 0) {
                log.info("{} 수익률 {}% < 목표 {}%. 리밸런싱 스킵",
                        config.getSourceSymbol(),
                        profitRatePercent != null ? profitRatePercent : "N/A",
                        triggerRatePercent);
                results.add(new PurchaseResult(config.getSourceSymbol(), false, null, null, 0,
                        "수익률 미달 (" + profitRatePercent + "% < " + triggerRatePercent + "%)"));
                logPurchaseSummary(results);
                return results;
            }

            log.info("{} 수익률 {}% >= 목표 {}%. 리밸런싱 진행!",
                    config.getSourceSymbol(), profitRatePercent, triggerRatePercent);

            // 3. 매도 수량 계산 (보유수량의 N%)
            int sellQuantity = BigDecimal.valueOf(sourceBalance.quantity())
                    .multiply(config.getSellRate())
                    .setScale(0, RoundingMode.DOWN)
                    .intValue();

            if (sellQuantity <= 0) {
                log.warn("{} 매도 수량이 0입니다. 보유수량: {}주, 매도비율: {}%",
                        config.getSourceSymbol(), sourceBalance.quantity(),
                        config.getSellRate().multiply(BigDecimal.valueOf(100)));
                results.add(new PurchaseResult(config.getSourceSymbol(), false, null, null, 0,
                        "매도 수량 0 (보유수량 부족)"));
                logPurchaseSummary(results);
                return results;
            }

            // 4. 매도 주문 실행
            PurchaseResult sellResult = executeSell(config, sourceBalance, sellQuantity);
            results.add(sellResult);

            if (!sellResult.success()) {
                log.error("{} 매도 실패. 리밸런싱 중단", config.getSourceSymbol());
                logPurchaseSummary(results);
                return results;
            }

            // 5. 매도 금액으로 대상 종목 매수
            BigDecimal sellAmount = sellResult.price().multiply(BigDecimal.valueOf(sellQuantity));
            log.info("매도 예상 금액: ${}", sellAmount);

            PurchaseResult buyResult = executeBuy(config, sellAmount);
            results.add(buyResult);

        } catch (Exception e) {
            log.error("리밸런싱 실행 중 오류: {}", e.getMessage(), e);
            results.add(new PurchaseResult("REBALANCE", false, null, null, 0, e.getMessage()));
        }

        logPurchaseSummary(results);
        return results;
    }

    /**
     * 기준 종목 매도
     */
    private PurchaseResult executeSell(RebalanceConfig config, OverseasStockBalance balance, int quantity) {
        String symbol = config.getSourceSymbol();
        log.info("----- {} 매도 시작 ({}주) -----", symbol, quantity);

        try {
            OverseasExchange exchange = parseExchange(config.getSourceExchange());
            BigDecimal currentPrice = balance.currentPrice();

            // 현재가 null 체크
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} 현재가 조회 실패", symbol);
                return new PurchaseResult(symbol + "_SELL", false, null, null, quantity,
                        "현재가 조회 실패");
            }

            // 매도가 계산 (할인율 적용 - 매도 시에는 프리미엄 적용)
            BigDecimal premium = currentPrice.multiply(config.getDiscountRate());
            BigDecimal sellPrice = currentPrice.add(premium).setScale(2, RoundingMode.UP);
            log.info("{} 매도가격: ${} (현재가 ${} + {}% 프리미엄)",
                    symbol, sellPrice, currentPrice,
                    config.getDiscountRate().multiply(BigDecimal.valueOf(100)));

            // 매도 주문
            OverseasOrderRequest request = OverseasOrderRequest.builder()
                    .exchange(exchange)
                    .symbol(symbol)
                    .quantity(quantity)
                    .price(sellPrice)
                    .orderType(OverseasOrderType.LIMIT)
                    .build();

            OverseasOrderResult orderResult = orderService.sell(request);

            log.info("{} 매도 주문 성공 - 주문번호: {}, {}주 x ${}",
                    symbol, orderResult.orderNumber(), quantity, sellPrice);

            return new PurchaseResult(symbol + "_SELL", true, orderResult.orderNumber(),
                    sellPrice, quantity, null);

        } catch (Exception e) {
            log.error("{} 매도 실패: {}", symbol, e.getMessage());
            return new PurchaseResult(symbol + "_SELL", false, null, null, quantity, e.getMessage());
        }
    }

    /**
     * 대상 종목 매수
     */
    private PurchaseResult executeBuy(RebalanceConfig config, BigDecimal budget) {
        String symbol = config.getTargetSymbol();
        log.info("----- {} 매수 시작 (예산: ${}) -----", symbol, budget);

        try {
            OverseasExchange exchange = parseExchange(config.getTargetExchange());

            // 현재가 조회
            OverseasStockPrice price = stockService.getPrice(exchange, symbol);
            BigDecimal currentPrice = price.currentPrice();

            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} 현재가 조회 실패", symbol);
                return new PurchaseResult(symbol + "_BUY", false, null, null, 0,
                        "현재가 조회 실패");
            }

            log.info("{} 현재가: ${}", symbol, currentPrice);

            // 매수가 계산 (할인율 적용)
            BigDecimal discount = currentPrice.multiply(config.getDiscountRate());
            BigDecimal buyPrice = currentPrice.subtract(discount).setScale(2, RoundingMode.DOWN);
            log.info("{} 매수가격: ${} (현재가 ${} - {}% 할인)",
                    symbol, buyPrice, currentPrice,
                    config.getDiscountRate().multiply(BigDecimal.valueOf(100)));

            // 매수 수량 계산
            int quantity = budget.divide(buyPrice, 0, RoundingMode.DOWN).intValue();
            log.info("{} 매수 수량: {}주 (${} / ${})", symbol, quantity, budget, buyPrice);

            if (quantity <= 0) {
                log.warn("{} 매수 가능 수량이 0입니다. 예산 부족", symbol);
                return new PurchaseResult(symbol + "_BUY", false, null, buyPrice, 0,
                        "매수 가능 수량 0 (예산: $" + budget + ")");
            }

            // 매수 주문
            OverseasOrderRequest request = OverseasOrderRequest.builder()
                    .exchange(exchange)
                    .symbol(symbol)
                    .quantity(quantity)
                    .price(buyPrice)
                    .orderType(OverseasOrderType.LIMIT)
                    .build();

            OverseasOrderResult orderResult = orderService.buy(request);

            log.info("{} 매수 주문 성공 - 주문번호: {}, {}주 x ${}",
                    symbol, orderResult.orderNumber(), quantity, buyPrice);

            return new PurchaseResult(symbol + "_BUY", true, orderResult.orderNumber(),
                    buyPrice, quantity, null);

        } catch (Exception e) {
            log.error("{} 매수 실패: {}", symbol, e.getMessage());
            return new PurchaseResult(symbol + "_BUY", false, null, null, 0, e.getMessage());
        }
    }
}
