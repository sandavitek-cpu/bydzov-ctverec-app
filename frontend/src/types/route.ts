// ── Route / Itinerary types ──

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
  schedule: import('./schedule').ScheduleItemData[]
  checkpoints: ItineraryCheckpoint[]
  passedCount: number
  remainingCount: number
  route: ItineraryRoute | null
  contact: ItineraryContact | null
}
