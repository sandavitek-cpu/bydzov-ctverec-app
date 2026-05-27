// ── Judge-related types ──

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
