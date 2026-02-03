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
import java.util.List;

/**
 * 국내주식 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomesticStockService {

    private final KisProperties kisProperties;
    private final KisTokenManager tokenManager;
    private final WebClient kisWebClient;

    private static final String PRICE_API_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String BALANCE_API_PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
    private static final String PSAMOUNT_API_PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";

    private static final String TR_ID_PRICE = "FHKST01010100";
    private static final String TR_ID_BALANCE = "TTTC8434R";
    private static final String TR_ID_PSAMOUNT = "TTTC8908R";

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
     * 국내주식 현재가 조회
     *
     * @param stockCode 종목코드 (예: "005930")
     * @return 현재가 정보
     */
    public DomesticStockPrice getPrice(String stockCode) {
        log.info("국내주식 현재가 조회 - 종목: {}", stockCode);

        try {
            PriceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PRICE_API_PATH)
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")  // J: 주식, ETF, ETN
                            .queryParam("FID_INPUT_ISCD", stockCode)
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
                log.info("국내주식 현재가 조회 완료 - 종목: {}, 현재가: {}", stockCode, response.output().currentPrice());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 현재가 조회 실패 - 종목: {}, 오류: {}", stockCode, errorMsg);
                throw new DomesticStockException("국내주식 현재가 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 현재가 조회 API 오류 - 상태코드: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticStockException("국내주식 현재가 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticStockException) {
                throw e;
            }
            log.error("국내주식 현재가 조회 중 예외 발생", e);
            throw new DomesticStockException("국내주식 현재가 조회 중 오류: " + e.getMessage(), e);
        }
    }

    // ==================== 잔고 조회 ====================

    /**
     * 국내주식 잔고 조회
     *
     * @return 보유 종목 목록
     */
    public List<DomesticStockBalance> getBalance() {
        log.info("국내주식 잔고 조회 - 모드: {}", demoMode ? "모의투자" : "실전투자");

        // 계좌 정보 확인
        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new DomesticStockException("계좌번호가 설정되지 않았습니다");
        }

        // 모의투자 모드면 TR ID 앞에 V를 붙임
        String trId = demoMode ? "V" + TR_ID_BALANCE.substring(1) : TR_ID_BALANCE;

        try {
            BalanceResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BALANCE_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("AFHR_FLPR_YN", "N")           // 시간외단일가여부
                            .queryParam("OFL_YN", "")                  // 오프라인여부
                            .queryParam("INQR_DVSN", "02")             // 조회구분 (01:대출일별, 02:종목별)
                            .queryParam("UNPR_DVSN", "01")             // 단가구분 (01:기본값)
                            .queryParam("FUND_STTL_ICLD_YN", "N")      // 펀드결제분포함여부
                            .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")  // 융자금액자동상환여부
                            .queryParam("PRCS_DVSN", "00")             // 처리구분 (00:전일매매포함, 01:전일매매미포함)
                            .queryParam("CTX_AREA_FK100", "")
                            .queryParam("CTX_AREA_NK100", "")
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
                List<DomesticStockBalance> balances = response.output1() != null
                        ? response.output1()
                        : Collections.emptyList();
                log.info("국내주식 잔고 조회 완료 - 보유 종목 수: {}", balances.size());
                return balances;
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 잔고 조회 실패 - 오류: {}", errorMsg);
                throw new DomesticStockException("국내주식 잔고 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 잔고 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticStockException("국내주식 잔고 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticStockException) {
                throw e;
            }
            log.error("국내주식 잔고 조회 중 예외 발생", e);
            throw new DomesticStockException("국내주식 잔고 조회 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 종목 보유 여부 확인
     *
     * @param stockCode 종목코드
     * @return 보유 정보 (없으면 null)
     */
    public DomesticStockBalance getBalanceByStockCode(String stockCode) {
        return getBalance().stream()
                .filter(b -> stockCode.equals(b.stockCode()))
                .findFirst()
                .orElse(null);
    }

    // ==================== 매수가능금액 조회 ====================

    /**
     * 국내주식 매수가능금액 조회
     *
     * @param stockCode 종목코드
     * @param price     주문단가 (0이면 현재가 기준)
     * @return 매수가능금액 정보
     */
    public DomesticPurchasableAmount getPurchasableAmount(String stockCode, BigDecimal price) {
        log.info("국내주식 매수가능금액 조회 - 종목: {}, 가격: {}", stockCode, price);

        if (kisProperties.getEffectiveAccountNumber() == null || kisProperties.getEffectiveAccountNumber().isBlank()) {
            throw new DomesticStockException("계좌번호가 설정되지 않았습니다");
        }

        String trId = demoMode ? "V" + TR_ID_PSAMOUNT.substring(1) : TR_ID_PSAMOUNT;

        try {
            PurchasableAmountResponse response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PSAMOUNT_API_PATH)
                            .queryParam("CANO", kisProperties.getEffectiveAccountNumber())
                            .queryParam("ACNT_PRDT_CD", kisProperties.getEffectiveAccountProductCode())
                            .queryParam("PDNO", stockCode)
                            .queryParam("ORD_UNPR", price.toPlainString())
                            .queryParam("ORD_DVSN", "00")              // 주문구분 (00:지정가)
                            .queryParam("CMA_EVLU_AMT_ICLD_YN", "Y")   // CMA평가금액포함여부
                            .queryParam("OVRS_ICLD_YN", "N")           // 해외포함여부
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
                log.info("국내주식 매수가능금액 조회 완료 - 종목: {}, 매수가능수량: {}",
                        stockCode, response.output().maxBuyQuantity());
                return response.output();
            } else {
                String errorMsg = response != null ? response.message() : "응답 없음";
                log.error("국내주식 매수가능금액 조회 실패 - 오류: {}", errorMsg);
                throw new DomesticStockException("국내주식 매수가능금액 조회 실패: " + errorMsg);
            }
        } catch (WebClientResponseException e) {
            log.error("국내주식 매수가능금액 조회 API 오류 - 상태코드: {}, 응답: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new DomesticStockException("국내주식 매수가능금액 조회 API 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof DomesticStockException) {
                throw e;
            }
            log.error("국내주식 매수가능금액 조회 중 예외 발생", e);
            throw new DomesticStockException("국내주식 매수가능금액 조회 중 오류: " + e.getMessage(), e);
        }
    }

    // ==================== Response Records ====================

    /**
     * 현재가 API 응답 래퍼
     */
    private record PriceResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") DomesticStockPrice output
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
            @JsonProperty("output1") List<DomesticStockBalance> output1,  // 보유 종목 목록
            @JsonProperty("output2") List<BalanceSummary> output2         // 계좌 요약
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    /**
     * 잔고 요약 정보
     */
    private record BalanceSummary(
            @JsonProperty("dnca_tot_amt") BigDecimal totalDeposit,          // 예수금총금액
            @JsonProperty("nxdy_excc_amt") BigDecimal nextDaySettlement,    // 익일정산금액
            @JsonProperty("prvs_rcdl_excc_amt") BigDecimal prevReceipt,     // 전일정산금액
            @JsonProperty("tot_evlu_amt") BigDecimal totalEvalAmount,       // 총평가금액
            @JsonProperty("evlu_pfls_smtl_amt") BigDecimal totalProfitLoss  // 평가손익합계금액
    ) {}

    /**
     * 매수가능금액 API 응답 래퍼
     */
    private record PurchasableAmountResponse(
            @JsonProperty("rt_cd") String returnCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") DomesticPurchasableAmount output
    ) {
        boolean isSuccess() {
            return "0".equals(returnCode);
        }
    }

    // ==================== Exception ====================

    /**
     * 국내주식 조회 예외
     */
    public static class DomesticStockException extends RuntimeException {
        public DomesticStockException(String message) {
            super(message);
        }

        public DomesticStockException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
