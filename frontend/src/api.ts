export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'https://bydzov-ctverec-api.onrender.com'

export async function fetchCurrentEdition() {
  const res = await fetch(`${apiBaseUrl}/api/public/editions/current`)
  if (!res.ok) {
    throw new Error(`API ${res.status}`)
  }
  return res.json() as Promise<{ id: number; year: number; label: string }>
}

export interface RegistrationData {
  teamName: string
  email: string
  phone: string
  vehicleCategory: string
  vehiclePlate: string
  vehicleYear: number
  crewCount: number
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
  runNumber: number
  points: number
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
