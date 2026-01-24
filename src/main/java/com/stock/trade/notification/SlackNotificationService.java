package com.stock.trade.notification;

import com.stock.trade.scheduler.AbstractPurchaseScheduler.PurchaseResult;
import com.stock.trade.scheduler.MarketFallbackScheduler.MarketFallbackResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Slack 알림 서비스
 */
@Slf4j
@Service
public class SlackNotificationService {

    private final SlackProperties slackProperties;
    private final WebClient webClient;

    public SlackNotificationService(SlackProperties slackProperties) {
        this.slackProperties = slackProperties;
        this.webClient = WebClient.create();
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 주간 매수 결과 알림
     */
    public void notifyWeeklyPurchaseResult(List<PurchaseResult> results) {
        if (!slackProperties.isEnabled() || results.isEmpty()) {
            return;
        }

        long successCount = results.stream().filter(PurchaseResult::success).count();
        String emoji = successCount == results.size() ? ":white_check_mark:" : ":warning:";

        StringBuilder message = new StringBuilder();
        message.append(emoji).append(" *주간 정기 매수 완료*\n");
        message.append("시각: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n\n");

        for (PurchaseResult result : results) {
            if (result.success()) {
                message.append(":moneybag: *").append(result.symbol()).append("* - 성공\n");
                message.append("   주문번호: `").append(result.orderNumber()).append("`\n");
                message.append("   가격: $").append(result.price()).append(" x ").append(result.quantity()).append("주\n");
            } else {
                message.append(":x: *").append(result.symbol()).append("* - 실패\n");
                message.append("   사유: ").append(result.errorMessage()).append("\n");
            }
        }

        sendMessage(message.toString());
    }

    /**
     * 월간 리밸런싱 결과 알림
     */
    public void notifyMonthlyRebalanceResult(List<PurchaseResult> results) {
        if (!slackProperties.isEnabled() || results.isEmpty()) {
            return;
        }

        long successCount = results.stream().filter(PurchaseResult::success).count();
        String emoji = successCount == results.size() ? ":scales:" : ":warning:";

        StringBuilder message = new StringBuilder();
        message.append(emoji).append(" *월간 리밸런싱 완료*\n");
        message.append("시각: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n\n");

        for (PurchaseResult result : results) {
            String action = result.symbol().contains("_SELL") ? "매도" : "매수";
            String symbol = result.symbol().replace("_SELL", "").replace("_BUY", "");

            if (result.success()) {
                message.append(":white_check_mark: *").append(symbol).append("* ").append(action).append(" 성공\n");
                message.append("   주문번호: `").append(result.orderNumber()).append("`\n");
                message.append("   가격: $").append(result.price()).append(" x ").append(result.quantity()).append("주\n");
            } else {
                message.append(":x: *").append(symbol).append("* ").append(action).append(" 실패\n");
                message.append("   사유: ").append(result.errorMessage()).append("\n");
            }
        }

        sendMessage(message.toString());
    }

    /**
     * 미체결 시장가 전환 결과 알림
     */
    public void notifyMarketFallbackResult(List<MarketFallbackResult> results) {
        if (!slackProperties.isEnabled() || results.isEmpty()) {
            return;
        }

        long successCount = results.stream().filter(MarketFallbackResult::success).count();
        String emoji = successCount == results.size() ? ":arrows_counterclockwise:" : ":warning:";

        StringBuilder message = new StringBuilder();
        message.append(emoji).append(" *미체결 주문 시장가 전환*\n");
        message.append("시각: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n\n");

        for (MarketFallbackResult result : results) {
            if (result.success()) {
                message.append(":white_check_mark: *").append(result.symbol()).append("* 전환 성공\n");
                message.append("   취소: `").append(result.cancelledOrderNumber()).append("`\n");
                message.append("   신규: `").append(result.newOrderNumber()).append("` (").append(result.quantity()).append("주)\n");
            } else {
                message.append(":x: *").append(result.symbol()).append("* 전환 실패\n");
                message.append("   사유: ").append(result.errorMessage()).append("\n");
            }
        }

        sendMessage(message.toString());
    }

    /**
     * 잔액 알림
     */
    public void notifyBalance(BigDecimal krwBalance, BigDecimal usdBalance, BigDecimal exchangeRate) {
        if (!slackProperties.isEnabled()) {
            return;
        }

        // 원화 환산 총액 계산
        BigDecimal usdInKrw = usdBalance.multiply(exchangeRate).setScale(0, RoundingMode.DOWN);
        BigDecimal totalKrw = krwBalance.add(usdInKrw);

        StringBuilder message = new StringBuilder();
        message.append(":bank: *주간 잔액 알림*\n");
        message.append("시각: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n\n");

        message.append(":kr: *원화 잔액*: ").append(String.format("%,.0f", krwBalance)).append("원\n");
        message.append(":us: *달러 잔액*: $").append(String.format("%,.2f", usdBalance)).append("\n");
        message.append(":currency_exchange: *현재 환율*: ").append(String.format("%,.2f", exchangeRate)).append("원/$\n\n");

        message.append(":moneybag: *원화 환산 총액*: ").append(String.format("%,.0f", totalKrw)).append("원\n");
        message.append("   (달러 환산: ").append(String.format("%,.0f", usdInKrw)).append("원)");

        sendMessage(message.toString());
    }

    /**
     * 오류 알림
     */
    public void notifyError(String title, String errorMessage) {
        if (!slackProperties.isEnabled()) {
            return;
        }

        String message = ":rotating_light: *" + title + "*\n" +
                "시각: " + LocalDateTime.now().format(TIME_FORMAT) + "\n\n" +
                "```" + errorMessage + "```";

        sendMessage(message);
    }

    /**
     * 커스텀 메시지 전송
     */
    public void sendCustomMessage(String message) {
        if (!slackProperties.isEnabled()) {
            return;
        }
        sendMessage(message);
    }

    /**
     * Slack으로 메시지 전송
     */
    private void sendMessage(String text) {
        if (slackProperties.getWebhookUrl() == null || slackProperties.getWebhookUrl().isBlank()) {
            log.warn("Slack webhook URL이 설정되지 않았습니다");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", text);
        payload.put("username", slackProperties.getUsername());
        payload.put("icon_emoji", slackProperties.getIconEmoji());

        if (slackProperties.getChannel() != null && !slackProperties.getChannel().isBlank()) {
            payload.put("channel", slackProperties.getChannel());
        }

        try {
            String response = webClient.post()
                    .uri(slackProperties.getWebhookUrl())
                    .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();  // 동기 방식으로 전송

            log.info("Slack 알림 전송 완료: {}", response);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage());
        }
    }
}
