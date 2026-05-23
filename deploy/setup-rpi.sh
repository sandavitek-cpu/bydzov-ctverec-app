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
JWT_SECRET=faf42bc20d70c3a29b7c63a12be179b2bb47ed558a0ffb4153d8af2ec5abc90f1c2c41b9dfef9b1170e70ef7b403108f2caa39dfdf6c77ef2cca5868bf6aeb1f
GOOGLE_CLIENT_ID=531070217480-3m6va4pamau78fhk85qs3mql1o26iv0e.apps.googleusercontent.com
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USER=
SMTP_PASS=
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
