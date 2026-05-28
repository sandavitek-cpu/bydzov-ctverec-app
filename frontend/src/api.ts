export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'https://bydzov-ctverec-api.onrender.com'

const apiVersionPrefix = import.meta.env.VITE_API_VERSION ?? ''


export function apiVersionedUrl(path: string): string {
  return `${apiBaseUrl}/api${apiVersionPrefix}${path}`
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
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

function isTokenExpiring(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now() + 60000
  } catch {
    return true
  }
}

let refreshPromise: Promise<boolean> | null = null

export async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  let token = getToken()
  if (token && isTokenExpiring(token)) {
    if (!refreshPromise) {
      refreshPromise = tryRefreshToken().finally(() => { refreshPromise = null })
    }
    await refreshPromise
    token = getToken()
  }
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> ?? {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return fetch(url, { ...options, headers })
}

async function apiError(res: Response): Promise<string> {
  try {
    const body = await res.json()
    return body.error ?? body.message ?? `API ${res.status}`
  } catch {
    return `API ${res.status}`
  }
}



export async function fetchCurrentEdition() {
  const res = await fetch(`${apiBaseUrl}/api/public/editions/current`)
  if (!res.ok) throw new Error(await apiError(res))
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
  vehicleMake?: string
  vehicleNotes?: string
  vehicleStory?: string
  notes?: string
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
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<RacerLookup>
}

export interface UserLookup {
  found: boolean
  firstName?: string
  lastName?: string
}

export async function lookupUserByEmail(email: string) {
  const res = await fetch(`${apiBaseUrl}/api/public/registrations/lookup-user?email=${encodeURIComponent(email)}`)
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<UserLookup>
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
  if (!res.ok) throw new Error(await apiError(res))
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
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<ArchiveResponse>
}

export async function fetchArchiveByYear(year: number) {
  const res = await fetch(`${apiBaseUrl}/api/public/archive/${year}`)
  if (!res.ok) throw new Error(await apiError(res))
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
  if (!res.ok) throw new Error(await apiError(res))
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
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<RouteData[]>
}

export async function deleteAdminCheckpoint(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(await apiError(res))
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
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<MessageLogEntry[]>
}

export interface LogLevelResponse {
  level: string
}

export async function fetchLogLevel(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/logging/level`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
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

export async function fetchAdminUsers(headers: Record<string, string>, q?: string) {
  const url = q ? `${apiBaseUrl}/api/admin/users?q=${encodeURIComponent(q)}` : `${apiBaseUrl}/api/admin/users`
  const res = await fetch(url, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<AdminUser[]>
}

export interface VariantConfig {
  id: number
  variantCode: string
  label: string
  registrationDeadline: string | null
  registrationReopenedUntil: string | null
  raceDate: string | null
  enabled: boolean
}

export async function fetchAdminVariants(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<VariantConfig[]>
}

export async function createAdminVariant(data: Partial<VariantConfig>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<VariantConfig>
}

export async function updateAdminVariant(id: number, data: Partial<VariantConfig>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<VariantConfig>
}

export async function deleteAdminVariant(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(await apiError(res))
}

export async function reopenAdminVariant(id: number, data: { durationMinutes: number } | { untilMidnight: boolean }, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants/${id}/reopen`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as VariantConfig
}

export async function closeAdminVariantReopen(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/variants/${id}/reopen`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<VariantConfig>
}

export interface RuiAnAddress {
  kod: number
  adresa: string
  psc: number | null
  cislodomovni: number | null
  cisloorientacni: number | null
  cisloorientacnipismeno: string | null
}

export async function searchRuiAnAddress(q: string): Promise<RuiAnAddress[]> {
  if (!q.trim()) return []
  const res = await fetch(`${apiBaseUrl}/api/public/ruian/search?q=${encodeURIComponent(q)}`)
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<RuiAnAddress[]>
}

export interface PublicVariant {
  variantCode: string
  label: string
  registrationDeadline: string | null
  registrationReopenedUntil: string | null
  raceDate: string | null
}

export interface FeeConfig {
  baseDo1945: number
  baseOd1946: number
  extraPerson: number
}

export async function fetchFees() {
  const res = await fetch(`${apiBaseUrl}/api/public/fees`)
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<Record<string, FeeConfig>>
}

export async function fetchPublicVariants() {
  const res = await fetch(`${apiBaseUrl}/api/public/variants`)
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<PublicVariant[]>
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
  if (!res.ok) throw new Error(await apiError(res))
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

export async function fetchRacerProfile(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/profile`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<{ firstName: string; lastName: string; email: string; phone: string }>
}

export async function fetchRacerStatus(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/status`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<{
    id: number; paymentReference: number; teamName: string; startNumber: number; startFee: number
    paidAmount: number | null; status: string; variant: string; vehicleCategory: string; vehiclePlate: string
    vehicleYear: number; vehicleMake: string; crewCount: number; approved: boolean
    cancelledAt: string | null; refundAmount: number | null
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

export interface VehicleData {
  id: number
  vehicleMake: string
  vehiclePlate: string
  vehicleYear: number
  vehicleCategory: string
  engineDisplacement: number | null
  power: number | null
  maxSpeed: number | null
  vehicleNotes: string | null
  createdAt: string
}

export async function fetchVehicles(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/vehicles`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<VehicleData[]>
}

