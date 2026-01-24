package com.stock.trade.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.kis")
public class KisProperties {

    /**
     * 모의투자 모드 여부
     * true: 모의투자, false: 실전투자
     */
    private boolean demoMode = true;

    /**
     * 실전투자 REST API URL
     */
    private String baseUrl = "https://openapi.koreainvestment.com:9443";

    /**
     * 모의투자 REST API URL
     */
    private String demoBaseUrl = "https://openapivts.koreainvestment.com:29443";

    /**
     * 실전투자 WebSocket URL
     */
    private String wsUrl = "ws://ops.koreainvestment.com:21000";

    /**
     * 모의투자 WebSocket URL
     */
    private String demoWsUrl = "ws://ops.koreainvestment.com:31000";

    // ===== 실전투자 인증 정보 =====
    private String appKey;
    private String appSecret;
    private String accountNumber;
    private String accountProductCode;

    // ===== 모의투자 인증 정보 =====
    private String demoAppKey;
    private String demoAppSecret;
    private String demoAccountNumber;
    private String demoAccountProductCode;

    /**
     * 현재 모드에 맞는 REST API URL 반환
     */
    public String getEffectiveBaseUrl() {
        return demoMode ? demoBaseUrl : baseUrl;
    }

    /**
     * 현재 모드에 맞는 WebSocket URL 반환
     */
    public String getEffectiveWsUrl() {
        return demoMode ? demoWsUrl : wsUrl;
    }

    /**
     * 현재 모드에 맞는 App Key 반환
     */
    public String getEffectiveAppKey() {
        return demoMode ? demoAppKey : appKey;
    }

    /**
     * 현재 모드에 맞는 App Secret 반환
     */
    public String getEffectiveAppSecret() {
        return demoMode ? demoAppSecret : appSecret;
    }

    /**
     * 현재 모드에 맞는 계좌번호 반환
     */
    public String getEffectiveAccountNumber() {
        return demoMode ? demoAccountNumber : accountNumber;
    }

    /**
     * 현재 모드에 맞는 계좌상품코드 반환
     */
    public String getEffectiveAccountProductCode() {
        return demoMode ? demoAccountProductCode : accountProductCode;
    }
}
