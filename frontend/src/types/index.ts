// ── Barrel re-exports for all shared types ──

export type {
  CrewMemberInput, RegistrationData, RegistrationResult,
  RacerLookup, UserLookup, RuiAnAddress, FeeConfig,
  PublicVariant, CrewInfo, AdminReg, AdminStats, VehicleData,
} from './registration'

export type {
  CheckpointData, ScoreSubmit, RunScore,
} from './checkpoint'

export type {
  RoutePointData, RouteData,
  ItineraryCheckpoint, ItineraryRoute, ItineraryContact, ItineraryResponse,
} from './route'

export type {
  VariantConfig, RaceCategory,
  CeremonyCategory, CeremonyOverallRow, CeremonyData,
} from './race'

export type {
  AdminUser, AppRole, ImpersonateResult, LoginResponse,
} from './auth'

export type {
  ResultRow, ResultsResponse, ArchiveRow, ArchiveResponse,
} from './results'

export type {
  JudgeCheckpoint, JudgeRacer, JudgeStats, JudgeOverview,
} from './judge'

export type {
  ScheduleItemData,
} from './schedule'

export type {
  NotificationItem, NotificationResponse,
  NotifyRequest, NotifyResult, MessageLogEntry,
} from './notifications'
