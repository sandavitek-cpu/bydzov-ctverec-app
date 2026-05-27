#!/bin/bash
# Spustit na Raspberry Pi: bash deploy/setup-rpi.sh
set -e

APP_USER=gitlab-runner
DEPLOY_DIR=/home/$APP_USER/bydzov-api

echo "=== 1. Vytvoreni deploy adresare ==="
sudo mkdir -p $DEPLOY_DIR
sudo chown $APP_USER:$APP_USER $DEPLOY_DIR

echo "=== 2. Kopirovani systemd service ==="
sudo cp deploy/bydzov-api.service /etc/systemd/system/bydzov-api.service
sudo systemctl daemon-reload

echo "=== 3. Povoleni service ==="
sudo systemctl enable bydzov-api

echo "=== 4. Pridani gitlab-runner do sudo pro systemctl ==="
echo "$APP_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart bydzov-api, /usr/bin/systemctl start bydzov-api, /usr/bin/systemctl stop bydzov-api" | sudo tee /etc/sudoers.d/bydzov-api

echo "=== 5. Rucni prvni deploy ==="
cd backend && mvn -B -ntp package -DskipTests
cp backend/target/bydzov-ctverec-api.jar $DEPLOY_DIR/
# .env vytvori CI pri dalsim pushi, nebo vytvor rucne:
echo "Vytvorim .env (zadej DATABASE_URL z GitLab CI variables):"
read -rp "DATABASE_URL: " dburl
cat > $DEPLOY_DIR/.env << ENVEOF
DATABASE_URL=$dburl
JWT_SECRET=$JWT_SECRET
GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID
SMTP_HOST=$SMTP_HOST
SMTP_PORT=$SMTP_PORT
SMTP_USER=$SMTP_USER
SMTP_PASS=$SMTP_PASS
PORT=8080
ENVEOF
chmod 600 $DEPLOY_DIR/.env

echo "=== 6. Spusteni service ==="
sudo systemctl start bydzov-api
sudo systemctl status bydzov-api --no-pager

echo ""
echo "=== HOTOVO ==="
echo "Service bezi na PORT=8080"
echo "Pro externi pristup nastav Cloudflare Tunnel (viz prompt nize)"
