package com.stock.trade.api;

import com.stock.trade.overseas.ForeignMargin;
import com.stock.trade.overseas.OverseasStockBalance;
import com.stock.trade.overseas.OverseasStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 계좌 잔고 조회 API 컨트롤러
 * MCP(Model Context Protocol) 서버로 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountBalanceController {

    private final OverseasStockService overseasStockService;

    /**
     * 해외주식 보유 종목 조회
     *
     * @param exchangeCode 거래소코드 (기본값: NASD - 미국 전체)
     * @param currency 통화코드 (기본값: USD)
     * @return 보유 종목 목록
     */
    @GetMapping("/balance")
    public ResponseEntity<List<OverseasStockBalance>> getBalance(
            @RequestParam(defaultValue = "NASD") String exchangeCode,
            @RequestParam(defaultValue = "USD") String currency
    ) {
        log.info("계좌 잔고 조회 API 호출 - 거래소: {}, 통화: {}", exchangeCode, currency);
        List<OverseasStockBalance> balances = overseasStockService.getBalance(exchangeCode, currency);
        return ResponseEntity.ok(balances);
    }

    /**
     * 특정 종목 보유 여부 조회
     *
     * @param symbol 종목코드 (예: AAPL, QQQ)
     * @return 보유 정보
     */
    @GetMapping("/balance/{symbol}")
    public ResponseEntity<OverseasStockBalance> getBalanceBySymbol(@PathVariable String symbol) {
        log.info("종목 보유 여부 조회 API 호출 - 종목: {}", symbol);
        OverseasStockBalance balance = overseasStockService.getBalanceBySymbol(symbol);
        if (balance != null) {
            return ResponseEntity.ok(balance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 통화별 예수금/출금가능금액 조회
     *
     * @return 통화별 증거금 정보 (KRW, USD 등)
     */
    @GetMapping("/margin")
    public ResponseEntity<List<ForeignMargin>> getForeignMargin() {
        log.info("통화별 증거금 조회 API 호출");
        List<ForeignMargin> margins = overseasStockService.getForeignMargin();
        return ResponseEntity.ok(margins);
    }

    /**
     * 계좌 전체 요약 정보
     *
     * @return 보유 종목, 평가금액, 예수금 등 종합 정보
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAccountSummary() {
        log.info("계좌 요약 정보 조회 API 호출");

        // 보유 종목 조회
        List<OverseasStockBalance> balances = overseasStockService.getBalance();

        // 통화별 증거금 조회
        List<ForeignMargin> margins = overseasStockService.getForeignMargin();

        // 총 평가금액 계산
        BigDecimal totalEvalAmount = balances.stream()
                .map(OverseasStockBalance::evalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 손익금액 계산
        BigDecimal totalProfitLoss = balances.stream()
                .map(OverseasStockBalance::profitLossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 수익 종목 수
        long profitStocksCount = balances.stream()
                .filter(OverseasStockBalance::isProfit)
                .count();

        // 손실 종목 수
        long lossStocksCount = balances.stream()
                .filter(OverseasStockBalance::isLoss)
                .count();

        return ResponseEntity.ok(Map.of(
                "holdings", balances,
                "margins", margins,
                "totalEvalAmount", totalEvalAmount,
                "totalProfitLoss", totalProfitLoss,
                "totalStocksCount", balances.size(),
                "profitStocksCount", profitStocksCount,
                "lossStocksCount", lossStocksCount
        ));
    }
}
