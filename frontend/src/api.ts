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
