# Prompt pro AI ovládající prohlížeč: GitHub token + GitLab sync

## Krok 1: GitHub Personal Access Token
1. Otevři https://github.com/settings/tokens
2. Klikni **Generate new token → Classic**
3. Note: `gitlab-sync`
4. Expiration: **No expiration**
5. Select scopes: `repo` (všechny) + `workflow`
6. Klikni **Generate token**
7. Zkopíruj token (formát `ghp_...`)

## Krok 2: Přidej token do GitLab CI variables
1. Otevři https://gitlab.com/pre-vit/bydzov-ctverec-app/-/settings/ci_cd
2. Rozbal **Variables**
3. Klikni **Add variable**
4. Key: `GITHUB_PAT`
5. Value: token z kroku 1
6. Type: **Variable**
7. Flags: ✅ **Protect variable** (jen main branch)
8. Flags: ❌ **Mask variable** (nejde maskovat, začíná ghp_)
9. Klikni **Add variable**

## Krok 3: Render API secrets do GitHubu
1. Otevři https://github.com/sandavitek-cpu/bydzov-ctverec-app/settings/secrets/actions
2. Klikni **New repository secret**
3. Name: `RENDER_API_KEY` → Value: `rnd_pxg0yEamD3wLbmlTu8i8w6Upmnmn`
4. Klikni **Add secret**
5. Klikni **New repository secret**
6. Name: `RENDER_SERVICE_ID` → Value: `srv-d821b91j2pic73auum4g`
7. Klikni **Add secret**

## Hotovo
Při příštím pushi na main v GitLabu CI automaticky:
1. Otestuje backend a frontend na Pi runneru
2. Pushne kód na GitHub
3. GitHub Actions buildí Docker image → pushne na Docker Hub
4. GitHub Actions zavolá Render API → deploy
5. Render pullne nejnovější image → live