export async function createVehicle(data: Partial<VehicleData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/vehicles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as VehicleData
}

export async function updateVehicle(id: number, data: Partial<VehicleData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/vehicles/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as VehicleData
}

export async function deleteVehicle(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/vehicles/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(`API ${res.status}`)
}

export interface RaceCategory {
  id: number
  name: string
  code: string | null
  variant: string | null
  determination: string
  sortOrder: number
  winnerRegistrationId: number | null
  winnerName: string | null
  winnerTeam: string | null
  winnerNumber: number | null
  winnerPoints: number | null
}

export interface CeremonyCategory {
  id: number
  name: string
  sortOrder: number
  winnerName: string | null
  winnerTeam: string | null
  winnerNumber: number | null
  winnerPoints: number | null
}

export interface CeremonyOverallRow {
  rank: number
  startNumber: number
  teamName: string
  vehicleCategory: string
  vehiclePlate: string
  totalPoints: number
}

export interface CeremonyData {
  year: number
  overall: CeremonyOverallRow[]
  categories: CeremonyCategory[]
}

export async function fetchCeremonyCategories(year: number) {
  const res = await fetch(`${apiBaseUrl}/api/public/ceremony/${year}`)
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<CeremonyData>
}

export async function fetchAdminCeremonyData(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/ceremony/data`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<CeremonyData>
}

export async function generateCeremonyPresentation(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/ceremony/generate`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as { computed: number; data: CeremonyData }
}

export async function fetchAdminCategories(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/categories`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<RaceCategory[]>
}

export async function createAdminCategory(data: Partial<RaceCategory>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/categories`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as RaceCategory
}

export async function updateAdminCategory(id: number, data: Partial<RaceCategory>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/categories/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as RaceCategory
}

export async function deleteAdminCategory(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/categories/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(`API ${res.status}`)
}

export async function computeCategoryWinners(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/categories/compute`, {
    method: 'POST',
    headers,
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as { computed: boolean; count: number }
}

export interface ScheduleItemData {
  id?: number
  time: string
  label: string
  description: string | null
  sortOrder: number
}

export async function fetchAdminSchedule(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/schedule`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<ScheduleItemData[]>
}

export async function createAdminScheduleItem(data: Partial<ScheduleItemData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/schedule`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as ScheduleItemData
}

export async function updateAdminScheduleItem(id: number, data: Partial<ScheduleItemData>, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/schedule/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(data),
  })
  const body = await res.json()
  if (!res.ok) throw new Error(body.error ?? `API ${res.status}`)
  return body as ScheduleItemData
}

export async function deleteAdminScheduleItem(id: number, headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/admin/schedule/${id}`, {
    method: 'DELETE',
    headers,
  })
  if (!res.ok) throw new Error(`API ${res.status}`)
}

export interface NotificationItem {
  id: number
  title: string
  message: string
  type: string
  relatedUrl: string | null
  isRead: boolean
  createdAt: string
}

export interface NotificationResponse {
  notifications: NotificationItem[]
  unreadCount: number
}

export async function fetchNotifications(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/notifications`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<NotificationResponse>
}

export async function markNotificationRead(id: number, headers: Record<string, string>) {
  await fetch(`${apiBaseUrl}/api/notifications/${id}/read`, {
    method: 'PATCH',
    headers,
  })
}

export async function markAllNotificationsRead(headers: Record<string, string>) {
  await fetch(`${apiBaseUrl}/api/notifications/read-all`, {
    method: 'POST',
    headers,
  })
}

export interface ItineraryCheckpoint {
  name: string
  sortOrder: number
  maxPoints: number | null
  scorePoints: number | null
  phone: string | null
  taskDescription: string | null
  volunteers: string[]
}

export interface ItineraryRoute {
  name: string
  totalDistance: number
  pointCount: number
}

export interface ItineraryContact {
  towPhone: string | null
  towNote: string | null
}

export interface ItineraryResponse {
  teamName: string | null
  startNumber: number | null
  schedule: ScheduleItemData[]
  checkpoints: ItineraryCheckpoint[]
  passedCount: number
  remainingCount: number
  route: ItineraryRoute | null
  contact: ItineraryContact | null
}

export async function fetchRacerItinerary(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/racer/itinerary`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<ItineraryResponse>
}

export interface JudgeCheckpoint {
  id: number
  name: string
  sortOrder: number
}

export interface JudgeRacer {
  id: number
  startNumber: number
  teamName: string
  vehiclePlate: string
  vehicleCategory: string
  scored: boolean
}

export interface JudgeStats {
  total: number
  scored: number
  remaining: number
}

export interface JudgeOverview {
  checkpoints: JudgeCheckpoint[]
  racers: JudgeRacer[]
  stats: JudgeStats
}

export async function fetchJudgeOverview(headers: Record<string, string>) {
  const res = await fetch(`${apiBaseUrl}/api/judge/overview`, { headers })
  if (!res.ok) throw new Error(await apiError(res))
  return res.json() as Promise<JudgeOverview>
}
