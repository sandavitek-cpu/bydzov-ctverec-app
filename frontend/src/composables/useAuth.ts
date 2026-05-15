import { computed, ref } from 'vue'
import { apiBaseUrl } from '@/api'

const token = ref(localStorage.getItem('admin_token') ?? '')
const role = ref(localStorage.getItem('admin_role') ?? '')
const name = ref(localStorage.getItem('admin_name') ?? '')

export function useAuth() {
  function login(accessToken: string, userRole: string, userName: string) {
    token.value = accessToken
    role.value = userRole
    name.value = userName
    localStorage.setItem('admin_token', accessToken)
    localStorage.setItem('admin_role', userRole)
    localStorage.setItem('admin_name', userName)
  }

  function logout() {
    token.value = ''
    role.value = ''
    name.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_role')
    localStorage.removeItem('admin_name')
  }

  function authHeaders(): Record<string, string> {
    return token.value ? { Authorization: `Bearer ${token.value}` } : {}
  }

  const isAdmin = computed(() => role.value.split(',').includes('ADMIN'))
  const isLoggedIn = computed(() => !!token.value)

  async function loginRequest(login: string, password: string) {
    const res = await fetch(`${apiBaseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ login, password }),
    })
    const data = await res.json()
    if (!res.ok) {
      throw new Error(data.error ?? 'Přihlášení selhalo')
    }
    login(data.accessToken, data.role, data.name)
    return data
  }

  return { token, role, name, login, logout, authHeaders, isAdmin, isLoggedIn, loginRequest }
}
