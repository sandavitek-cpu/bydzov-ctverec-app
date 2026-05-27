// ── Checkpoint types ──

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
