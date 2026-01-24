package com.stock.trade.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final KisProperties kisProperties;

    @Bean
    public WebClient kisWebClient() {
        String baseUrl = kisProperties.getEffectiveBaseUrl();
        log.info("KIS API 모드: {}, URL: {}",
                kisProperties.isDemoMode() ? "모의투자" : "실전투자", baseUrl);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
