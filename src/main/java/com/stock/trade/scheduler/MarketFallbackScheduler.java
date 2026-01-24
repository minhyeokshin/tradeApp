package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.notification.SlackNotificationService;
import com.stock.trade.overseas.*;
import com.stock.trade.scheduler.ScheduledPurchaseProperties.StockPurchaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 미체결 주문 시장가 전환 스케줄러
 * 장마감 1시간 전 미체결 주문을 시장가로 전환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketFallbackScheduler {

    private final ScheduledPurchaseProperties properties;
    private final KisProperties kisProperties;
    private final OverseasOrderService orderService;
    private final SlackNotificationService slackNotificationService;

    /**
     * 장마감 1시간 전 미체결 주문 시장가 전환
     * 미국 장 마감: 06:00 KST (서머타임 05:00 KST)
     * 1시간 전: 05:00 KST (서머타임 04:00 KST)
     *
     * 평일에만 실행 (월~금)
     */
    @Scheduled(cron = "${scheduler.purchase.fallback-cron:0 0 5 * * MON-FRI}", zone = "Asia/Seoul")
    public void execute() {
        if (!properties.isEnabled()) {
            log.debug("스케줄러가 비활성화 상태입니다");
            return;
        }

        log.info("========== 미체결 주문 시장가 전환 체크 시작 ==========");
        log.info("실행 시각: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));

        orderService.setDemoMode(kisProperties.isDemoMode());

        List<StockPurchaseConfig> allStocks = getAllMarketFallbackStocks();
        List<MarketFallbackResult> results = new ArrayList<>();

        for (StockPurchaseConfig config : allStocks) {
            MarketFallbackResult result = checkAndConvertToMarket(config);
            if (result != null) {
                results.add(result);
            }
        }

        logSummary(results);
        slackNotificationService.notifyMarketFallbackResult(results);
    }

    /**
     * 수동 실행 (테스트용)
     */
    public List<MarketFallbackResult> executeManually() {
        log.info("========== 미체결 주문 시장가 전환 수동 실행 ==========");

        orderService.setDemoMode(kisProperties.isDemoMode());

        List<StockPurchaseConfig> allStocks = getAllMarketFallbackStocks();
        List<MarketFallbackResult> results = new ArrayList<>();

        for (StockPurchaseConfig config : allStocks) {
            MarketFallbackResult result = checkAndConvertToMarket(config);
            if (result != null) {
                results.add(result);
            }
        }

        logSummary(results);
        return results;
    }

    /**
     * 시장가 전환 대상 종목 목록 (주간 + 월간)
     */
    private List<StockPurchaseConfig> getAllMarketFallbackStocks() {
        List<StockPurchaseConfig> allStocks = new ArrayList<>();

        for (StockPurchaseConfig config : properties.getStocks()) {
            if (config.isEnabled() && config.isMarketFallback()) {
                allStocks.add(config);
            }
        }

        for (StockPurchaseConfig config : properties.getMonthlyStocks()) {
            if (config.isEnabled() && config.isMarketFallback()) {
                allStocks.add(config);
            }
        }

        return allStocks;
    }

    /**
     * 미체결 확인 및 시장가 전환
     */
    private MarketFallbackResult checkAndConvertToMarket(StockPurchaseConfig config) {
        String symbol = config.getSymbol();
        OverseasExchange exchange = parseExchange(config.getExchange());

        try {
            // 해당 종목의 미체결 주문 조회
            List<OverseasUnfilledOrder> unfilledOrders = orderService.getUnfilledOrdersBySymbol(exchange, symbol);

            // 매수 미체결만 필터링
            List<OverseasUnfilledOrder> buyUnfilled = unfilledOrders.stream()
                    .filter(OverseasUnfilledOrder::isBuyOrder)
                    .filter(OverseasUnfilledOrder::hasUnfilledQuantity)
                    .toList();

            if (buyUnfilled.isEmpty()) {
                log.info("{}: 미체결 매수 주문 없음", symbol);
                return null;
            }

            log.info("{}: 미체결 매수 주문 {}건 발견", symbol, buyUnfilled.size());

            // 각 미체결 주문 취소 후 시장가로 재주문
            for (OverseasUnfilledOrder unfilled : buyUnfilled) {
                log.info("{}: 주문번호 {} 취소 시도 (미체결 {}주)",
                        symbol, unfilled.orderNumber(), unfilled.unfilledQuantity());

                // 1. 기존 주문 취소
                orderService.cancelOrder(unfilled);
                log.info("{}: 주문 취소 완료", symbol);

                // 2. 시장가(MOC)로 재주문
                OverseasOrderRequest marketRequest = OverseasOrderRequest.builder()
                        .exchange(exchange)
                        .symbol(symbol)
                        .quantity(unfilled.unfilledQuantity().intValue())
                        .price(BigDecimal.ZERO)
                        .orderType(OverseasOrderType.MARKET_ON_CLOSE)
                        .build();

                OverseasOrderResult newOrder = orderService.buy(marketRequest);
                log.info("{}: 시장가 재주문 완료 - 새 주문번호: {}", symbol, newOrder.orderNumber());

                return new MarketFallbackResult(
                        symbol,
                        true,
                        unfilled.orderNumber(),
                        newOrder.orderNumber(),
                        unfilled.unfilledQuantity().intValue(),
                        null
                );
            }

        } catch (Exception e) {
            log.error("{}: 시장가 전환 실패 - {}", symbol, e.getMessage());
            return new MarketFallbackResult(symbol, false, null, null, 0, e.getMessage());
        }

        return null;
    }

    private OverseasExchange parseExchange(String exchange) {
        try {
            return OverseasExchange.valueOf(exchange.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OverseasExchange.fromCode(exchange.toUpperCase());
        }
    }

    private void logSummary(List<MarketFallbackResult> results) {
        if (results.isEmpty()) {
            log.info("시장가 전환 대상 없음");
            return;
        }

        log.info("========== 시장가 전환 결과 요약 ==========");

        long successCount = results.stream().filter(MarketFallbackResult::success).count();
        log.info("총 {}건 중 성공: {}건", results.size(), successCount);

        for (MarketFallbackResult result : results) {
            if (result.success()) {
                log.info("  [성공] {} - 취소: {} → 시장가: {} ({}주)",
                        result.symbol(), result.cancelledOrderNumber(),
                        result.newOrderNumber(), result.quantity());
            } else {
                log.info("  [실패] {} - 사유: {}", result.symbol(), result.errorMessage());
            }
        }

        log.info("==========================================");
    }

    /**
     * 시장가 전환 결과
     */
    public record MarketFallbackResult(
            String symbol,
            boolean success,
            String cancelledOrderNumber,
            String newOrderNumber,
            int quantity,
            String errorMessage
    ) {}
}
