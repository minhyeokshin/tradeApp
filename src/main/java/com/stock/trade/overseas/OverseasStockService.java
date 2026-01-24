package com.stock.trade.overseas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stock.trade.config.KisProperties;
import com.stock.trade.token.KisTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;

/**
 * 해외주식 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverseasStockService {

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final WebClient kisWebClient;

    private static final String PRICE_API_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String BALANCE_API_PATH = "/uapi/overseas-stock/v1/trading/inquire-balance";
    private static final String PSAMOUNT_API_PATH = "/uapi/overseas-stock/v1/trading/inquire-psamount";
    private static final String FOREIGN_MARGIN_API_PATH = "/uapi/overseas-stock/v1/trading/foreign-margin";

    private static final String TR_ID_PRICE = "HHDFS00000300";
    private static final String TR_ID_BALANCE = "TTTS3012R";
    private static final String TR_ID_PSAMOUNT = "TTTS3007R";
    private static final String TR_ID_FOREIGN_MARGIN = "TTTC2101R";

    /**
     * 모의투자 여부
     */
    private boolean demoMode = false;

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    /**
     * 해외주식 현재가 조회
     *
     * @param exchange 거래소 (예: OverseasExchange.NASDAQ)
     * @param symbol   종목코드 (예: "AAPL")
     * @return 현재가 정보
     */
    public OverseasStockPrice getPrice(OverseasExchange exchange, String symbol) {
        return getPrice(exchange.getCode(), symbol);
    }

    /**
     * 해외주식 현재가 조회
     *
     * @param exchangeCode 거래소코드 (예: "NAS")
     * @param symbol       종목코드 (예: "AAPL")
     * @return 현재가 정보
     */
    public OverseasStockPrice getPrice(String exchangeCode, String symbol) {
        log.info("해외주식 현재가 조회 - 거래소: {}, 종목: {}", exchangeCode, symbol);

        try {
            PriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PRICE_API_PATH)
                            .queryParam("AUTH", "")
                            .queryParam("EXCD", exchangeCode)
                            .queryParam("SYMB", symbol)
                            .build())
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", TR_ID_PRICE)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PriceResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("해외주식 현재가 조회 완료 - 종목: {}, 현재가: {}", symbol, response.output().currentPrice());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 현재가 조회 실패 - 종목: {}, 오류: {}", symbol, errorMsg);
                throw new OverseasStockException("해외주식 현재가 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 현재가 조회 API 오류 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasStockException("해외주식 현재가 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasStockException) {
                throw e;
            }
            log.error("해외주식 현재가 조회 중 예외 발생", e);
            throw new OverseasStockException("해외주식 현재가 조회 중 오류: " + e.getMessage(), e);
        }
    }

    // ==================== 잔고 조회 ====================

    /**
     * 해외주식 잔고 조회 (미국 전체)
     *
     * @return 보유 종목 목록
     */
    public List<OverseasStockBalance> getBalance() {
        return getBalance("NASD", "USD");
    }

    /**
     * 해외주식 잔고 조회
     *
     * @param exchangeCode 거래소코드 (NASD: 미국전체, SEHK: 홍콩 등)
     * @param currency     통화코드 (USD, HKD, CNY, JPY, VND)
     * @return 보유 종목 목록
     */
    public List<OverseasStockBalance> getBalance(String exchangeCode, String currency) {
        log.info("해외주식 잔고 조회 - 거래소: {}, 통화: {}, 모드: {}", exchangeCode, currency, demoMode ? "모의투자" : "실전투자");

        // 계좌 정보 확인
        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new OverseasStockException("계좌번호가 설정되지 않았습니다");
        }

        // 모의투자 모드면 TR ID 앞에 V를 붙임
        String trId = demoMode ? "V" + TR_ID_BALANCE.substring(1) : TR_ID_BALANCE;

        try {
            BalanceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BALANCE_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("OVRS_EXCG_CD", exchangeCode)
                            .queryParam("TR_CRCY_CD", currency)
                            .queryParam("CTX_AREA_FK200", "")
                            .queryParam("CTX_AREA_NK200", "")
                            .build())
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", trId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(BalanceResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                List<OverseasStockBalance> balances = response.output1() != null
                        ? response.output1()
                        : Collections.emptyList();
                log.info("해외주식 잔고 조회 완료 - 보유 종목 수: {}", balances.size());
                return balances;
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 잔고 조회 실패 - 오류: {}", errorMsg);
                throw new OverseasStockException("해외주식 잔고 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 잔고 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasStockException("해외주식 잔고 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasStockException) {
                throw e;
            }
            log.error("해외주식 잔고 조회 중 예외 발생", e);
            throw new OverseasStockException("해외주식 잔고 조회 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 종목 보유 여부 확인
     *
     * @param symbol 종목코드
     * @return 보유 정보 (없으면 null)
     */
    public OverseasStockBalance getBalanceBySymbol(String symbol) {
        return getBalance().stream()
                .filter(b -> symbol.equals(b.symbol()))
                .findFirst()
                .orElse(null);
    }

    // ==================== 매수가능금액 조회 ====================

    /**
     * 해외주식 매수가능금액 조회
     *
     * @param exchange 거래소
     * @param symbol   종목코드
     * @param price    주문단가
     * @return 매수가능금액 정보
     */
    public OverseasPurchasableAmount getPurchasableAmount(OverseasExchange exchange, String symbol, java.math.BigDecimal price) {
        return getPurchasableAmount(getApiExchangeCode(exchange), symbol, price);
    }

    /**
     * 해외주식 매수가능금액 조회
     *
     * @param exchangeCode 거래소코드 (NASD, NYSE 등)
     * @param symbol       종목코드
     * @param price        주문단가
     * @return 매수가능금액 정보
     */
    public OverseasPurchasableAmount getPurchasableAmount(String exchangeCode, String symbol, java.math.BigDecimal price) {
        log.info("해외주식 매수가능금액 조회 - 거래소: {}, 종목: {}, 가격: {}", exchangeCode, symbol, price);

        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new OverseasStockException("계좌번호가 설정되지 않았습니다");
        }

        String trId = demoMode ? "V" + TR_ID_PSAMOUNT.substring(1) : TR_ID_PSAMOUNT;

        try {
            PurchasableAmountResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PSAMOUNT_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("OVRS_EXCG_CD", exchangeCode)
                            .queryParam("OVRS_ORD_UNPR", price.toPlainString())
                            .queryParam("ITEM_CD", symbol)
                            .build())
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", trId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PurchasableAmountResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("해외주식 매수가능금액 조회 완료 - 종목: {}, 매수가능수량: {}, 외화잔액: {}",
                        symbol, response.output().purchasableQuantity(), response.output().availableAmount());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 매수가능금액 조회 실패 - 오류: {}", errorMsg);
                throw new OverseasStockException("해외주식 매수가능금액 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 매수가능금액 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasStockException("해외주식 매수가능금액 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasStockException) {
                throw e;
            }
            log.error("해외주식 매수가능금액 조회 중 예외 발생", e);
            throw new OverseasStockException("해외주식 매수가능금액 조회 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 해외증거금 통화별 조회 (원화/달러 잔액)
     *
     * @return 통화별 증거금 목록
     */
    public List<ForeignMargin> getForeignMargin() {
        log.info("해외증거금 통화별 조회");

        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new OverseasStockException("계좌번호가 설정되지 않았습니다");
        }

        try {
            ForeignMarginResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(FOREIGN_MARGIN_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .build())
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", TR_ID_FOREIGN_MARGIN)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(ForeignMarginResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("해외증거금 통화별 조회 완료 - {}건", response.output().size());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외증거금 통화별 조회 실패 - 오류: {}", errorMsg);
                throw new OverseasStockException("해외증거금 통화별 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외증거금 통화별 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasStockException("해외증거금 통화별 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasStockException) {
                throw e;
            }
            log.error("해외증거금 통화별 조회 중 예외 발생", e);
            throw new OverseasStockException("해외증거금 통화별 조회 중 오류: " + e.getMessage(), e);
        }
    }

    private String getApiExchangeCode(OverseasExchange exchange) {
        return switch (exchange) {
            case NASDAQ, NASDAQ_DAY -> "NASD";
            case NYSE, NYSE_DAY -> "NYSE";
            case AMEX, AMEX_DAY -> "AMEX";
            case HONG_KONG -> "SEHK";
            case SHANGHAI, SHANGHAI_INDEX -> "SHAA";
            case SHENZHEN, SHENZHEN_INDEX -> "SZAA";
            case TOKYO -> "TKSE";
            case HO_CHI_MINH -> "VNSE";
            case HANOI -> "HASE";
        };
    }

    // ==================== Response Records ====================

    /**
     * 현재가 API 응답 래퍼
     */
    private record PriceResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") OverseasStockPrice output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    /**
     * 잔고 API 응답 래퍼
     */
    private record BalanceResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output1") List<OverseasStockBalance> output1  // 보유 종목 목록
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    /**
     * 매수가능금액 API 응답 래퍼
     */
    private record PurchasableAmountResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") OverseasPurchasableAmount output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    /**
     * 해외증거금 통화별 API 응답 래퍼
     */
    private record ForeignMarginResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") List<ForeignMargin> output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    // ==================== Exception ====================

    /**
     * 해외주식 조회 예외
     */
    public static class OverseasStockException extends RuntimeException {
        public OverseasStockException(String message) {
            super(message);
        }

        public OverseasStockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
