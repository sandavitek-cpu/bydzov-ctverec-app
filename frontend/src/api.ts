export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'https://bydzov-ctverec-api.onrender.com'

export async function fetchCurrentEdition() {
  const res = await fetch(`${apiBaseUrl}/api/public/editions/current`)
  if (!res.ok) {
    throw new Error(`API ${res.status}`)
  }
  return res.json() as Promise<{ id: number; year: number; label: string }>
}

export interface CrewMemberInput {
  firstName: string
  lastName: string
  email: string
  driverAge: number
  gender: string
  address: string
  clubMember: boolean
  clubName: string
  firstTime: boolean
}

export interface RegistrationData {
  teamName: string
  email: string
  phone: string
  vehicleCategory: string
  vehiclePlate: string
  vehicleYear: number
  crewCount: number
  variant: string
  firstName: string
  lastName: string
  driverAge: number
  gender: string
  address: string
  club: string
  firstTime: boolean
  crewMembers: CrewMemberInput[]
}

export interface RegistrationResult {
  id: number
  teamName: string
  email: string
  phone: string
  vehicleCategory: string
  vehiclePlate: string
  vehicleYear: number
  crewCount: number
  startNumber: number
  startFee: number
  status: string
  variant: string
  paymentReference: number
}

export async function submitRegistration(data: RegistrationData) {
  const res = await fetch(`${apiBaseUrl}/api/public/registrations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) {
    throw new Error(body.error ?? `API ${res.status}`)
  }
  return body as RegistrationResult
}

export interface RacerLookup {
  id: number
  teamName: string
  startNumber: number
  vehicleCategory: string
  vehiclePlate: string
}

export async function lookupRacerByStartNumber(startNumber: number) {
  const res = await fetch(`${apiBaseUrl}/api/public/registrations/lookup/${startNumber}`)
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<RacerLookup>
}

export interface ScoreSubmit {
  racerRegistrationId: number
  checkpointId: number
  points: number
}

export interface RunScore {
  checkpointOrder: number
  checkpointName: string
  points: number
}

export interface ResultRow {
  rank: number
  startNumber: number
  teamName: string
  vehicleCategory: string
  vehiclePlate: string
  totalPoints: number
  runs: RunScore[]
}

export interface ResultsResponse {
  year: number
  results: ResultRow[]
}

export async function fetchResults(year: number) {
  const res = await fetch(`${apiBaseUrl}/api/public/results/${year}`)
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<ResultsResponse>
}

export interface ArchiveRow {
  editionYear: number
  rank: number
  racerName: string
  vehicle: string | null
  points: number
}

export interface ArchiveResponse {
  results: ArchiveRow[]
}

export async function fetchArchive(params?: { year?: number; name?: string; vehicle?: string }) {
  const q = new URLSearchParams()
  if (params?.year) q.set('year', String(params.year))
  if (params?.name) q.set('name', params.name)
  if (params?.vehicle) q.set('vehicle', params.vehicle)
  const qs = q.toString()
  const url = `${apiBaseUrl}/api/public/archive${qs ? '?' + qs : ''}`
  const res = await fetch(url)
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<ArchiveResponse>
}

export async function fetchArchiveByYear(year: number) {
  const res = await fetch(`${apiBaseUrl}/api/public/archive/${year}`)
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<ArchiveResponse>
}

export interface CheckpointData {
  id?: number
  name: string
  lat: number
  lng: number
  radius: number
  sortOrder: number
  taskDescription: string | null
  maxPoints: number | null
  volunteers: string[]
}

export async function fetchAdminCheckpoints(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<CheckpointData[]>
}

export async function createAdminCheckpoint(data: Partial<CheckpointData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as CheckpointData
}

export async function updateAdminCheckpoint(id: number, data: Partial<CheckpointData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as CheckpointData
}

export async function approveRegistration(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${id}/approve`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body
}

export async function impersonateRegistration(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${id}/impersonate`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as { accessToken: string; username: string; name: string; role: string }
}

export async function impersonateUser(userId: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/users/${userId}/impersonate`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as { accessToken: string; username: string; name: string; role: string }
}

export interface RoutePointData {
  id: number | null
  sortOrder: number
  lat: number
  lng: number
  distanceFromStart: number
}

export interface RouteData {
  id: number
  variant: string
  name: string
  totalDistance: number
  published: boolean
  points: RoutePointData[]
}

export async function fetchAdminRoutes(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/routes`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<RouteData[]>
}

export async function deleteAdminCheckpoint(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(`API ${res.status}`)
}

export interface NotifyRequest {
  recipientType: string
  subject: string
  body: string
}

export interface NotifyResult {
  sent: number
  total: number
  recipientType: string
}

export interface MessageLogEntry {
  id: number
  recipientType: string
  subject: string
  body: string
  recipientCount: number
  createdAt: string
}

export async function sendNotify(data: NotifyRequest, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/notify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as NotifyResult
}

export async function fetchNotifyHistory(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/notify`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<MessageLogEntry[]>
}

export interface LogLevelResponse {
  level: string
}

export async function fetchLogLevel(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/logging/level`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<LogLevelResponse>
}

export async function setLogLevel(level: string, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/logging/level`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify({ level }),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as LogLevelResponse
}

export async function downloadLog(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/logging/download`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.blob()
}

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
  appRoles: { id: number; name: string; displayName: string }[]
}

export async function fetchAdminUsers(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/users`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<AdminUser[]>
}

export async function submitScore(data: ScoreSubmit, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/scores`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) {
    throw new Error(body.message ?? `API ${res.status}`)
  }
  return body
}

export async function fetchAccount(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/account`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<AdminUser>
}

export async function assignStartNumber(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${id}/assign-start-number`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as { startNumber: number }
}

export async function fetchRacerStatus(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/status`, { headers })
  if (!res.ok) throw new Error(`API ${res.status}`)
  return res.json() as Promise<{
    id: number; paymentReference: number; teamName: string; startNumber: number; startFee: number
    status: string; variant: string; vehicleCategory: string; vehiclePlate: string
    vehicleYear: number; vehicleMake: string; crewCount: number; approved: boolean
  }>
}

export async function updateAccount(data: { name?: string; email?: string; phone?: string }, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/account`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body
}
