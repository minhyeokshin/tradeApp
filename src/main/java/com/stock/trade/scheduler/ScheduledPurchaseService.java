package com.stock.trade.scheduler;

import com.stock.trade.notification.SlackNotificationService;
import com.stock.trade.scheduler.AbstractPurchaseScheduler.PurchaseResult;
import com.stock.trade.scheduler.MarketFallbackScheduler.MarketFallbackResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 정기 매수 스케줄러 파사드 서비스
 * 주간/월간/시장가전환 스케줄러를 통합 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPurchaseService {

    private final WeeklyPurchaseScheduler weeklyScheduler;
    private final MonthlyPurchaseScheduler monthlyScheduler;
    private final MarketFallbackScheduler fallbackScheduler;
    private final ScheduledPurchaseProperties properties;
    private final SlackNotificationService slackNotificationService;

    /**
     * 주간 매수 수동 실행
     */
    public List<PurchaseResult> executeWeeklyManually() {
        List<PurchaseResult> results = weeklyScheduler.executeManually();
        slackNotificationService.notifyWeeklyPurchaseResult(results);
        return results;
    }

    /**
     * 월간 매수 수동 실행
     */
    public List<PurchaseResult> executeMonthlyManually() {
        List<PurchaseResult> results = monthlyScheduler.executeManually();
        slackNotificationService.notifyMonthlyRebalanceResult(results);
        return results;
    }

    /**
     * 전체 매수 수동 실행 (주간 + 월간)
     */
    public List<PurchaseResult> executeAllManually() {
        log.info("========== 전체 매수 수동 실행 ==========");

        List<PurchaseResult> weeklyResults = weeklyScheduler.executeManually();
        slackNotificationService.notifyWeeklyPurchaseResult(weeklyResults);

        List<PurchaseResult> monthlyResults = monthlyScheduler.executeManually();
        slackNotificationService.notifyMonthlyRebalanceResult(monthlyResults);

        List<PurchaseResult> allResults = new ArrayList<>();
        allResults.addAll(weeklyResults);
        allResults.addAll(monthlyResults);

        return allResults;
    }

    /**
     * 미체결 주문 시장가 전환 수동 실행
     */
    public List<MarketFallbackResult> checkUnfilledManually() {
        List<MarketFallbackResult> results = fallbackScheduler.executeManually();
        slackNotificationService.notifyMarketFallbackResult(results);
        return results;
    }

    /**
     * 스케줄러 설정 조회
     */
    public ScheduledPurchaseProperties getProperties() {
        return properties;
    }
}
