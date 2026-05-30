import { showApiErrorToast } from '@/composables/useToast'

const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'https://bydzov-ctverec-api.onrender.com'

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public errorCode?: string,
    public body?: unknown
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

function getToken(): string | null {
  return localStorage.getItem('admin_token')
}

function getRefreshToken(): string | null {
  return localStorage.getItem('admin_refresh_token')
}

async function tryRefreshToken(): Promise<boolean> {
  const rt = getRefreshToken()
  if (!rt) return false
  try {
    const res = await fetch(`${apiBaseUrl}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    })
    if (!res.ok) return false
    const data = await res.json()
    localStorage.setItem('admin_token', data.accessToken)
    return true
  } catch {
    return false
  }
}

function isTokenExpiring(token_: string): boolean {
  try {
    const payload = JSON.parse(atob(token_.split('.')[1]))
    return payload.exp * 1000 < Date.now() + 60000
  } catch {
    return true
  }
}

let refreshPromise: Promise<boolean> | null = null

async function buildHeaders(): Promise<Record<string, string>> {
  const headers: Record<string, string> = {}
  let token = getToken()
  if (token && isTokenExpiring(token)) {
    if (!refreshPromise) {
      refreshPromise = tryRefreshToken().finally(() => { refreshPromise = null })
    }
    await refreshPromise
    token = getToken()
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

interface RequestOptions {
  params?: Record<string, string | number | undefined>
  body?: unknown
  headers?: Record<string, string>
}

async function extractError(res: Response): Promise<{ message: string; errorCode?: string }> {
  try {
    const body = await res.json()
    const message = body?.error ?? body?.message ?? `API ${res.status}`
    return { message, errorCode: body?.errorCode }
  } catch {
    return { message: `API ${res.status}` }
  }
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const url = new URL(`${apiBaseUrl}/api${path}`, window.location.origin)
  if (options.params) {
    for (const [key, value] of Object.entries(options.params)) {
      if (value !== undefined) url.searchParams.set(key, String(value))
    }
  }

  const headers: Record<string, string> = { ...options.headers }
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const authHeaders = await buildHeaders()
  Object.assign(headers, authHeaders)

  const res = await fetch(url.toString(), {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (!res.ok) {
    const { message, errorCode } = await extractError(res)
    showApiErrorToast(message, errorCode)
    console.error(`[${errorCode ?? 'FE-' + generateShortCode()}] ${method} ${path} - ${message}`)
    throw new ApiError(message, res.status, errorCode)
  }

  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    const body = await res.json()
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return body.data as T
    }
    return body as T
  }

  return undefined as T
}

function generateShortCode(): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let result = ''
  for (let i = 0; i < 8; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

export const api = {
  get<T>(path: string, options?: RequestOptions): Promise<T> {
    return request<T>('GET', path, options)
  },
  post<T>(path: string, options?: RequestOptions): Promise<T> {
    return request<T>('POST', path, options)
  },
  put<T>(path: string, options?: RequestOptions): Promise<T> {
    return request<T>('PUT', path, options)
  },
  patch<T>(path: string, options?: RequestOptions): Promise<T> {
    return request<T>('PATCH', path, options)
  },
  delete<T = void>(path: string, options?: RequestOptions): Promise<T> {
    return request<T>('DELETE', path, options)
  },
}
