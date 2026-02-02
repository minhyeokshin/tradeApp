#!/bin/bash
# Docker 이미지 빌드 및 GitHub Container Registry에 푸시

set -e

# 변수 설정
IMAGE_NAME="ghcr.io/minhyeokshin/tradeapp"
TAG="${1:-latest}"

echo "🔨 Docker 이미지 빌드 시작..."
docker build -t $IMAGE_NAME:$TAG .

echo "✅ 빌드 완료: $IMAGE_NAME:$TAG"

# GitHub Container Registry 로그인 확인
echo "📦 GitHub Container Registry에 푸시..."
docker push $IMAGE_NAME:$TAG

echo "✅ 푸시 완료!"
echo ""
echo "리눅스 서버에서 실행:"
echo "  docker pull $IMAGE_NAME:$TAG"
echo "  docker compose -f docker-compose.prod.yml up -d"
