import { computed, ref } from 'vue'
import { apiBaseUrl } from '@/api'

const token = ref(localStorage.getItem('admin_token') ?? '')
const role = ref(localStorage.getItem('admin_role') ?? '')
const name = ref(localStorage.getItem('admin_name') ?? '')
const username = ref(localStorage.getItem('admin_username') ?? '')
const impersonating = ref(localStorage.getItem('admin_impersonating') === 'true')

export function useAuth() {
  function saveTokens(accessToken: string, userRole: string, userName: string, userUsername?: string) {
    token.value = accessToken
    role.value = userRole
    name.value = userName
    username.value = userUsername ?? ''
    localStorage.setItem('admin_token', accessToken)
    localStorage.setItem('admin_role', userRole)
    localStorage.setItem('admin_name', userName)
    if (userUsername) localStorage.setItem('admin_username', userUsername)
  }

  function logout() {
    token.value = ''
    role.value = ''
    name.value = ''
    username.value = ''
    impersonating.value = false
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_role')
    localStorage.removeItem('admin_name')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_impersonating')
    localStorage.removeItem('admin_token_backup')
    localStorage.removeItem('admin_role_backup')
    localStorage.removeItem('admin_name_backup')
    localStorage.removeItem('admin_username_backup')
  }

  function authHeaders(): Record<string, string> {
    return token.value ? { Authorization: `Bearer ${token.value}` } : {}
  }

  function impersonateAs(newToken: string, newRole: string, newName: string, newUsername: string) {
    localStorage.setItem('admin_token_backup', localStorage.getItem('admin_token') ?? '')
    localStorage.setItem('admin_role_backup', role.value)
    localStorage.setItem('admin_name_backup', name.value)
    localStorage.setItem('admin_username_backup', username.value)
    impersonating.value = true
    localStorage.setItem('admin_impersonating', 'true')
    saveTokens(newToken, newRole, newName, newUsername)
  }

  function restoreFromImpersonation() {
    const backupToken = localStorage.getItem('admin_token_backup') ?? ''
    if (!backupToken) {
      logout()
      return
    }
    impersonating.value = false
    localStorage.removeItem('admin_impersonating')
    localStorage.removeItem('admin_token_backup')
    const r = localStorage.getItem('admin_role_backup') ?? ''
    const n = localStorage.getItem('admin_name_backup') ?? ''
    const u = localStorage.getItem('admin_username_backup') ?? ''
    saveTokens(backupToken, r, n, u)
  }

  const roles = computed(() => role.value ? role.value.split(',') : [])
  const hasAdmin = computed(() => roles.value.includes('ADMIN'))
  const hasJudge = computed(() => roles.value.includes('JUDGE'))
  const hasRacer = computed(() => roles.value.includes('RACER'))
  const isAdmin = hasAdmin
  const isLoggedIn = computed(() => !!token.value)

  async function loginRequest(loginValue: string, password: string) {
    const res = await fetch(`${apiBaseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ login: loginValue, password }),
    })
    const data = await res.json()
    if (!res.ok) {
      throw new Error(data.error ?? 'Přihlášení selhalo')
    }
    saveTokens(data.accessToken, data.role, data.name, data.username)
    return data
  }

  return { token, role, roles, name, username, impersonating, logout, authHeaders, isAdmin, hasAdmin, hasJudge, hasRacer, isLoggedIn, loginRequest, saveTokens, impersonateAs, restoreFromImpersonation }
}
