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

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/scheduler/config` | 스케줄러 설정 조회 |
| POST | `/api/scheduler/execute/weekly` | 주간 매수 수동 실행 |
| POST | `/api/scheduler/execute/monthly` | 월간 리밸런싱 수동 실행 |
| POST | `/api/scheduler/execute/fallback` | 미체결 시장가 전환 |
| POST | `/api/scheduler/toggle?enabled=true` | 스케줄러 활성화/비활성화 |
