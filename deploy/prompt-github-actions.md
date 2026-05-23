# Prompt pro AI ovládající prohlížeč: GitHub Actions + Docker Hub + Render

## Krok 1: Docker Hub účet
1. Otevři https://hub.docker.com/signup
2. Zaregistruj se (stačí free plan, veřejný repozitář)
3. Potvrď email
4. Po přihlášení jdi na **Account Settings → Security → New Access Token**
5. Nazvi ho `github-actions`, vyber permisse **Read & Write**
6. Zkopíruj vygenerovaný token (něco jako `dckr_pat_...`)

## Krok 2: GitHub repozitář (import z GitLabu)
1. Přihlas se na https://github.com
2. Jdi na **New repository → Import a repository**
3. Import URL: `https://gitlab.com/pre-vit/bydzov-ctverec-app`
4. Nazvi ho `bydzov-ctverec-app`
5. Vyber **Public** (public repo = 2000 min/month zdarma od GitHub Actions)
6. Klikni **Begin import** (importuje se celý GitLab repozitář i s historií)

## Krok 3: GitHub Secrets
1. V GitHub repozitáři jdi na **Settings → Secrets and variables → Actions**
2. Klikni **New repository secret**
3. `DOCKER_USERNAME`: tvoje Docker Hub přihlašovací jméno (ne email)
4. Klikni **Add secret**
5. Klikni **New repository secret**
6. `DOCKER_PASSWORD`: vlož access token z kroku 1
7. Klikni **Add secret**

## Krok 4: Render → přepni na pre-built image
1. Otevři Render dashboard: https://dashboard.render.com
2. Klikni na `bydzov-ctverec-api`
3. Jdi na **Settings → Build & Deploy**
4. Klikni **Edit** u "Deploy Type"
5. Změň z **Build from Dockerfile** na **Existing Docker Image**
6. Do "Image URL" zadej: `docker.io/TVUJ_DOCKER_HUB_USERNAME/bydzov-ctverec-api:latest`
7. (Nahraď `TVUJ_DOCKER_HUB_USERNAME` svým Docker Hub username)
8. Klikni **Save**
9. V dialogu "Auto-Deploy" vyber **Yes** (bude naslouchat Docker Hub webhooku)
10. Klikni **Save**

## Krok 5: Ověř
1. V GitHub repozitáři jdi na **Actions** → měl by tam být workflow
2. Po pushi na main: GitHub Actions buildí image → Docker Hub → Render auto-deploy

## Problemy?
- Render `runtime` se změnil na `docker` a `autoDeploy: yes`. Po přepnutí na image zmizí auto-deploy z GitLabu a začne auto-deploy z Docker Hubu.
- Render free tier se uspává po 15 minutách nečinnosti (cold start 50+ sekund) – to je stejné jako teď.
