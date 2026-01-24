package com.stock.trade;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		properties = {
				"external.kis.app-key=PS8Sx5ikDMOeWrvOSzbMHB9CZ7oAtcs46JKP",
				"external.kis.app-secret=5zzUnS2PL2SQD5HtXRaC1JQkm4op0GxENDgEh1To5M+vH0mivPLJE0KlW5waT7Q4jj6+5m3v0+u4FbvEut9Gv6bhKA+u3N+2VfGbYns1LQ4i7rQA/M6qMn8+YGLvAT8M8G13XkIdNWS0HPz9ZNGSC6i91NHCRMdS9cWA7BRXFueJ/IDqHYY=",
				"external.kis.base-url=https://openapi.koreainvestment.com:9443"
		}
)
class TradeApplicationTests {

	@Test
	void contextLoads() {
	}

}
