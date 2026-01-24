package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.overseas.*;
import com.stock.trade.scheduler.ScheduledPurchaseProperties.StockPurchaseConfig;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 정기 매수 스케줄러 공통 기능
 */
@Slf4j
public abstract class AbstractPurchaseScheduler {

    protected final ScheduledPurchaseProperties properties;
    protected final KisProperties kisProperties;
    protected final OverseasOrderService orderService;
    protected final OverseasStockService stockService;

    protected AbstractPurchaseScheduler(ScheduledPurchaseProperties properties,
                                        KisProperties kisProperties,
                                        OverseasOrderService orderService,
                                        OverseasStockService stockService) {
        this.properties = properties;
        this.kisProperties = kisProperties;
        this.orderService = orderService;
        this.stockService = stockService;
    }

    /**
     * 스케줄러 타입 (로그용)
     */
    protected abstract String getScheduleType();

    /**
     * 매수할 종목 목록
     */
    protected abstract List<StockPurchaseConfig> getStockConfigs();

    /**
     * 정기 매수 실행
     */
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

        List<PurchaseResult> results = new ArrayList<>();

        for (StockPurchaseConfig config : stocks) {
            if (!config.isEnabled()) {
                log.info("[{}] 종목 {} 스킵 (비활성화)", getScheduleType(), config.getSymbol());
                continue;
            }

            PurchaseResult result = executeSinglePurchase(config);
            results.add(result);
        }

        logPurchaseSummary(results);
        return results;
    }

    /**
     * 단일 종목 매수 실행
     */
    protected PurchaseResult executeSinglePurchase(StockPurchaseConfig config) {
        String symbol = config.getSymbol();
        log.info("----- {} 매수 시작 -----", symbol);

        try {
            OverseasExchange exchange = parseExchange(config.getExchange());

            // 현재가 조회
            OverseasStockPrice price = stockService.getPrice(exchange, symbol);
            BigDecimal currentPrice = price.currentPrice();

            // 현재가 null 체크 (장외시간 또는 API 오류)
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("{} 현재가 조회 실패 (null 또는 0). 장외시간일 수 있습니다.", symbol);
                return new PurchaseResult(symbol, false, null, null, config.getQuantity(),
                        "현재가 조회 실패 - 장외시간이거나 API 오류");
            }

            log.info("{} 현재가: ${}", symbol, currentPrice);

            // 주문가격 계산
            BigDecimal orderPrice = calculateOrderPrice(currentPrice, config);
            log.info("{} 주문가격: ${} (할인율: {}%)",
                    symbol, orderPrice, config.getDiscountRate().multiply(BigDecimal.valueOf(100)));

            // 주문 실행
            OverseasOrderRequest request = OverseasOrderRequest.builder()
                    .exchange(exchange)
                    .symbol(symbol)
                    .quantity(config.getQuantity())
                    .price(orderPrice)
                    .orderType(parseOrderType(config.getOrderType()))
                    .build();

            OverseasOrderResult orderResult = orderService.buy(request);

            log.info("{} 매수 주문 성공 - 주문번호: {}", symbol, orderResult.orderNumber());

            return new PurchaseResult(symbol, true, orderResult.orderNumber(),
                    orderPrice, config.getQuantity(), null);

        } catch (Exception e) {
            log.error("{} 매수 실패: {}", symbol, e.getMessage());
            return new PurchaseResult(symbol, false, null, null, config.getQuantity(), e.getMessage());
        }
    }

    /**
     * 주문가격 계산
     */
    protected BigDecimal calculateOrderPrice(BigDecimal currentPrice, StockPurchaseConfig config) {
        if ("MARKET_ON_CLOSE".equals(config.getOrderType()) ||
            "MARKET_ON_OPEN".equals(config.getOrderType())) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = currentPrice.multiply(config.getDiscountRate());
        return currentPrice.subtract(discount).setScale(2, RoundingMode.DOWN);
    }

    /**
     * 거래소 파싱
     */
    protected OverseasExchange parseExchange(String exchange) {
        try {
            return OverseasExchange.valueOf(exchange.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OverseasExchange.fromCode(exchange.toUpperCase());
        }
    }

    /**
     * 주문유형 파싱
     */
    protected OverseasOrderType parseOrderType(String orderType) {
        try {
            return OverseasOrderType.valueOf(orderType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OverseasOrderType.LIMIT;
        }
    }

    /**
     * 결과 요약 로그
     */
    protected void logPurchaseSummary(List<PurchaseResult> results) {
        log.info("========== {} 매수 결과 요약 ==========", getScheduleType());

        long successCount = results.stream().filter(PurchaseResult::success).count();
        long failCount = results.size() - successCount;

        log.info("총 {}건 중 성공: {}건, 실패: {}건", results.size(), successCount, failCount);

        for (PurchaseResult result : results) {
            if (result.success()) {
                log.info("  [성공] {} - 주문번호: {}, 가격: ${}, 수량: {}주",
                        result.symbol(), result.orderNumber(), result.price(), result.quantity());
            } else {
                log.info("  [실패] {} - 사유: {}", result.symbol(), result.errorMessage());
            }
        }

        log.info("==========================================");
    }

    /**
     * 매수 결과
     */
    public record PurchaseResult(
            String symbol,
            boolean success,
            String orderNumber,
            BigDecimal price,
            int quantity,
            String errorMessage
    ) {}
}
