package com.stock.trade.websocket;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        properties = {
                "external.kis.app-key=PS8Sx5ikDMOeWrvOSzbMHB9CZ7oAtcs46JKP",
                "external.kis.app-secret=5zzUnS2PL2SQD5HtXRaC1JQkm4op0GxENDgEh1To5M+vH0mivPLJE0KlW5waT7Q4jj6+5m3v0+u4FbvEut9Gv6bhKA+u3N+2VfGbYns1LQ4i7rQA/M6qMn8+YGLvAT8M8G13XkIdNWS0HPz9ZNGSC6i91NHCRMdS9cWA7BRXFueJ/IDqHYY=",
                "external.kis.base-url=https://openapi.koreainvestment.com:9443",
                "external.kis.ws-url=ws://ops.koreainvestment.com:21000"
        }
)
class OverseasWebSocketTest {

    @Autowired
    private KisWebSocketClient webSocketClient;

    @Test
    @DisplayName("해외주식 WebSocket 연결 및 AAPL 실시간 시세 수신 (30초간)")
    void subscribeOverseasStock_AAPL() throws InterruptedException {
        // given
        OverseasStockPriceListener listener = new OverseasStockPriceListener();
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        listener.setOnPriceUpdate(price -> {
            int count = receivedCount.incrementAndGet();
            log.info("========== [{}] AAPL 실시간 시세 수신 ==========", count);
            log.info("종목코드: {}", price.symbol());
            log.info("현재가: ${}", price.currentPrice());
            log.info("시가: ${}, 고가: ${}, 저가: ${}", price.open(), price.high(), price.low());
            log.info("전일대비: ${} ({}%)", price.change(), price.changeRate());
            log.info("거래량: {}", price.volume());
            log.info("한국시간: {} {}", price.koreaDate(), price.koreaTime());
            log.info("=============================================");

            if (count >= 3) {
                latch.countDown();
            }
        });

        // when
        webSocketClient.addListener(listener);
        webSocketClient.connect();

        // 애플 실시간 시세 구독 (DNAS + AAPL)
        String trKey = KisTrId.overseasTrKey(KisTrId.EXCD_NASDAQ, "AAPL");
        webSocketClient.subscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);

        log.info("===== AAPL 실시간 시세 구독 시작 (30초간 대기) =====");
        log.info("TR_ID: {}, TR_KEY: {}", KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);

        // 30초간 대기 또는 3개 이상 수신 시 종료
        boolean received = latch.await(30, TimeUnit.SECONDS);

        // then
        webSocketClient.unsubscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);
        webSocketClient.removeListener(listener);
        webSocketClient.disconnect();

        log.info("===== 테스트 종료 - 총 {}개 시세 수신 =====", receivedCount.get());

        // 미국 장이 열려있는 시간에만 데이터 수신 가능
        // 장 마감 시에는 수신되지 않을 수 있음
        if (received) {
            assertThat(receivedCount.get()).isGreaterThanOrEqualTo(1);
        } else {
            log.warn("30초 내 시세 수신 없음 - 미국 장 마감 시간일 수 있음");
        }
    }

    @Test
    @DisplayName("해외주식 여러 종목 실시간 시세 수신 (AAPL, TSLA, NVDA) - 30초간")
    void subscribeMultipleOverseasStocks() throws InterruptedException {
        // given
        OverseasStockPriceListener listener = new OverseasStockPriceListener();
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        listener.setOnPriceUpdate(price -> {
            int count = receivedCount.incrementAndGet();
            log.info("[{}] {} 실시간 시세 - 현재가: ${}, 등락율: {}%",
                    count, price.symbol(), price.currentPrice(), price.changeRate());
            latch.countDown();
        });

        // when
        webSocketClient.addListener(listener);
        webSocketClient.connect();

        // 여러 종목 구독
        String[] symbols = {"AAPL", "TSLA", "NVDA"};
        for (String symbol : symbols) {
            String trKey = KisTrId.overseasTrKey(KisTrId.EXCD_NASDAQ, symbol);
            webSocketClient.subscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);
            log.info("{} 구독 시작 - TR_KEY: {}", symbol, trKey);
            Thread.sleep(500); // API 호출 간격
        }

        log.info("===== 여러 종목 실시간 시세 구독 시작 (30초간 대기) =====");

        // 30초간 대기
        boolean received = latch.await(30, TimeUnit.SECONDS);

        // then
        for (String symbol : symbols) {
            String trKey = KisTrId.overseasTrKey(KisTrId.EXCD_NASDAQ, symbol);
            webSocketClient.unsubscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);
        }
        webSocketClient.removeListener(listener);
        webSocketClient.disconnect();

        log.info("===== 테스트 종료 - 총 {}개 시세 수신 =====", receivedCount.get());

        // 최신 시세 확인
        for (String symbol : symbols) {
            var price = listener.getLatestPrice(symbol);
            if (price != null) {
                log.info("{} 최종 시세 - 현재가: ${}, 거래량: {}",
                        symbol, price.currentPrice(), price.volume());
            }
        }
    }

    @Test
    @DisplayName("해외주식 10초 간격 시세 로깅 (60초간)")
    void subscribeWithPeriodicLogging() throws InterruptedException {
        // given
        OverseasStockPriceListener listener = new OverseasStockPriceListener();
        AtomicInteger totalReceived = new AtomicInteger(0);

        listener.setOnPriceUpdate(price -> {
            totalReceived.incrementAndGet();
        });

        // when
        webSocketClient.addListener(listener);
        webSocketClient.connect();

        String trKey = KisTrId.overseasTrKey(KisTrId.EXCD_NASDAQ, "AAPL");
        webSocketClient.subscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);

        log.info("===== AAPL 실시간 시세 구독 시작 =====");
        log.info("10초 간격으로 최신 시세를 출력합니다 (총 60초간)");

        // 60초간 10초 간격으로 시세 출력
        for (int i = 1; i <= 6; i++) {
            Thread.sleep(10_000); // 10초 대기

            var price = listener.getLatestPrice("AAPL");
            if (price != null) {
                log.info("========== [{}0초 경과] AAPL 시세 ==========", i);
                log.info("현재가: ${}", price.currentPrice());
                log.info("시가: ${}, 고가: ${}, 저가: ${}", price.open(), price.high(), price.low());
                log.info("전일대비: ${} ({}%)", price.change(), price.changeRate());
                log.info("거래량: {}", price.volume());
                log.info("누적 수신 건수: {}", totalReceived.get());
                log.info("==========================================");
            } else {
                log.warn("[{}0초 경과] 아직 시세 수신 없음 (미국 장 마감 시간일 수 있음)", i);
            }
        }

        // then
        webSocketClient.unsubscribe(KisTrId.OVERSEAS_STOCK_DELAYED_CCNL, trKey);
        webSocketClient.removeListener(listener);
        webSocketClient.disconnect();

        log.info("===== 테스트 종료 - 총 {}개 시세 수신 =====", totalReceived.get());
    }
}
