package com.stock.trade.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stock.trade.config.KisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisTokenManager {

    private final KisProperties kisProperties;
    private final WebClient kisWebClient;

    // HTTP REST API 토큰
    private String accessToken;
    private LocalDateTime tokenExpireTime;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // WebSocket 접속키
    private String wsApprovalKey;
    private LocalDateTime wsApprovalKeyExpireTime;
    private final ReentrantLock wsLock = new ReentrantLock();

    private static final String TOKEN_PATH = "/oauth2/tokenP";
    private static final String WS_APPROVAL_PATH = "/oauth2/Approval";
    private static final long TOKEN_EXPIRE_MARGIN_HOURS = 1;

    // ==================== HTTP REST API 토큰 ====================

    /**
     * 유효한 액세스 토큰 반환 (없거나 만료 임박 시 자동 갱신)
     */
    public String getAccessToken() {
        if (isTokenValid()) {
            return accessToken;
        }

        tokenLock.lock();
        try {
            if (isTokenValid()) {
                return accessToken;
            }
            refreshToken();
            return accessToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 토큰 강제 갱신
     */
    public void forceRefresh() {
        tokenLock.lock();
        try {
            refreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Authorization 헤더 값 반환
     */
    public String getAuthorizationHeader() {
        return "Bearer " + getAccessToken();
    }

    private boolean isTokenValid() {
        if (accessToken == null || tokenExpireTime == null) {
            return false;
        }
        return LocalDateTime.now().plusHours(TOKEN_EXPIRE_MARGIN_HOURS).isBefore(tokenExpireTime);
    }

    // ==================== WebSocket 접속키 ====================

    /**
     * 유효한 WebSocket 접속키 반환 (없거나 만료 임박 시 자동 갱신)
     */
    public String getWsApprovalKey() {
        if (isWsApprovalKeyValid()) {
            return wsApprovalKey;
        }

        wsLock.lock();
        try {
            if (isWsApprovalKeyValid()) {
                return wsApprovalKey;
            }
            refreshWsApprovalKey();
            return wsApprovalKey;
        } finally {
            wsLock.unlock();
        }
    }

    /**
     * WebSocket 접속키 강제 갱신
     */
    public void forceRefreshWsApprovalKey() {
        wsLock.lock();
        try {
            refreshWsApprovalKey();
        } finally {
            wsLock.unlock();
        }
    }

    private boolean isWsApprovalKeyValid() {
        if (wsApprovalKey == null || wsApprovalKeyExpireTime == null) {
            return false;
        }
        return LocalDateTime.now().plusHours(TOKEN_EXPIRE_MARGIN_HOURS).isBefore(wsApprovalKeyExpireTime);
    }

    private void refreshWsApprovalKey() {
        log.info("KIS WebSocket 접속키 발급 요청 - URL: {}", WS_APPROVAL_PATH);

        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisProperties.getEffectiveAppKey(),
                "secretkey", kisProperties.getEffectiveAppSecret()
        );

        try {
            WsApprovalResponse response = kisWebClient.post()
                    .uri(WS_APPROVAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_PLAIN)
                    .header("charset", "UTF-8")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(WsApprovalResponse.class)
                    .block();

            if (response != null && response.approvalKey() != null) {
                this.wsApprovalKey = response.approvalKey();
                // WebSocket 접속키는 24시간 유효
                this.wsApprovalKeyExpireTime = LocalDateTime.now().plusHours(24);
                log.info("KIS WebSocket 접속키 발급 완료. 만료시간: {}", wsApprovalKeyExpireTime);
            } else {
                throw new KisTokenException("KIS WebSocket 접속키 발급 실패: 응답이 비어있습니다");
            }
        } catch (WebClientResponseException e) {
            log.error("KIS WebSocket 접속키 발급 실패 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new KisTokenException("KIS WebSocket 접속키 발급 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("KIS WebSocket 접속키 발급 중 예외 발생", e);
            throw new KisTokenException("KIS WebSocket 접속키 발급 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private void refreshToken() {
        log.info("KIS 토큰 발급 요청 - URL: {}", TOKEN_PATH);

        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", kisProperties.getEffectiveAppKey(),
                "appsecret", kisProperties.getEffectiveAppSecret()
        );

        try {
            TokenResponse response = kisWebClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_PLAIN)
                    .header("charset", "UTF-8")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response != null && response.accessToken() != null) {
                this.accessToken = response.accessToken();
                this.tokenExpireTime = LocalDateTime.now().plusSeconds(response.expiresIn());
                log.info("KIS 토큰 발급 완료. 만료시간: {}", tokenExpireTime);
            } else {
                throw new KisTokenException("KIS 토큰 발급 실패: 응답이 비어있습니다");
            }
        } catch (WebClientResponseException e) {
            log.error("KIS 토큰 발급 실패 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new KisTokenException("KIS 토큰 발급 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("KIS 토큰 발급 중 예외 발생", e);
            throw new KisTokenException("KIS 토큰 발급 중 오류 발생: " + e.getMessage(), e);
        }
    }

    // ==================== Response Records ====================

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Long expiresIn
    ) {}

    private record WsApprovalResponse(
            @JsonProperty("approval_key") String approvalKey
    ) {}

    public static class KisTokenException extends RuntimeException {
        public KisTokenException(String message) {
            super(message);
        }

        public KisTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
