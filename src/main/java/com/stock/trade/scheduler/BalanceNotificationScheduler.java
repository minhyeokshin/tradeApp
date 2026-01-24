package com.stock.trade.scheduler;

import com.stock.trade.config.KisProperties;
import com.stock.trade.notification.SlackNotificationService;
import com.stock.trade.overseas.ForeignMargin;
import com.stock.trade.overseas.OverseasStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 잔액 알림 스케줄러
 * 매주 월요일 오전 10시에 계좌 잔액 알림
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceNotificationScheduler {

    private final OverseasStockService stockService;
    private final SlackNotificationService slackNotificationService;
    private final KisProperties kisProperties;
    private final ScheduledPurchaseProperties properties;

    /**
     * 매주 월요일 오전 10시 (KST) 실행
     */
    @Scheduled(cron = "${scheduler.purchase.balance-cron:0 0 10 * * MON}", zone = "Asia/Seoul")
    public void execute() {
        if (!properties.isEnabled()) {
            log.debug("스케줄러가 비활성화 상태입니다");
            return;
        }
        sendBalanceNotification();
    }

    /**
     * 수동 실행 (테스트용)
     */
    public void executeManually() {
        log.info("========== 잔액 알림 수동 실행 ==========");
        sendBalanceNotification();
    }

    private void sendBalanceNotification() {
        log.info("========== 잔액 알림 스케줄러 시작 ==========");
        log.info("실행 시각: {}", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        log.info("모드: {}", kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        stockService.setDemoMode(kisProperties.isDemoMode());

        try {
            List<ForeignMargin> margins = stockService.getForeignMargin();

            BigDecimal krwBalance = BigDecimal.ZERO;
            BigDecimal usdBalance = BigDecimal.ZERO;
            BigDecimal exchangeRate = BigDecimal.ZERO;

            for (ForeignMargin margin : margins) {
                if ("KRW".equals(margin.currencyCode())) {
                    krwBalance = margin.depositAmount() != null ? margin.depositAmount() : BigDecimal.ZERO;
                } else if ("USD".equals(margin.currencyCode())) {
                    usdBalance = margin.depositAmount() != null ? margin.depositAmount() : BigDecimal.ZERO;
                    exchangeRate = margin.exchangeRate() != null ? margin.exchangeRate() : BigDecimal.ZERO;
                }
            }

            log.info("원화 잔액: {}원, 달러 잔액: ${}, 환율: {}", krwBalance, usdBalance, exchangeRate);

            slackNotificationService.notifyBalance(krwBalance, usdBalance, exchangeRate);

        } catch (Exception e) {
            log.error("잔액 조회 실패: {}", e.getMessage());
            slackNotificationService.sendCustomMessage(
                    ":warning: *잔액 조회 실패*\n" + e.getMessage()
            );
        }
    }
}
