package com.stock.trade.overseas;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 해외 거래소 코드
 */
@Getter
@RequiredArgsConstructor
public enum OverseasExchange {

    // 미국
    NASDAQ("NAS", "나스닥"),
    NYSE("NYS", "뉴욕"),
    AMEX("AMS", "아멕스"),

    // 미국 주간거래
    NASDAQ_DAY("BAQ", "나스닥(주간)"),
    NYSE_DAY("BAY", "뉴욕(주간)"),
    AMEX_DAY("BAA", "아멕스(주간)"),

    // 아시아
    HONG_KONG("HKS", "홍콩"),
    TOKYO("TSE", "도쿄"),
    SHANGHAI("SHS", "상해"),
    SHENZHEN("SZS", "심천"),

    // 지수
    SHANGHAI_INDEX("SHI", "상해지수"),
    SHENZHEN_INDEX("SZI", "심천지수"),

    // 베트남
    HO_CHI_MINH("HSX", "호치민"),
    HANOI("HNX", "하노이");

    private final String code;
    private final String description;

    /**
     * 거래소 코드로 Enum 찾기
     */
    public static OverseasExchange fromCode(String code) {
        for (OverseasExchange exchange : values()) {
            if (exchange.code.equals(code)) {
                return exchange;
            }
        }
        throw new IllegalArgumentException("Unknown exchange code: " + code);
    }
}
