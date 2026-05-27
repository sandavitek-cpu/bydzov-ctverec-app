// ── Registration / Crew / Fee / Address types ──

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

export interface RacerLookup {
  id: number
  teamName: string
  startNumber: number
  vehicleCategory: string
  vehiclePlate: string
}

export interface UserLookup {
  firstName: string
  lastName: string
}

export interface RuiAnAddress {
  kod: number
  adresa: string
  psc: number | null
  cislodomovni: number | null
  cisloorientacni: number | null
  cisloorientacnipismeno: string | null
}

export interface FeeConfig {
  baseDo1945: number
  baseOd1946: number
  extraPerson: number
}

export interface PublicVariant {
  variantCode: string
  label: string
  registrationDeadline: string | null
  registrationReopenedUntil: string | null
  raceDate: string | null
}

export interface CrewInfo {
  firstName: string
  lastName: string
  email: string
  driverAge: number | null
  gender: string | null
  address: string | null
  clubMember: boolean
  clubName: string | null
  firstTime: boolean
}

export interface AdminReg {
  id: number
  teamName: string
  email: string
  phone: string
  vehicleCategory: string
  vehicleMake: string
  vehiclePlate: string
  vehicleYear: number
  crewCount: number
  startNumber: number
  startFee: number
  paidAmount: number | null
  paymentReference: number | null
  status: string
  variant: string | null
  firstName: string | null
  lastName: string | null
  firstTime: boolean
  gender: string | null
  driverAge: number | null
  club: string | null
  address: string | null
  youngestAge: number | null
  youngestName: string | null
  engineDisplacement: number | null
  power: number | null
  maxSpeed: number | null
  vehicleNotes: string | null
  vehicleStory: string | null
  notes: string | null
  contacted: boolean
  properlyRegistered: boolean
  arrived: boolean
  consent: boolean
  approved: boolean
  createdAt: string
  paidAt: string | null
  cancelledAt: string | null
  refundAmount: number | null
  crewMembers: CrewInfo[]
}

export interface AdminStats {
  totalCrews: number
  totalMembers: number
  paid: number
  contacted: number
  arrived: number
  firstTimers: number
  women: number
  kidsUnder10: number
  vehiclesBefore1945: number
  cars: number
  motos: number
  oldestVehicle: number
  newestVehicle: number
  oldestDriver: number
  youngestDriver: number
  jednodenni: number
  dvoudenni: number
  withoutAccommodation: number
  jednodenniMembers: number
  dvoudenniMembers: number
  approved: number
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
