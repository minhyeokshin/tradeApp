package com.stock.trade.websocket;

/**
 * KIS 실시간 데이터 TR ID 상수
 */
public final class KisTrId {

    private KisTrId() {
    }

    // ==================== 국내주식 실시간시세 ====================

    /**
     * 실시간 체결가 (통합)
     */
    public static final String STOCK_CCNL_TOTAL = "H0UNCNT0";

    /**
     * 실시간 체결가 (KRX)
     */
    public static final String STOCK_CCNL_KRX = "H0STCNT0";

    /**
     * 실시간 체결가 (NXT)
     */
    public static final String STOCK_CCNL_NXT = "H0NXCNT0";

    /**
     * 실시간 호가 (통합)
     */
    public static final String STOCK_ASKING_PRICE_TOTAL = "H0UNORP0";

    /**
     * 실시간 호가 (KRX)
     */
    public static final String STOCK_ASKING_PRICE_KRX = "H0STASP0";

    /**
     * 실시간 호가 (NXT)
     */
    public static final String STOCK_ASKING_PRICE_NXT = "H0NXASP0";

    /**
     * 실시간 체결통보 (실전투자)
     */
    public static final String STOCK_CCNL_NOTICE = "H0STCNI0";

    /**
     * 실시간 체결통보 (모의투자)
     */
    public static final String STOCK_CCNL_NOTICE_DEMO = "H0STCNI9";

    /**
     * 실시간 예상체결 (KRX)
     */
    public static final String STOCK_EXP_CCNL_KRX = "H0STCNT7";

    // ==================== 국내지수 ====================

    /**
     * 국내지수 실시간체결
     */
    public static final String INDEX_CCNL = "H0UPCNT0";

    /**
     * 국내지수 실시간예상체결
     */
    public static final String INDEX_EXP_CCNL = "H0UPCNT3";

    // ==================== 장운영정보 ====================

    /**
     * 장운영정보 (KRX)
     */
    public static final String MARKET_STATUS_KRX = "H0STMNI0";

    /**
     * 장운영정보 (통합)
     */
    public static final String MARKET_STATUS_TOTAL = "H0UNMNI0";

    // ==================== 프로그램매매 ====================

    /**
     * 실시간 프로그램매매 (KRX)
     */
    public static final String PROGRAM_TRADE_KRX = "H0STPGM0";

    /**
     * 실시간 프로그램매매 (통합)
     */
    public static final String PROGRAM_TRADE_TOTAL = "H0UNPGM0";

    // ==================== 해외주식 실시간시세 ====================

    /**
     * 해외주식 실시간지연체결가
     * - 미국: 실시간무료(0분지연)
     * - 홍콩/베트남/중국/일본: 15분지연
     * - tr_key 형식: D + 거래소코드 + 종목코드 (예: DNASAAPL)
     */
    public static final String OVERSEAS_STOCK_DELAYED_CCNL = "HDFSCNT0";

    /**
     * 해외주식 실시간호가 (미국)
     * - tr_key 형식: D + 거래소코드 + 종목코드 (예: DNASAAPL)
     */
    public static final String OVERSEAS_STOCK_ASKING_PRICE = "HDFSASP0";

    /**
     * 해외주식 실시간지연호가 (아시아)
     * - tr_key 형식: D + 거래소코드 + 종목코드
     */
    public static final String OVERSEAS_STOCK_DELAYED_ASKING_PRICE_ASIA = "HDFSASP1";

    /**
     * 해외주식 실시간체결통보 (실전투자)
     */
    public static final String OVERSEAS_STOCK_CCNL_NOTICE = "H0GSCNI0";

    /**
     * 해외주식 실시간체결통보 (모의투자)
     */
    public static final String OVERSEAS_STOCK_CCNL_NOTICE_DEMO = "H0GSCNI9";

    // ==================== 해외주식 거래소코드 (tr_key 접두사) ====================

    /**
     * 나스닥 접두사 (예: DNASAAPL)
     */
    public static final String EXCD_NASDAQ = "DNAS";

    /**
     * 뉴욕 접두사 (예: DNYSAMZN)
     */
    public static final String EXCD_NYSE = "DNYS";

    /**
     * 아멕스 접두사
     */
    public static final String EXCD_AMEX = "DAMS";

    /**
     * 홍콩 접두사
     */
    public static final String EXCD_HONGKONG = "DHKS";

    /**
     * 상해 접두사
     */
    public static final String EXCD_SHANGHAI = "DSHS";

    /**
     * 심천 접두사
     */
    public static final String EXCD_SHENZHEN = "DSZS";

    /**
     * 도쿄 접두사
     */
    public static final String EXCD_TOKYO = "DTSE";

    /**
     * 해외주식 tr_key 생성 헬퍼
     * @param exchangePrefix 거래소 접두사 (예: DNAS)
     * @param symbol 종목코드 (예: AAPL)
     * @return tr_key (예: DNASAAPL)
     */
    public static String overseasTrKey(String exchangePrefix, String symbol) {
        return exchangePrefix + symbol;
    }
}
