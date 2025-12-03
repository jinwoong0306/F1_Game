#!/bin/bash
# F1 Racing Game - 연구실 서버 배포 스크립트

SERVER_HOST="203.234.62.35"
SERVER_USER="your_username"  # 연구실 서버 사용자명으로 변경
DEPLOY_PATH="/home/$SERVER_USER/f1-server"

echo "================================================"
echo "F1 Racing Server - 연구실 서버 배포"
echo "================================================"
echo "서버: $SERVER_HOST"
echo "배포 경로: $DEPLOY_PATH"
echo ""

# 1. 서버 빌드
echo "[1/4] 서버 빌드 중..."
./gradlew server:build || { echo "빌드 실패"; exit 1; }

# 2. SSH로 서버 접속 후 디렉토리 생성
echo "[2/4] 서버 디렉토리 생성..."
ssh $SERVER_USER@$SERVER_HOST "mkdir -p $DEPLOY_PATH"

# 3. 빌드된 jar 파일 전송
echo "[3/4] 서버 파일 전송 중..."
scp server/build/libs/server-*.jar $SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/server.jar

# 4. 서버 실행 스크립트 전송
cat > /tmp/run_server.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
echo "F1 Racing Server 시작 중..."
echo "포트: TCP 54555, UDP 54777"
java -server -Xms256m -Xmx512m -jar server.jar 54555 54777
EOF

scp /tmp/run_server.sh $SERVER_USER@$SERVER_HOST:$DEPLOY_PATH/
ssh $SERVER_USER@$SERVER_HOST "chmod +x $DEPLOY_PATH/run_server.sh"

echo ""
echo "================================================"
echo "배포 완료!"
echo "================================================"
echo "서버 실행 방법:"
echo "  ssh $SERVER_USER@$SERVER_HOST"
echo "  cd $DEPLOY_PATH"
echo "  ./run_server.sh"
echo ""
echo "방화벽 설정 필요 (서버에서 실행):"
echo "  sudo ufw allow 54555/tcp"
echo "  sudo ufw allow 54777/udp"
echo ""
echo "클라이언트 접속 방법:"
echo "  set F1_SERVER_HOST=203.234.62.35"
echo "  run_multiplayer_client.bat"
echo "================================================"
