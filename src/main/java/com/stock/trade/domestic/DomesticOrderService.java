package com.stock.trade.domestic;

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
 * 국내주식 주문 서비스
 * API: /uapi/domestic-stock/v1/trading/order-cash
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomesticOrderService {

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final WebClient kisWebClient;

    private static final String ORDER_API_PATH = "/uapi/domestic-stock/v1/trading/order-cash";
    private static final String UNFILLED_API_PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";
    private static final String CANCEL_API_PATH = "/uapi/domestic-stock/v1/trading/order-rvsecncl";

    // TR ID (실전투자)
    private static final String TR_BUY = "TTTC0802U";
    private static final String TR_SELL = "TTTC0801U";
    private static final String TR_UNFILLED = "TTTC8908R";
    private static final String TR_CANCEL = "TTTC0803U";

    /**
     * 모의투자 여부 (true: 모의투자, false: 실전투자)
     */
    private boolean demoMode = false;

    /**
     * 모의투자 모드 설정
     */
    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
        log.info("국내주식 주문 모드 변경: {}", demoMode ? "모의투자" : "실전투자");
    }

    /**
     * 모의투자 모드 여부
     */
    public boolean isDemoMode() {
        return demoMode;
    }

    // ==================== 매수 주문 ====================

    /**
     * 국내주식 매수 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    public DomesticOrderResult buy(DomesticOrderRequest request) {
        request.validate();
        String trId = applyDemoMode(TR_BUY);
        return executeOrder(request, trId, false);
    }

    /**
     * 국내주식 매수 주문 (간편 버전 - 지정가)
     *
     * @param stockCode 종목코드
     * @param quantity  수량
     * @param price     가격
     * @return 주문 결과
     */
    public DomesticOrderResult buy(String stockCode, int quantity, BigDecimal price) {
        return buy(DomesticOrderRequest.buyLimit(stockCode, quantity, price));
    }

    /**
     * 국내주식 매수 주문 (간편 버전 - 시장가)
     *
     * @param stockCode 종목코드
     * @param quantity  수량
     * @return 주문 결과
     */
    public DomesticOrderResult buyMarket(String stockCode, int quantity) {
        return buy(DomesticOrderRequest.buyMarket(stockCode, quantity));
    }

    // ==================== 매도 주문 ====================

    /**
     * 국내주식 매도 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    public DomesticOrderResult sell(DomesticOrderRequest request) {
        request.validate();
        String trId = applyDemoMode(TR_SELL);
        return executeOrder(request, trId, true);
    }

    /**
     * 국내주식 매도 주문 (간편 버전 - 지정가)
     *
     * @param stockCode 종목코드
     * @param quantity  수량
     * @param price     가격
     * @return 주문 결과
     */
    public DomesticOrderResult sell(String stockCode, int quantity, BigDecimal price) {
        return sell(DomesticOrderRequest.sellLimit(stockCode, quantity, price));
    }

    /**
     * 국내주식 매도 주문 (간편 버전 - 시장가)
     *
     * @param stockCode 종목코드
     * @param quantity  수량
     * @return 주문 결과
     */
    public DomesticOrderResult sellMarket(String stockCode, int quantity) {
        return sell(DomesticOrderRequest.sellMarket(stockCode, quantity));
    }

    // ==================== 미체결 조회 ====================

    /**
     * 국내주식 미체결 내역 조회
     *
     * @return 미체결 주문 목록
     */
    public List<DomesticUnfilledOrder> getUnfilledOrders() {
        log.info("국내주식 미체결 조회");

        validateAccountInfo();

        String trId = applyDemoMode(TR_UNFILLED);

        try {
            UnfilledResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(UNFILLED_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("INQR_DVSN_1", "0")            // 조회구분1 (0:전체, 1:매도, 2:매수)
                            .queryParam("INQR_DVSN_2", "0")            // 조회구분2 (0:전체)
                            .queryParam("CTX_AREA_FK100", "")
                            .queryParam("CTX_AREA_NK100", "")
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
                List<DomesticUnfilledOrder> orders = response.output() != null
                        ? response.output()
                        : Collections.emptyList();
                log.info("국내주식 미체결 조회 완료 - {}건", orders.size());
                return orders;
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 미체결 조회 실패 - 오류: {}", errorMsg);
                throw new DomesticOrderException("국내주식 미체결 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 미체결 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticOrderException("국내주식 미체결 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticOrderException) {
                throw e;
            }
            log.error("국내주식 미체결 조회 중 예외 발생", e);
            throw new DomesticOrderException("국내주식 미체결 조회 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 종목의 미체결 주문 조회
     *
     * @param stockCode 종목코드
     * @return 해당 종목의 미체결 주문 목록
     */
    public List<DomesticUnfilledOrder> getUnfilledOrdersByStockCode(String stockCode) {
        return getUnfilledOrders().stream()
                .filter(order -> stockCode.equals(order.stockCode()))
                .toList();
    }

    // ==================== 주문 취소 ====================

    /**
     * 국내주식 주문 취소
     *
     * @param orderNumber 원주문번호
     * @param quantity    취소수량 (전량 취소 시 0)
     * @return 취소 결과
     */
    public DomesticOrderResult cancelOrder(String orderNumber, int quantity) {
        log.info("국내주식 주문 취소 - 주문번호: {}, 수량: {}", orderNumber, quantity);

        validateAccountInfo();

        String trId = applyDemoMode(TR_CANCEL);

        Map<String, String> body = new HashMap<>();
        body.put("CANO", kisProperties.getEffectiveAccountNumber());
        body.put("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode());
        body.put("KRX_FWDG_ORD_ORGNO", "");
        body.put("ORGN_ODNO", orderNumber);
        body.put("ORD_DVSN", "00");                    // 주문구분 (취소 시 00)
        body.put("RVSE_CNCL_DVSN_CD", "02");          // 정정취소구분 (02:취소)
        body.put("ORD_QTY", String.valueOf(quantity)); // 0이면 전량취소
        body.put("ORD_UNPR", "0");                     // 취소 시 0
        body.put("QTY_ALL_ORD_YN", quantity == 0 ? "Y" : "N"); // 전량주문여부

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
                log.info("국내주식 주문 취소 성공 - 주문번호: {}", response.output().orderNumber());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 주문 취소 실패 - 오류: {}", errorMsg);
                throw new DomesticOrderException("국내주식 주문 취소 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 주문 취소 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticOrderException("국내주식 주문 취소 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticOrderException) {
                throw e;
            }
            log.error("국내주식 주문 취소 중 예외 발생", e);
            throw new DomesticOrderException("국내주식 주문 취소 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 미체결 주문 취소
     *
     * @param unfilledOrder 미체결 주문
     * @return 취소 결과
     */
    public DomesticOrderResult cancelOrder(DomesticUnfilledOrder unfilledOrder) {
        return cancelOrder(
                unfilledOrder.orderNumber(),
                unfilledOrder.unfilledQuantity().intValue()
        );
    }

    // ==================== Private Methods ====================

    private DomesticOrderResult executeOrder(DomesticOrderRequest request, String trId, boolean isSell) {
        log.info("국내주식 {} 주문 - 종목: {}, 수량: {}, 가격: {}, 유형: {}",
                isSell ? "매도" : "매수",
                request.getStockCode(),
                request.getQuantity(),
                request.getPrice(),
                request.getOrderType().getDescription());

        // 계좌 정보 확인
        validateAccountInfo();

        // 요청 바디 생성
        Map<String, String> body = new HashMap<>();
        body.put("CANO", kisProperties.getEffectiveAccountNumber());
        body.put("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode());
        body.put("PDNO", request.getStockCode());
        body.put("ORD_DVSN", request.getOrderType().getCode());
        body.put("ORD_QTY", String.valueOf(request.getQuantity()));
        body.put("ORD_UNPR", request.getPrice().toPlainString());

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
                log.info("국내주식 주문 성공 - 주문번호: {}, 시각: {}",
                        response.output().orderNumber(), response.output().orderTime());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 주문 실패 - 오류: {}", errorMsg);
                throw new DomesticOrderException("국내주식 주문 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 주문 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticOrderException("국내주식 주문 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticOrderException) {
                throw e;
            }
            log.error("국내주식 주문 중 예외 발생", e);
            throw new DomesticOrderException("국내주식 주문 중 오류: " + e.getMessage(), e);
        }
    }

    private void validateAccountInfo() {
        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new DomesticOrderException("계좌번호가 설정되지 않았습니다. external.kis.account-number 설정 필요");
        }
        if (kisProperties.getEffectiveAccountProductCode() == null || kisProperties.getEffectiveAccountProductCode().isBlank()) {
            throw new DomesticOrderException("계좌상품코드가 설정되지 않았습니다. external.kis.account-product-code 설정 필요");
        }
    }

    /**
     * 모의투자 모드면 TR ID 앞에 V를 붙임
     * 예: TTTC0802U -> VTTC0802U
     */
    private String applyDemoMode(String trId) {
        if (demoMode) {
            return "V" + trId.substring(1);
        }
        return trId;
    }

    // ==================== Response Records ====================

    private record OrderResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") DomesticOrderResult output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    private record UnfilledResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") List<DomesticUnfilledOrder> output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    // ==================== Exception ====================

    public static class DomesticOrderException extends RuntimeException {
        public DomesticOrderException(String message) {
            super(message);
        }

        public DomesticOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
