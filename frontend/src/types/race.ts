// ── Variant / Category / Ceremony types ──

export interface VariantConfig {
  id: number
  variantCode: string
  label: string
  registrationDeadline: string | null
  registrationReopenedUntil: string | null
  raceDate: string | null
  enabled: boolean
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
