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

## Hotovo
Při příštím pushi na main v GitLabu CI automaticky:
1. Otestuje backend a frontend (Pi runner)
2. Pushne kód na GitHub
3. GitHub Actions buildí Docker image → Docker Hub
4. Render auto-deploy
