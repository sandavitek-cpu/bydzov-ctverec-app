# Prompt pro AI ovládající prohlížeč: Cloudflare Tunnel

## 1. Účet na Cloudflare
1. Otevři https://dash.cloudflare.com/sign-up
2. Zaregistruj se (nebo přihlas, pokud už účet máš)
3. Přidej doménu `bydzov-ctverec.cz`: zadej doménu, Cloudflare ti ukáže DNS nameservery
4. Jdi do svého registrátora (např. Wedos, Forpsi) a změň NS na ty od Cloudflare
5. Počkej 5-10 minut, dokud Cloudflare nepotvrdí "Active"

## 2. Token pro Tunnel
1. V Cloudflare dashboardu jdi na **Zero Trust** → **Access** → **Tunnels**
2. Klikni **Create a tunnel**
3. Vyber **Cloudflared** → **Next**
4. Dej název: `bydzov-ctverec-api`
5. Zkopíruj **token** (řetězec jako `eyJ...`)
6. Klikni **Save**

## 3. Nainstaluj cloudflared na Raspberry Pi a připoj tunel
Na Raspberry Pi (SSH) spusť:

```bash
# Stáhnout cloudflared pro ARM
curl -fsSL https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm -o /tmp/cloudflared
sudo install /tmp/cloudflared /usr/local/bin/cloudflared

# Připojit tunel (token z kroku 2)
sudo cloudflared tunnel run --token TOKEN_Z_2_KROKU
```

## 4. Nastav DNS a Public hostname
1. Vrať se do Cloudflare dashboardu → **Tunnels**
2. Klikni na tunel → **Edit** → **Public Hostname** tab
3. Přidej:
   - **Subdomain**: `api`
   - **Domain**: `bydzov-ctverec.cz`
   - **Type**: `HTTP`
   - **URL**: `localhost:8080`
4. Klikni **Save**

## 5. Nastav cloudflared jako systemd service
Na Raspberry Pi:

```bash
# Vytvoř konfigurační adresář
sudo mkdir -p /etc/cloudflared

# Vytvoř config.yml
cat << 'EOF' | sudo tee /etc/cloudflared/config.yml
tunnel: TOKEN_Z_2_KROKU
ingress:
  - hostname: api.bydzov-ctverec.cz
    service: http://localhost:8080
  - service: http_status:404
EOF

# Nainstaluj jako službu
sudo cloudflared service install

# Zkontroluj
sudo systemctl status cloudflared
```

## Hotovo
Až bude tunel aktivní, `https://api.bydzov-ctverec.cz` bude směřovat na Spring Boot API na Raspberry Pi.
