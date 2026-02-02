# Trade App

KIS Open API 기반 해외주식 자동 매매 시스템

## 주요 기능

- **해외주식 조회/주문** - 미국, 홍콩, 중국, 일본, 베트남 주식 지원
- **정기 매수 스케줄러** - 주간/월간 자동 매수
- **리밸런싱** - 수익률 기반 자동 리밸런싱 (예: QLD 25% 수익 시 10% 매도 → JEPQ 매수)
- **미체결 시장가 전환** - 장마감 전 미체결 주문 자동 시장가 전환
- **모의/실전 투자 모드** - 설정으로 간편 전환
- **Slack 알림** - 주문 결과 실시간 알림 (Webhook)

## 기술 스택

- Java 21
- Spring Boot 4.0
- WebFlux (비동기 HTTP)
- WebSocket (실시간 시세)

## 설정

### 환경 변수 (.env)

```bash
# 실전투자
KIS_APP_KEY=your-app-key
KIS_APP_SECRET=your-app-secret
KIS_ACCOUNT_NUMBER=12345678
KIS_ACCOUNT_PRODUCT_CODE=01

# 모의투자
KIS_DEMO_APP_KEY=your-demo-app-key
KIS_DEMO_APP_SECRET=your-demo-app-secret
KIS_DEMO_ACCOUNT_NUMBER=50160641
KIS_DEMO_ACCOUNT_PRODUCT_CODE=01

# Slack 알림
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/xxx/xxx
```

### 투자 모드 전환 (application.yaml)

```yaml
external:
  kis:
    demo-mode: true   # true: 모의투자, false: 실전투자
```

### Slack 알림 설정 (application.yaml)

```yaml
notification:
  slack:
    enabled: true     # Slack 알림 활성화
```

## 실행

```bash
./gradlew bootRun
```

## API 엔드포인트

### 스케줄러 API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/scheduler/config` | 스케줄러 설정 조회 |
| POST | `/api/scheduler/execute/weekly` | 주간 매수 수동 실행 |
| POST | `/api/scheduler/execute/monthly` | 월간 리밸런싱 수동 실행 |
| POST | `/api/scheduler/execute/fallback` | 미체결 시장가 전환 |
| POST | `/api/scheduler/toggle?enabled=true` | 스케줄러 활성화/비활성화 |

### 계좌 잔고 API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/account/balance` | 보유 종목 전체 조회 |
| GET | `/api/account/balance/{symbol}` | 특정 종목 보유 정보 조회 |
| GET | `/api/account/margin` | 통화별 예수금/출금가능금액 조회 |
| GET | `/api/account/summary` | 계좌 전체 요약 정보 |

## MCP (Model Context Protocol) 연동

Claude Desktop 등 MCP를 지원하는 클라이언트에서 계좌 정보를 조회할 수 있습니다.

### MCP 서버 설정

Claude Desktop 설정 파일(`~/Library/Application Support/Claude/claude_desktop_config.json`)에 추가:

```json
{
  "mcpServers": {
    "kis-account": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"],
      "env": {
        "ALLOWED_ORIGINS": "http://localhost:8080",
        "FETCH_GET_ACCOUNT_BALANCE": "http://localhost:8080/api/account/balance",
        "FETCH_GET_STOCK_HOLDING": "http://localhost:8080/api/account/balance/{symbol}",
        "FETCH_GET_FOREIGN_MARGIN": "http://localhost:8080/api/account/margin",
        "FETCH_GET_ACCOUNT_SUMMARY": "http://localhost:8080/api/account/summary"
      }
    }
  }
}
```

### 사용 가능한 MCP 도구

- `get_account_balance` - 보유 종목 조회
- `get_stock_holding` - 특정 종목 보유 여부 확인
- `get_foreign_margin` - 통화별 예수금 조회
- `get_account_summary` - 계좌 전체 요약

## API 사용 예제

### 보유 종목 전체 조회
```bash
curl http://localhost:8080/api/account/balance
```

### 특정 종목 보유 여부 확인 (예: AAPL)
```bash
curl http://localhost:8080/api/account/balance/AAPL
```

### 통화별 예수금 조회
```bash
curl http://localhost:8080/api/account/margin
```

### 계좌 요약 정보
```bash
curl http://localhost:8080/api/account/summary
```

응답 예시:
```json
{
  "holdings": [
    {
      "symbol": "QQQ",
      "quantity": 10,
      "avgBuyPrice": 380.50,
      "currentPrice": 395.20,
      "profitLossAmount": 147.00,
      "profitLossRate": 3.87
    }
  ],
  "margins": [
    {
      "currencyCode": "USD",
      "depositAmount": 5000.00,
      "withdrawableAmount": 4800.00
    }
  ],
  "totalEvalAmount": 3952.00,
  "totalProfitLoss": 147.00,
  "totalStocksCount": 1,
  "profitStocksCount": 1,
  "lossStocksCount": 0
}
```
