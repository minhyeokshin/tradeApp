package com.stock.trade.util;

import com.stock.trade.config.KisProperties;
import com.stock.trade.token.KisTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KisHttpClient {

    private final WebClient kisWebClient;
    private final KisTokenManager kisTokenManager;
    private final KisProperties kisProperties;

    /**
     * GET 요청
     */
    public <T> Mono<T> get(String uri, Map<String, String> headers, Class<T> responseType) {
        return kisWebClient.get()
                .uri(uri)
                .headers(httpHeaders -> addHeaders(httpHeaders, headers))
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * GET 요청 (쿼리 파라미터 포함)
     */
    public <T> Mono<T> get(String uri, Map<String, String> queryParams,
                           Map<String, String> headers, Class<T> responseType) {
        return kisWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(uri);
                    if (queryParams != null) {
                        queryParams.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.build();
                })
                .headers(httpHeaders -> addHeaders(httpHeaders, headers))
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * POST 요청
     */
    public <T, R> Mono<T> post(String uri, R body, Map<String, String> headers, Class<T> responseType) {
        return kisWebClient.post()
                .uri(uri)
                .headers(httpHeaders -> addHeaders(httpHeaders, headers))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * 동기 GET 요청
     */
    public <T> T getSync(String uri, Map<String, String> headers, Class<T> responseType) {
        return get(uri, headers, responseType).block();
    }

    /**
     * 동기 GET 요청 (쿼리 파라미터 포함)
     */
    public <T> T getSync(String uri, Map<String, String> queryParams,
                         Map<String, String> headers, Class<T> responseType) {
        return get(uri, queryParams, headers, responseType).block();
    }

    /**
     * 동기 POST 요청
     */
    public <T, R> T postSync(String uri, R body, Map<String, String> headers, Class<T> responseType) {
        return post(uri, body, headers, responseType).block();
    }

    private void addHeaders(HttpHeaders httpHeaders, Map<String, String> headers) {
        // 기본 인증 헤더 추가
        httpHeaders.set("authorization", kisTokenManager.getAuthorizationHeader());
        httpHeaders.set("appkey", kisProperties.getAppKey());
        httpHeaders.set("appsecret", kisProperties.getAppSecret());

        // 추가 헤더 (tr_id 등)
        if (headers != null) {
            headers.forEach(httpHeaders::set);
        }
    }
}
