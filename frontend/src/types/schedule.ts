// ── Schedule types ──

export interface ScheduleItemData {
  id?: number
  time: string
  label: string
  description: string | null
  sortOrder: number
}
