# Novobydžovský čtverec — webová aplikace

Webová aplikace pro správu a průběh **Novobydžovského čtverce** — motoristické soutěže pro historická vozidla konající se každý rok v Novém Bydžově.

## Stack

| Vrstva | Technologie | Hosting |
|---|---|---|
| Frontend | Vue 3 + Vite + Tailwind CSS | GitLab Pages (`app.bydzov-ctverec.cz`) |
| Backend | Spring Boot 3 + Java 17 | Render (`bydzov-ctverec-api.onrender.com`), později `api.bydzov-ctverec.cz` |
| Databáze | PostgreSQL 17 | Neon.tech free tier |
| Monitoring | UptimeRobot | free tier (wake-up ping) |
| Email | Brevo / Sendinblue | free tier (300 emailů/den) |

## Moduly (MVP — 30. ročník, 20. 6. 2026)

- Registrační formulář závodníka
- Dashboard pořadatele s přehledem přihlášek
- Bodovací formulář pro rozhodchí na stanovišti
- Živá výsledková tabulka (veřejná URL)
- JWT autentizace a správa rolí (ADMIN / RACER / JUDGE)
- GitLab CI/CD + GitLab Pages deployment

## Struktura repozitáře

```
bydzov-ctverec-app/
├── Dockerfile        # Docker image API (kontext = kořen repa — pro Render)
├── backend/          # Spring Boot aplikace
│   ├── src/
│   └── pom.xml
├── frontend/         # Vue 3 SPA
│   ├── src/
│   └── package.json
├── .gitlab-ci.yml    # CI/CD pipeline
├── .gitignore
└── README.md
```

## Nasazení (bez práce na tvé straně)

Po **pushnutí na `main`** GitLab sám otestuje backend, zbuildí frontend a nahraje ho na Pages; zároveň zavolá Render, aby přenasadil API (hook už máš v CI proměnných). Frontend je v kódu nastavený na **API na Renderu**, takže stránka zobrazí ročník i bez vlastní subdomény API.

Až bude DNS hotová, v GitLabu můžeš změnit proměnnou `VITE_API_BASE_URL` na `https://api.bydzov-ctverec.cz` (volitelné).

**Render (Docker):** v nastavení služby nech **Root Directory** prázdný a jako **Dockerfile Path** zadej `Dockerfile` (soubor v kořeni repa). Render pak posílá do buildu celý repozitář — starý `backend/Dockerfile` s `COPY pom.xml` v tom kontextu nefungoval.

## Spuštění lokálně

```bash
# Backend — vyžaduje JDK 17+ a proměnnou DATABASE_URL (postgresql://… z Neon)
cd backend
mvn spring-boot:run

# Firemní Maven mirror (403): mvn -s ci-settings.xml spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

## CI/CD

Push na `main` branch automaticky:
1. Sestavuje a testuje backend (Maven + JUnit, JDK 17)
2. Sestavuje frontend (Node 20 + Vite)
3. Nasazuje frontend na GitLab Pages
4. Triggeruje deploy backendu na Render.com

## Prostředí a secrets

Viz Wiki: `Plan → Wiki → Přihlašovací údaje — externí služby`

Secrets jsou uloženy v `Settings → CI/CD → Variables`:
- `DATABASE_URL` — Neon.tech connection string
- `BREVO_API_KEY` — Brevo API klíč
- `JWT_SECRET` — JWT podpisový klíč
- `RENDER_DEPLOY_HOOK` — doplnit po založení služby na Render.com

## Odkazy

- **Projekt:** https://gitlab.com/pre-vit/bydzov-ctverec-app
- **Frontend (po deploymentu):** https://app.bydzov-ctverec.cz
- **Backend API (aktuálně na Renderu):** https://bydzov-ctverec-api.onrender.com
- **Backend API (až bude DNS):** https://api.bydzov-ctverec.cz
- **Neon.tech konzole:** https://console.neon.tech/app/projects/sweet-violet-38872502
- **Render.com dashboard:** https://dashboard.render.com
- **UptimeRobot status:** https://stats.uptimerobot.com/SE8kCpiVgv