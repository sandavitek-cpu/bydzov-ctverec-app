// ── Auth / User / Role types ──

export interface AdminUser {
  id: number
  email: string
  username: string
  firstName: string
  lastName: string
  name: string
  phone: string
  memberSince: string
  role: string
  createdAt: string
  appRoles: AppRole[]
}

export interface AppRole {
  id: number
  name: string
  displayName: string
}

export interface ImpersonateResult {
  accessToken: string
  username: string
  name: string
  role: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  username: string
  name: string
  role: string
}
