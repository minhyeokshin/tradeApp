package com.stock.trade.scheduler;

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

    /**
     * 주간 매수 수동 실행
     */
    public List<PurchaseResult> executeWeeklyManually() {
        return weeklyScheduler.executeManually();
    }

    /**
     * 월간 매수 수동 실행
     */
    public List<PurchaseResult> executeMonthlyManually() {
        return monthlyScheduler.executeManually();
    }

    /**
     * 전체 매수 수동 실행 (주간 + 월간)
     */
    public List<PurchaseResult> executeAllManually() {
        log.info("========== 전체 매수 수동 실행 ==========");

        List<PurchaseResult> allResults = new ArrayList<>();
        allResults.addAll(weeklyScheduler.executeManually());
        allResults.addAll(monthlyScheduler.executeManually());

        return allResults;
    }

    /**
     * 미체결 주문 시장가 전환 수동 실행
     */
    public List<MarketFallbackResult> checkUnfilledManually() {
        return fallbackScheduler.executeManually();
    }

    /**
     * 스케줄러 설정 조회
     */
    public ScheduledPurchaseProperties getProperties() {
        return properties;
    }
}
