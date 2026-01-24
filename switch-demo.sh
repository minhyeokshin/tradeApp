#!/bin/bash
# 모의투자 모드로 전환
cd /home/ubuntu/tradeApp

cat > .env << 'EOF'
# 투자 모드
KIS_DEMO_MODE=true

# 모의투자 인증
KIS_DEMO_APP_KEY=your_demo_app_key
KIS_DEMO_APP_SECRET=your_demo_app_secret
KIS_DEMO_ACCOUNT_NUMBER=12345678
KIS_DEMO_ACCOUNT_PRODUCT_CODE=01

# 실전투자 인증 (모의투자에서는 사용 안함)
KIS_APP_KEY=
KIS_APP_SECRET=
KIS_ACCOUNT_NUMBER=
KIS_ACCOUNT_PRODUCT_CODE=01

# Slack 알림
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/xxx/xxx
EOF

echo "모의투자 모드로 전환되었습니다."
echo "docker compose -f docker-compose.prod.yml up -d 로 재시작하세요."
