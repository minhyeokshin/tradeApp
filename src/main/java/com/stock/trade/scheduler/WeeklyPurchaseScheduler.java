package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.overseas.*;
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
 * 주간 정기 매수 스케줄러
 * 매주 월요일 미국 장 시작 시간에 실행
 *
 * 로직:
 * 1. 현재 계좌 달러 매수가능 잔액 조회
 * 2. 원화 예산 기준으로 몇 주 매수 가능한지 계산
 * 3. 가능한 수량만큼 매수
 */
@Slf4j
@Service
public class WeeklyPurchaseScheduler extends AbstractPurchaseScheduler {

    public WeeklyPurchaseScheduler(ScheduledPurchaseProperties properties,
                                   KisProperties kisProperties,
                                   OverseasOrderService orderService,
                                   OverseasStockService stockService) {
        super(properties, kisProperties, orderService, stockService);
    }

    @Override
    protected String getScheduleType() {
        return "주간";
    }

    @Override
    protected List<StockPurchaseConfig> getStockConfigs() {
        return properties.getStocks();
    }

    /**
     * 매주 월요일 오후 11시 30분 (KST) 실행
     */
    @Scheduled(cron = "${scheduler.purchase.weekly-cron:0 30 23 * * MON}", zone = "Asia/Seoul")
    public void execute() {
        executePurchase();
    }

    /**
     * 수동 실행 (테스트용)
     */
    public List<PurchaseResult> executeManually() {
        log.info("========== 주간 매수 수동 실행 ==========");
        return executePurchase();
    }

    @Override
    protected List<PurchaseResult> executePurchase() {
        if (!properties.isEnabled()) {
            log.debug("정기 매수 스케줄러가 비활성화 상태입니다");
            return List.of();
        }

        List<StockPurchaseConfig> stocks = getStockConfigs();
        if (stocks.isEmpty()) {
            log.debug("{} 매수 종목이 없습니다", getScheduleType());
            return List.of();
        }

        log.info("========== {} 정기 매수 스케줄러 시작 ==========", getScheduleType());
        log.info("실행 시각: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        log.info("모드: {}", kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        orderService.setDemoMode(kisProperties.isDemoMode());
        stockService.setDemoMode(kisProperties.isDemoMode());

        List<PurchaseResult> results = new ArrayList<>();

        for (StockPurchaseConfig config : stocks) {
            if (!config.isEnabled()) {
                log.info("[{}] 종목 {} 스킵 (비활성화)", getScheduleType(), config.getSymbol());
                continue;
            }

            PurchaseResult result;
            if (config.isBudgetBased()) {
                result = executeBudgetBasedPurchase(config);
            } else {
                result = executeSinglePurchase(config);
            }
            results.add(result);
        }

        logPurchaseSummary(results);
        return results;
    }

    /**
     * 원화 예산 기준 매수 실행
     */
    private PurchaseResult executeBudgetBasedPurchase(StockPurchaseConfig config) {
        String symbol = config.getSymbol();
        BigDecimal budgetKrw = config.getBudgetKrw();

        log.info("----- {} 예산 기반 매수 시작 (예산: {}원) -----", symbol, budgetKrw);

        try {
            OverseasExchange exchange = parseExchange(config.getExchange());

            // 1. 현재가 조회
            OverseasStockPrice price = stockService.getPrice(exchange, symbol);
            BigDecimal currentPrice = price.currentPrice();

            // 현재가 null 체크 (장외시간 또는 API 오류)
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} 현재가 조회 실패 (null 또는 0). 장외시간일 수 있습니다.", symbol);
                return new PurchaseResult(symbol, false, null, null, 0,
                        "현재가 조회 실패 - 장외시간이거나 API 오류");
            }

            log.info("{} 현재가: ${}", symbol, currentPrice);

            // 2. 매수가능금액 조회 (환율 정보 포함)
            OverseasPurchasableAmount purchasable = stockService.getPurchasableAmount(exchange, symbol, currentPrice);
            BigDecimal availableUsd = purchasable.availableAmount();
            BigDecimal exchangeRate = purchasable.exchangeRate();

            // 환율이 없으면 기본값 사용
            if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
                exchangeRate = properties.getDefaultExchangeRate();
                log.info("환율 정보 없음, 기본값 사용: {}", exchangeRate);
            }

            log.info("{} 매수가능 달러: ${}, 환율: {}", symbol, availableUsd, exchangeRate);

            // 3. 원화 예산을 달러로 환산
            BigDecimal budgetUsd = budgetKrw.divide(exchangeRate, 2, RoundingMode.DOWN);
            log.info("예산 {}원 → ${}로 환산", budgetKrw, budgetUsd);

            // 4. 실제 사용할 금액 (예산 vs 잔액 중 작은 값)
            BigDecimal actualBudgetUsd = budgetUsd.min(availableUsd);
            log.info("실제 사용 가능 금액: ${} (예산: ${}, 잔액: ${})",
                    actualBudgetUsd, budgetUsd, availableUsd);

            // 5. 주문 가격 계산 (할인율 적용)
            BigDecimal orderPrice = calculateOrderPrice(currentPrice, config);
            log.info("{} 주문가격: ${} (할인율: {}%)",
                    symbol, orderPrice, config.getDiscountRate().multiply(BigDecimal.valueOf(100)));

            // 6. 매수 가능 수량 계산 (정수)
            int quantity = actualBudgetUsd.divide(orderPrice, 0, RoundingMode.DOWN).intValue();
            log.info("{} 매수 가능 수량: {}주 (${} / ${})",
                    symbol, quantity, actualBudgetUsd, orderPrice);

            if (quantity <= 0) {
                log.warn("{} 매수 가능 수량이 0입니다. 잔액 또는 예산 부족", symbol);
                return new PurchaseResult(symbol, false, null, orderPrice, 0,
                        "매수 가능 수량 0 (잔액: $" + availableUsd + ", 예산: $" + budgetUsd + ")");
            }

            // 7. 주문 실행
            OverseasOrderRequest request = OverseasOrderRequest.builder()
                    .exchange(exchange)
                    .symbol(symbol)
                    .quantity(quantity)
                    .price(orderPrice)
                    .orderType(parseOrderType(config.getOrderType()))
                    .build();

            OverseasOrderResult orderResult = orderService.buy(request);

            BigDecimal totalAmount = orderPrice.multiply(BigDecimal.valueOf(quantity));
            log.info("{} 매수 주문 성공 - 주문번호: {}, {}주 x ${} = ${}",
                    symbol, orderResult.orderNumber(), quantity, orderPrice, totalAmount);

            return new PurchaseResult(symbol, true, orderResult.orderNumber(),
                    orderPrice, quantity, null);

        } catch (Exception e) {
            log.error("{} 예산 기반 매수 실패: {}", symbol, e.getMessage());
            return new PurchaseResult(symbol, false, null, null, 0, e.getMessage());
        }
    }
}
