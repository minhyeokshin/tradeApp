package com.stock.trade.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stock.trade.config.KisProperties;
import com.stock.trade.domestic.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 국내주식 API 컨트롤러
 * MCP(Model Context Protocol) 서버로 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/domestic")
@RequiredArgsConstructor
public class DomesticStockController {

    private final DomesticStockService domesticStockService;
    private final DomesticOrderService domesticOrderService;
    private final KisProperties kisProperties;

    // ==================== 조회 API ====================

    /**
     * 국내주식 현재가 조회
     *
     * @param stockCode 종목코드 (예: 005930)
     * @return 현재가 정보
     */
    @GetMapping("/price/{stockCode}")
    public ResponseEntity<DomesticStockPrice> getPrice(@PathVariable String stockCode) {
        log.info("국내주식 현재가 조회 API 호출 - 종목: {}", stockCode);

        DomesticStockPrice price = domesticStockService.getPrice(stockCode);
        return ResponseEntity.ok(price);
    }

    /**
     * 국내주식 보유 종목 조회
     *
     * @return 보유 종목 목록
     */
    @GetMapping("/balance")
    public ResponseEntity<List<DomesticStockBalance>> getBalance() {
        log.info("국내주식 잔고 조회 API 호출 - 모드: {}",
                kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticStockService.setDemoMode(kisProperties.isDemoMode());

        List<DomesticStockBalance> balances = domesticStockService.getBalance();
        return ResponseEntity.ok(balances);
    }

    /**
     * 특정 종목 보유 여부 조회
     *
     * @param stockCode 종목코드 (예: 005930)
     * @return 보유 정보
     */
    @GetMapping("/balance/{stockCode}")
    public ResponseEntity<DomesticStockBalance> getBalanceByStockCode(@PathVariable String stockCode) {
        log.info("국내주식 종목 보유 여부 조회 API 호출 - 종목: {}, 모드: {}",
                stockCode, kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticStockService.setDemoMode(kisProperties.isDemoMode());

        DomesticStockBalance balance = domesticStockService.getBalanceByStockCode(stockCode);
        if (balance != null) {
            return ResponseEntity.ok(balance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 매수가능금액 조회
     *
     * @param stockCode 종목코드 (예: 005930)
     * @param price     주문단가 (옵션, 기본값: 0 - 현재가 기준)
     * @return 매수가능금액 정보
     */
    @GetMapping("/purchasable")
    public ResponseEntity<DomesticPurchasableAmount> getPurchasableAmount(
            @RequestParam String stockCode,
            @RequestParam(defaultValue = "0") BigDecimal price
    ) {
        log.info("국내주식 매수가능금액 조회 API 호출 - 종목: {}, 가격: {}, 모드: {}",
                stockCode, price, kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticStockService.setDemoMode(kisProperties.isDemoMode());

        DomesticPurchasableAmount amount = domesticStockService.getPurchasableAmount(stockCode, price);
        return ResponseEntity.ok(amount);
    }

    /**
     * 계좌 전체 요약 정보
     *
     * @return 보유 종목, 평가금액, 손익 등 종합 정보
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAccountSummary() {
        log.info("국내주식 계좌 요약 정보 조회 API 호출 - 모드: {}",
                kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticStockService.setDemoMode(kisProperties.isDemoMode());

        // 보유 종목 조회
        List<DomesticStockBalance> balances = domesticStockService.getBalance();

        // 총 평가금액 계산
        BigDecimal totalEvalAmount = balances.stream()
                .map(DomesticStockBalance::evalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 손익금액 계산
        BigDecimal totalProfitLoss = balances.stream()
                .map(DomesticStockBalance::profitLossAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 매입금액 계산
        BigDecimal totalBuyAmount = balances.stream()
                .map(DomesticStockBalance::buyAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 수익 종목 수
        long profitStocksCount = balances.stream()
                .filter(DomesticStockBalance::isProfit)
                .count();

        // 손실 종목 수
        long lossStocksCount = balances.stream()
                .filter(DomesticStockBalance::isLoss)
                .count();

        return ResponseEntity.ok(Map.of(
                "holdings", balances,
                "totalEvalAmount", totalEvalAmount,
                "totalBuyAmount", totalBuyAmount,
                "totalProfitLoss", totalProfitLoss,
                "totalStocksCount", balances.size(),
                "profitStocksCount", profitStocksCount,
                "lossStocksCount", lossStocksCount
        ));
    }

    // ==================== 주문 API ====================

    /**
     * 매수 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    @PostMapping("/order/buy")
    public ResponseEntity<DomesticOrderResult> buy(@RequestBody OrderRequestDto request) {
        log.info("국내주식 매수 주문 API 호출 - 종목: {}, 수량: {}, 가격: {}, 주문유형: {}, 모드: {}",
                request.stockCode(), request.quantity(), request.price(),
                request.orderType(), kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticOrderService.setDemoMode(kisProperties.isDemoMode());

        DomesticOrderRequest orderRequest = DomesticOrderRequest.builder()
                .stockCode(request.stockCode())
                .quantity(request.quantity())
                .price(request.price() != null ? request.price() : BigDecimal.ZERO)
                .orderType(request.orderType() != null ? request.orderType() : DomesticOrderType.LIMIT)
                .build();

        DomesticOrderResult result = domesticOrderService.buy(orderRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * 매도 주문
     *
     * @param request 주문 요청
     * @return 주문 결과
     */
    @PostMapping("/order/sell")
    public ResponseEntity<DomesticOrderResult> sell(@RequestBody OrderRequestDto request) {
        log.info("국내주식 매도 주문 API 호출 - 종목: {}, 수량: {}, 가격: {}, 주문유형: {}, 모드: {}",
                request.stockCode(), request.quantity(), request.price(),
                request.orderType(), kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticOrderService.setDemoMode(kisProperties.isDemoMode());

        DomesticOrderRequest orderRequest = DomesticOrderRequest.builder()
                .stockCode(request.stockCode())
                .quantity(request.quantity())
                .price(request.price() != null ? request.price() : BigDecimal.ZERO)
                .orderType(request.orderType() != null ? request.orderType() : DomesticOrderType.LIMIT)
                .build();

        DomesticOrderResult result = domesticOrderService.sell(orderRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * 미체결 주문 조회
     *
     * @return 미체결 주문 목록
     */
    @GetMapping("/order/unfilled")
    public ResponseEntity<List<DomesticUnfilledOrder>> getUnfilledOrders() {
        log.info("국내주식 미체결 조회 API 호출 - 모드: {}",
                kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticOrderService.setDemoMode(kisProperties.isDemoMode());

        List<DomesticUnfilledOrder> orders = domesticOrderService.getUnfilledOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * 주문 취소
     *
     * @param request 취소 요청
     * @return 취소 결과
     */
    @PostMapping("/order/cancel")
    public ResponseEntity<DomesticOrderResult> cancelOrder(@RequestBody CancelRequestDto request) {
        log.info("국내주식 주문 취소 API 호출 - 주문번호: {}, 수량: {}, 모드: {}",
                request.orderNumber(), request.quantity(),
                kisProperties.isDemoMode() ? "모의투자" : "실전투자");

        // 모의투자/실전투자 모드 설정
        domesticOrderService.setDemoMode(kisProperties.isDemoMode());

        DomesticOrderResult result = domesticOrderService.cancelOrder(
                request.orderNumber(),
                request.quantity() != null ? request.quantity() : 0
        );
        return ResponseEntity.ok(result);
    }

    // ==================== Request DTOs ====================

    /**
     * 주문 요청 DTO
     */
    public record OrderRequestDto(
            @JsonProperty("stockCode") String stockCode,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("orderType") DomesticOrderType orderType
    ) {}

    /**
     * 취소 요청 DTO
     */
    public record CancelRequestDto(
            @JsonProperty("orderNumber") String orderNumber,
            @JsonProperty("quantity") Integer quantity  // null이면 전량 취소
    ) {}
}
