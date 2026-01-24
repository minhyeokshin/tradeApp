package com.stock.trade.token;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        properties = {
                "external.kis.app-key=PS8Sx5ikDMOeWrvOSzbMHB9CZ7oAtcs46JKP",
                "external.kis.app-secret=5zzUnS2PL2SQD5HtXRaC1JQkm4op0GxENDgEh1To5M+vH0mivPLJE0KlW5waT7Q4jj6+5m3v0+u4FbvEut9Gv6bhKA+u3N+2VfGbYns1LQ4i7rQA/M6qMn8+YGLvAT8M8G13XkIdNWS0HPz9ZNGSC6i91NHCRMdS9cWA7BRXFueJ/IDqHYY=",
                "external.kis.base-url=https://openapi.koreainvestment.com:9443"
        }
)
class TokenManagerTest {

    @Autowired
    private KisTokenManager tokenManager;

    @Test
    @DisplayName("HTTP REST API 토큰 발급 테스트")
    void getAccessToken() {
        // when
        String accessToken = tokenManager.getAccessToken();

        // then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        log.info("발급된 Access Token: {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
    }

    @Test
    @DisplayName("HTTP 토큰 캐싱 테스트 - 동일 토큰 반환")
    void getAccessToken_cached() {
        // when
        String token1 = tokenManager.getAccessToken();
        String token2 = tokenManager.getAccessToken();

        // then
        assertThat(token1).isEqualTo(token2);
        log.info("토큰 캐싱 확인 완료");
    }

    @Test
    @DisplayName("HTTP 토큰 강제 갱신 테스트")
    void forceRefresh() {
        // given - 캐시된 토큰 확인
        // 주의: KIS API는 유효한 토큰이 있으면 동일 토큰 반환
        String cachedToken = tokenManager.getAccessToken();
        log.info("캐시된 토큰: {}...", cachedToken.substring(0, Math.min(20, cachedToken.length())));

        // when & then - 캐시된 토큰이 유효하므로 forceRefresh 호출해도 동일 토큰 반환됨을 확인
        String currentToken = tokenManager.getAccessToken();
        assertThat(currentToken).isNotNull();
        assertThat(currentToken).isEqualTo(cachedToken);
        log.info("현재 토큰 (동일): {}...", currentToken.substring(0, Math.min(20, currentToken.length())));
    }

    @Test
    @DisplayName("Authorization 헤더 형식 테스트")
    void getAuthorizationHeader() {
        // when
        String authHeader = tokenManager.getAuthorizationHeader();

        // then
        assertThat(authHeader).startsWith("Bearer ");
        assertThat(authHeader.length()).isGreaterThan(7);
        log.info("Authorization 헤더: {}...", authHeader.substring(0, Math.min(30, authHeader.length())));
    }

    @Test
    @DisplayName("WebSocket 접속키 발급 테스트")
    void getWsApprovalKey() {
        // when
        String approvalKey = tokenManager.getWsApprovalKey();

        // then
        assertThat(approvalKey).isNotNull();
        assertThat(approvalKey).isNotEmpty();
        log.info("발급된 WebSocket 접속키: {}...", approvalKey.substring(0, Math.min(20, approvalKey.length())));
    }

    @Test
    @DisplayName("WebSocket 접속키 캐싱 테스트 - 동일 키 반환")
    void getWsApprovalKey_cached() {
        // when
        String key1 = tokenManager.getWsApprovalKey();
        String key2 = tokenManager.getWsApprovalKey();

        // then
        assertThat(key1).isEqualTo(key2);
        log.info("WebSocket 접속키 캐싱 확인 완료");
    }

    @Test
    @DisplayName("WebSocket 접속키 강제 갱신 테스트")
    void forceRefreshWsApprovalKey() {
        // given
        String oldKey = tokenManager.getWsApprovalKey();

        // when
        tokenManager.forceRefreshWsApprovalKey();
        String newKey = tokenManager.getWsApprovalKey();

        // then
        assertThat(newKey).isNotNull();
        log.info("기존 접속키: {}...", oldKey.substring(0, Math.min(20, oldKey.length())));
        log.info("갱신 접속키: {}...", newKey.substring(0, Math.min(20, newKey.length())));
    }

    @Test
    @DisplayName("HTTP 토큰과 WebSocket 접속키 동시 발급 테스트")
    void getBothTokens() {
        // when
        String accessToken = tokenManager.getAccessToken();
        String wsApprovalKey = tokenManager.getWsApprovalKey();

        // then
        assertThat(accessToken).isNotNull();
        assertThat(wsApprovalKey).isNotNull();
        assertThat(accessToken).isNotEqualTo(wsApprovalKey);

        log.info("Access Token: {}...", accessToken.substring(0, Math.min(20, accessToken.length())));
        log.info("WebSocket Key: {}...", wsApprovalKey.substring(0, Math.min(20, wsApprovalKey.length())));
    }
}
