package com.stock.trade.domestic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 국내주식 주문 유형
 */
@Getter
@RequiredArgsConstructor
public enum DomesticOrderType {

    /**
     * 지정가 주문
     */
    LIMIT("00", "지정가"),

    /**
     * 시장가 주문
     */
    MARKET("01", "시장가"),

    /**
     * 조건부지정가
     */
    CONDITIONAL_LIMIT("02", "조건부지정가"),

    /**
     * 최유리지정가
     */
    BEST_LIMIT("03", "최유리지정가"),

    /**
     * 최우선지정가
     */
    PRIORITY_LIMIT("04", "최우선지정가"),

    /**
     * 장전 시간외
     */
    PRE_MARKET("05", "장전시간외"),

    /**
     * 장후 시간외
     */
    AFTER_MARKET("06", "장후시간외"),

    /**
     * 시간외 단일가
     */
    OFF_HOURS_SINGLE("07", "시간외단일가");

    private final String code;
    private final String description;
}
