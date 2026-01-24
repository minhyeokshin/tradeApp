package com.stock.trade.overseas;

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
class OverseasStockServiceTest {

    @Autowired
    private OverseasStockService overseasStockService;

    @Test
    @DisplayName("나스닥 - 애플(AAPL) 현재가 조회")
    void getPrice_AAPL() {
        // when
        OverseasStockPrice price = overseasStockService.getPrice(OverseasExchange.NASDAQ, "AAPL");

        // then
        assertThat(price).isNotNull();
        assertThat(price.currentPrice()).isNotNull();
        assertThat(price.realtimeSymbol()).contains("AAPL");

        log.info("===== 애플(AAPL) 현재가 정보 =====");
        log.info("종목코드: {}", price.realtimeSymbol());
        log.info("현재가: ${}", price.currentPrice());
        log.info("전일종가: ${}", price.previousClose());
        log.info("대비: ${} ({}%)", price.change(), price.changeRate());
        log.info("거래량: {}", price.volume());
        log.info("매수가능: {}", price.isTradable() ? "Y" : "N");
    }

    @Test
    @DisplayName("나스닥 - 테슬라(TSLA) 현재가 조회")
    void getPrice_TSLA() {
        // when
        OverseasStockPrice price = overseasStockService.getPrice(OverseasExchange.NASDAQ, "TSLA");

        // then
        assertThat(price).isNotNull();
        assertThat(price.currentPrice()).isNotNull();

        log.info("===== 테슬라(TSLA) 현재가 정보 =====");
        log.info("종목코드: {}", price.realtimeSymbol());
        log.info("현재가: ${}", price.currentPrice());
        log.info("대비: ${} ({}%)", price.change(), price.changeRate());
        log.info("상승/하락: {}", price.isUp() ? "상승" : (price.isDown() ? "하락" : "보합"));
    }

    @Test
    @DisplayName("뉴욕 - 마이크로소프트(MSFT) 현재가 조회")
    void getPrice_MSFT() {
        // when
        OverseasStockPrice price = overseasStockService.getPrice(OverseasExchange.NASDAQ, "MSFT");

        // then
        assertThat(price).isNotNull();
        assertThat(price.currentPrice()).isNotNull();

        log.info("===== 마이크로소프트(MSFT) 현재가 정보 =====");
        log.info("종목코드: {}", price.realtimeSymbol());
        log.info("현재가: ${}", price.currentPrice());
        log.info("거래대금: ${}", price.tradingAmount());
    }

    @Test
    @DisplayName("나스닥 - 엔비디아(NVDA) 현재가 조회")
    void getPrice_NVDA() {
        // when
        OverseasStockPrice price = overseasStockService.getPrice(OverseasExchange.NASDAQ, "NVDA");

        // then
        assertThat(price).isNotNull();
        assertThat(price.currentPrice()).isNotNull();

        log.info("===== 엔비디아(NVDA) 현재가 정보 =====");
        log.info("종목코드: {}", price.realtimeSymbol());
        log.info("현재가: ${}", price.currentPrice());
        log.info("전일거래량: {}", price.previousVolume());
        log.info("금일거래량: {}", price.volume());
    }

    @Test
    @DisplayName("거래소 코드 문자열로 조회")
    void getPrice_byExchangeCode() {
        // when
        OverseasStockPrice price = overseasStockService.getPrice("NAS", "GOOGL");

        // then
        assertThat(price).isNotNull();
        assertThat(price.currentPrice()).isNotNull();

        log.info("===== 구글(GOOGL) 현재가 정보 =====");
        log.info("종목코드: {}", price.realtimeSymbol());
        log.info("현재가: ${}", price.currentPrice());
    }
}
