package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.notification.SlackNotificationService;
import com.stock.trade.scheduler.AbstractPurchaseScheduler.PurchaseResult;
import com.stock.trade.scheduler.MarketFallbackScheduler.MarketFallbackResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 정기 매수 스케줄러 수동 실행 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class ScheduledPurchaseController {

    private final ScheduledPurchaseService purchaseService;
    private final ScheduledPurchaseProperties properties;
    private final KisProperties kisProperties;
    private final SlackNotificationService slackNotificationService;

    /**
     * 스케줄러 설정 조회
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "enabled", properties.isEnabled(),
                "demoMode", kisProperties.isDemoMode(),
                "weeklyStocks", properties.getStocks(),
                "rebalance", properties.getRebalance()
        ));
    }

    /**
     * 주간 매수 수동 실행
     */
    @PostMapping("/execute/weekly")
    public ResponseEntity<List<PurchaseResult>> executeWeekly() {
        log.info("주간 매수 수동 실행 요청");
        List<PurchaseResult> results = purchaseService.executeWeeklyManually();
        return buildResponse(results);
    }

    /**
     * 월간 매수 수동 실행
     */
    @PostMapping("/execute/monthly")
    public ResponseEntity<List<PurchaseResult>> executeMonthly() {
        log.info("월간 매수 수동 실행 요청");
        List<PurchaseResult> results = purchaseService.executeMonthlyManually();
        return buildResponse(results);
    }

    /**
     * 전체 매수 수동 실행
     */
    @PostMapping("/execute")
    public ResponseEntity<List<PurchaseResult>> executeAll() {
        log.info("전체 매수 수동 실행 요청");
        List<PurchaseResult> results = purchaseService.executeAllManually();
        return buildResponse(results);
    }

    private ResponseEntity<List<PurchaseResult>> buildResponse(List<PurchaseResult> results) {
        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        long successCount = results.stream().filter(PurchaseResult::success).count();

        if (successCount == results.size()) {
            return ResponseEntity.ok(results);
        } else if (successCount > 0) {
            return ResponseEntity.status(207).body(results); // Multi-Status
        } else {
            return ResponseEntity.internalServerError().body(results);
        }
    }

    /**
     * 미체결 주문 시장가 전환 수동 실행
     */
    @PostMapping("/execute/fallback")
    public ResponseEntity<List<MarketFallbackResult>> executeFallback() {
        log.info("미체결 주문 시장가 전환 수동 실행 요청");
        List<MarketFallbackResult> results = purchaseService.checkUnfilledManually();

        if (results.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        long successCount = results.stream().filter(MarketFallbackResult::success).count();
        if (successCount == results.size()) {
            return ResponseEntity.ok(results);
        } else {
            return ResponseEntity.status(207).body(results);
        }
    }

    /**
     * 스케줄러 활성화/비활성화
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@RequestParam boolean enabled) {
        properties.setEnabled(enabled);
        log.info("스케줄러 {} 됨", enabled ? "활성화" : "비활성화");

        return ResponseEntity.ok(Map.of(
                "enabled", properties.isEnabled(),
                "message", enabled ? "스케줄러가 활성화되었습니다" : "스케줄러가 비활성화되었습니다"
        ));
    }

    /**
     * Slack 알림 테스트
     */
    @PostMapping("/slack/test")
    public ResponseEntity<Map<String, String>> testSlack() {
        log.info("Slack 알림 테스트 요청");
        slackNotificationService.sendCustomMessage(":white_check_mark: *Trade Bot Test*\nSlack notification is working!");
        return ResponseEntity.ok(Map.of("message", "Slack test message sent"));
    }
}
