#!/bin/bash
# docker 재시작
cd /home/ubuntu/tradeApp

docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

echo "docker 재시작 완료."
