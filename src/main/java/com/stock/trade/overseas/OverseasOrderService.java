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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 해외주식 주문 서비스
 * API: /uapi/overseas-stock/v1/trading/order
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverseasOrderService {

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final WebClient kisWebClient;

    private static final String ORDER_API_PATH = "/uapi/overseas-stock/v1/trading/order";
    private static final String UNFILLED_API_PATH = "/uapi/overseas-stock/v1/trading/inquire-nccs";
    private static final String CANCEL_API_PATH = "/uapi/overseas-stock/v1/trading/order-rvsecncl";

    // 미체결 조회 TR ID
    private static final String TR_UNFILLED = "TTTS3018R";

    // 정정취소 TR ID
    private static final String TR_CANCEL = "TTTT1004U";

    /**
     * 모의투자 여부 (true: 모의투자, false: 실전투자)
     */
    private boolean demoMode = false;

    /**
     * 모의투자 모드 설정
     */
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
        log.info("주문 모드 변경: {}", demoMode ? "모의투자" : "실전투자");
    }

    /**
     * 모의투자 모드 여부
     */
    public boolean isDemoMode() {
        return demoMode;
    }

    // ==================== TR ID 상수 ====================

    // 미국 (NASD, NYSE, AMEX)
    private static final String TR_US_BUY = "TTTT1002U";
    private static final String TR_US_SELL = "TTTT1006U";

    // 홍콩 (SEHK)
    private static final String TR_HK_BUY = "TTTS1002U";
    private static final String TR_HK_SELL = "TTTS1001U";

    // 중국 상해 (SHAA)
    private static final String TR_SH_BUY = "TTTS0202U";
    private static final String TR_SH_SELL = "TTTS1005U";

    // 중국 심천 (SZAA)
    private static final String TR_SZ_BUY = "TTTS0305U";
    private static final String TR_SZ_SELL = "TTTS0304U";

    // 일본 (TKSE)
    private static final String TR_JP_BUY = "TTTS0308U";
    private static final String TR_JP_SELL = "TTTS0307U";

    // 베트남 (HASE, VNSE)
    private static final String TR_VN_BUY = "TTTS0311U";
    private static final String TR_VN_SELL = "TTTS0310U";

    // ==================== 매수 주문 ====================

    /**
     * 해외주식 매수 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    public OverseasOrderResult buy(OverseasOrderRequest request) {
        request.validate();
        String trId = applyDemoMode(getBuyTrId(request.getExchange()));
        return executeOrder(request, trId, false);
    }

    /**
     * 해외주식 매수 주문 (간편 버전)
     *
     * @param exchange 거래소
     * @param symbol   종목코드
     * @param quantity 수량
     * @param price    가격
     * @return 주문 결과
     */
    public OverseasOrderResult buy(OverseasExchange exchange, String symbol, int quantity, BigDecimal price) {
        return buy(OverseasOrderRequest.buyLimit(exchange, symbol, quantity, price));
    }

    // ==================== 매도 주문 ====================

    /**
     * 해외주식 매도 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    public OverseasOrderResult sell(OverseasOrderRequest request) {
        request.validate();
        String trId = applyDemoMode(getSellTrId(request.getExchange()));
        return executeOrder(request, trId, true);
    }

    /**
     * 해외주식 매도 주문 (간편 버전)
     *
     * @param exchange 거래소
     * @param symbol   종목코드
     * @param quantity 수량
     * @param price    가격
     * @return 주문 결과
     */
    public OverseasOrderResult sell(OverseasExchange exchange, String symbol, int quantity, BigDecimal price) {
        return sell(OverseasOrderRequest.sellLimit(exchange, symbol, quantity, price));
    }

    // ==================== 미체결 조회 ====================

    /**
     * 해외주식 미체결 내역 조회
     *
     * @param exchange 거래소 (NASD로 조회하면 미국 전체)
     * @return 미체결 주문 목록
     */
    public List<OverseasUnfilledOrder> getUnfilledOrders(OverseasExchange exchange) {
        return getUnfilledOrders(getApiExchangeCode(exchange));
    }

    /**
     * 해외주식 미체결 내역 조회
     *
     * @param exchangeCode 거래소 코드 (NASD, NYSE, AMEX 등)
     * @return 미체결 주문 목록
     */
    public List<OverseasUnfilledOrder> getUnfilledOrders(String exchangeCode) {
        log.info("해외주식 미체결 조회 - 거래소: {}", exchangeCode);

        validateAccountInfo();

        String trId = applyDemoMode(TR_UNFILLED);

        try {
            UnfilledResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(UNFILLED_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("OVRS_EXCG_CD", exchangeCode)
                            .queryParam("SORT_SQN", "DS")
                            .queryParam("CTX_AREA_FK200", "")
                            .queryParam("CTX_AREA_NK200", "")
                            .build())
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", trId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(UnfilledResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                List<OverseasUnfilledOrder> orders = response.output() != null
                        ? response.output()
                        : Collections.emptyList();
                log.info("해외주식 미체결 조회 완료 - {}건", orders.size());
                return orders;
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 미체결 조회 실패 - 오류: {}", errorMsg);
                throw new OverseasOrderException("해외주식 미체결 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 미체결 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasOrderException("해외주식 미체결 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasOrderException) {
                throw e;
            }
            log.error("해외주식 미체결 조회 중 예외 발생", e);
            throw new OverseasOrderException("해외주식 미체결 조회 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 종목의 미체결 주문 조회
     *
     * @param exchange 거래소
     * @param symbol   종목코드
     * @return 해당 종목의 미체결 주문 목록
     */
    public List<OverseasUnfilledOrder> getUnfilledOrdersBySymbol(OverseasExchange exchange, String symbol) {
        return getUnfilledOrders(exchange).stream()
                .filter(order -> symbol.equals(order.symbol()))
                .toList();
    }

    // ==================== 주문 취소 ====================

    /**
     * 해외주식 주문 취소
     *
     * @param exchange    거래소
     * @param symbol      종목코드
     * @param orderNumber 원주문번호
     * @param quantity    취소수량
     * @return 취소 결과
     */
    public OverseasOrderResult cancelOrder(OverseasExchange exchange, String symbol,
                                           String orderNumber, int quantity) {
        log.info("해외주식 주문 취소 - 거래소: {}, 종목: {}, 주문번호: {}, 수량: {}",
                exchange.getCode(), symbol, orderNumber, quantity);

        validateAccountInfo();

        String trId = applyDemoMode(TR_CANCEL);

        Map<String, String> body = new HashMap<>();
        body.put("CANO", kisProperties.getEffectiveAccountNumber());
        body.put("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode());
        body.put("OVRS_EXCG_CD", getApiExchangeCode(exchange));
        body.put("PDNO", symbol);
        body.put("ORGN_ODNO", orderNumber);
        body.put("RVSE_CNCL_DVSN_CD", "02");  // 02: 취소
        body.put("ORD_QTY", String.valueOf(quantity));
        body.put("OVRS_ORD_UNPR", "0");  // 취소 시 0
        body.put("MGCO_APTM_ODNO", "");
        body.put("ORD_SVR_DVSN_CD", "0");

        try {
            OrderResponse response = kisWebClient.post()
                    .uri(CANCEL_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", trId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(OrderResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("해외주식 주문 취소 성공 - 주문번호: {}", response.output().orderNumber());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 주문 취소 실패 - 오류: {}", errorMsg);
                throw new OverseasOrderException("해외주식 주문 취소 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 주문 취소 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasOrderException("해외주식 주문 취소 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasOrderException) {
                throw e;
            }
            log.error("해외주식 주문 취소 중 예외 발생", e);
            throw new OverseasOrderException("해외주식 주문 취소 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 미체결 주문 취소
     *
     * @param unfilledOrder 미체결 주문
     * @return 취소 결과
     */
    public OverseasOrderResult cancelOrder(OverseasUnfilledOrder unfilledOrder) {
        OverseasExchange exchange = parseExchangeCode(unfilledOrder.exchangeCode());
        return cancelOrder(
                exchange,
                unfilledOrder.symbol(),
                unfilledOrder.orderNumber(),
                unfilledOrder.unfilledQuantity().intValue()
        );
    }

    // ==================== Private Methods ====================

    private OverseasOrderResult executeOrder(OverseasOrderRequest request, String trId, boolean isSell) {
        log.info("해외주식 {} 주문 - 거래소: {}, 종목: {}, 수량: {}, 가격: {}",
                isSell ? "매도" : "매수",
                request.getExchange().getCode(),
                request.getSymbol(),
                request.getQuantity(),
                request.getPrice());

        // 계좌 정보 확인
        validateAccountInfo();

        // 요청 바디 생성
        Map<String, String> body = new HashMap<>();
        body.put("CANO", kisProperties.getEffectiveAccountNumber());
        body.put("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode());
        body.put("OVRS_EXCG_CD", getApiExchangeCode(request.getExchange()));
        body.put("PDNO", request.getSymbol());
        body.put("ORD_QTY", String.valueOf(request.getQuantity()));
        body.put("OVRS_ORD_UNPR", request.getPrice().toPlainString());
        body.put("CTAC_TLNO", "");
        body.put("MGCO_APTM_ODNO", "");
        body.put("SLL_TYPE", isSell ? "00" : "");
        body.put("ORD_SVR_DVSN_CD", "0");
        body.put("ORD_DVSN", request.getOrderType().getCode());

        try {
            OrderResponse response = kisWebClient.post()
                    .uri(ORDER_API_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.getAuthorizationHeader())
                    .header("appkey", kisProperties.getEffectiveAppKey())
                    .header("appsecret", kisProperties.getEffectiveAppSecret())
                    .header("tr_id", trId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(OrderResponse.class)
                    .block();

            if (response != null && response.isSuccess()) {
                log.info("해외주식 주문 성공 - 주문번호: {}, 시각: {}",
                        response.output().orderNumber(), response.output().orderTime());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("해외주식 주문 실패 - 오류: {}", errorMsg);
                throw new OverseasOrderException("해외주식 주문 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("해외주식 주문 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OverseasOrderException("해외주식 주문 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof OverseasOrderException) {
                throw e;
            }
            log.error("해외주식 주문 중 예외 발생", e);
            throw new OverseasOrderException("해외주식 주문 중 오류: " + e.getMessage(), e);
        }
    }

    private void validateAccountInfo() {
        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new OverseasOrderException("계좌번호가 설정되지 않았습니다. external.kis.account-number 설정 필요");
        }
        if (kisProperties.getEffectiveAccountProductCode() == null || kisProperties.getEffectiveAccountProductCode().isBlank()) {
            throw new OverseasOrderException("계좌상품코드가 설정되지 않았습니다. external.kis.account-product-code 설정 필요");
        }
    }

    private String getBuyTrId(OverseasExchange exchange) {
        return switch (exchange) {
            case NASDAQ, NYSE, AMEX, NASDAQ_DAY, NYSE_DAY, AMEX_DAY -> TR_US_BUY;
            case HONG_KONG -> TR_HK_BUY;
            case SHANGHAI, SHANGHAI_INDEX -> TR_SH_BUY;
            case SHENZHEN, SHENZHEN_INDEX -> TR_SZ_BUY;
            case TOKYO -> TR_JP_BUY;
            case HO_CHI_MINH, HANOI -> TR_VN_BUY;
        };
    }

    private String getSellTrId(OverseasExchange exchange) {
        return switch (exchange) {
            case NASDAQ, NYSE, AMEX, NASDAQ_DAY, NYSE_DAY, AMEX_DAY -> TR_US_SELL;
            case HONG_KONG -> TR_HK_SELL;
            case SHANGHAI, SHANGHAI_INDEX -> TR_SH_SELL;
            case SHENZHEN, SHENZHEN_INDEX -> TR_SZ_SELL;
            case TOKYO -> TR_JP_SELL;
            case HO_CHI_MINH, HANOI -> TR_VN_SELL;
        };
    }

    /**
     * 모의투자 모드면 TR ID 앞에 V를 붙임
     * 예: TTTT1002U -> VTTT1002U
     */
    private String applyDemoMode(String trId) {
        if (demoMode) {
            return "V" + trId.substring(1);
        }
        return trId;
    }

    private String getApiExchangeCode(OverseasExchange exchange) {
        // API에서 사용하는 거래소 코드
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

    private OverseasExchange parseExchangeCode(String apiExchangeCode) {
        return switch (apiExchangeCode) {
            case "NASD" -> OverseasExchange.NASDAQ;
            case "NYSE" -> OverseasExchange.NYSE;
            case "AMEX" -> OverseasExchange.AMEX;
            case "SEHK" -> OverseasExchange.HONG_KONG;
            case "SHAA" -> OverseasExchange.SHANGHAI;
            case "SZAA" -> OverseasExchange.SHENZHEN;
            case "TKSE" -> OverseasExchange.TOKYO;
            case "VNSE" -> OverseasExchange.HO_CHI_MINH;
            case "HASE" -> OverseasExchange.HANOI;
            default -> throw new IllegalArgumentException("Unknown exchange code: " + apiExchangeCode);
        };
    }

    // ==================== Response Records ====================

    private record OrderResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") OverseasOrderResult output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    private record UnfilledResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") List<OverseasUnfilledOrder> output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    // ==================== Exception ====================

    public static class OverseasOrderException extends RuntimeException {
        public OverseasOrderException(String message) {
            super(message);
        }

        public OverseasOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
