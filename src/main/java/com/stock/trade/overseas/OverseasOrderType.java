package com.stock.trade.overseas;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 해외주식 주문 유형
 */
@Getter
@RequiredArgsConstructor
public enum OverseasOrderType {

    /**
     * 지정가 주문
     */
    LIMIT("00", "지정가"),

    /**
     * 장개시시장가 (MOO - Market On Open) - 매도만 가능
     */
    MARKET_ON_OPEN("31", "장개시시장가"),

    /**
     * 장개시지정가 (LOO - Limit On Open)
     */
    LIMIT_ON_OPEN("32", "장개시지정가"),

    /**
     * 장마감시장가 (MOC - Market On Close) - 매도만 가능
     */
    MARKET_ON_CLOSE("33", "장마감시장가"),

    /**
     * 장마감지정가 (LOC - Limit On Close)
     */
    LIMIT_ON_CLOSE("34", "장마감지정가");

    private final String code;
    private final String description;
}
