package com.stock.trade.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.slack")
public class SlackProperties {

    /**
     * Slack 알림 활성화 여부
     */
    private boolean enabled = false;

    /**
     * Slack Incoming Webhook URL
     */
    private String webhookUrl;

    /**
     * 알림 채널 (webhook에 설정된 기본 채널 사용 시 비워둠)
     */
    private String channel;

    /**
     * 봇 이름
     */
    private String username = "Trade Bot";

    /**
     * 봇 아이콘 이모지
     */
    private String iconEmoji = ":chart_with_upwards_trend:";
}
