// ── Results / Archive / Leaderboard types ──

import type { RunScore } from './checkpoint'

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
