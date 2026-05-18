<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { submitRegistration, fetchVehicles, createVehicle, fetchRacerProfile, lookupUserByEmail, type RegistrationResult, type CrewMemberInput, type VehicleData } from '@/api'
import { useAuth } from '@/composables/useAuth'

const { isLoggedIn, authHeaders } = useAuth()

const VARIANTS = [
  { value: 'JEDNODENNI', label: 'Jednodenní závod', deadline: '6. 6. 2026' },
  { value: 'DVODENNI_UZAVRENO', label: 'Dvoudenní závod – UZAVŘENO', deadline: '30. 4. 2026' },
  { value: 'DVODENNI_BEZ_UBYTOVANI', label: 'Dvoudenní závod bez ubytování', deadline: '30. 4. 2026' },
]

const VEHICLE_TYPES = [
  { value: 'AUTO', label: 'Automobil' },
  { value: 'MOTO', label: 'Motocykl' },
]

interface FeeRow {
  baseDo1945: number
  baseOd1946: number
  extraPerson: number
}

const FEE: Record<string, FeeRow> = {
  JEDNODENNI: { baseDo1945: 500, baseOd1946: 800, extraPerson: 500 },
  DVODENNI_UZAVRENO: { baseDo1945: 1000, baseOd1946: 1200, extraPerson: 1000 },
  DVODENNI_BEZ_UBYTOVANI: { baseDo1945: 600, baseOd1946: 900, extraPerson: 600 },
}

const form = ref({
  teamName: '',
  email: '',
  phone: '',
  vehicleCategory: '',
  vehiclePlate: '',
  vehicleYear: new Date().getFullYear(),
  crewCount: 1,
  variant: '',
  firstName: '',
  lastName: '',
  driverAge: 0,
  gender: '',
  address: '',
  club: '',
  firstTime: false,
  vehicleMake: '',
  consent: false,
})

const driverClubMember = ref(false)
const crewMembers = ref<CrewMemberInput[]>([])

watch(() => form.value.crewCount, (n) => {
  const target = Math.max(0, n - 1)
  while (crewMembers.value.length < target) {
    crewMembers.value.push({ firstName: '', lastName: '', email: '', driverAge: 0, gender: '', address: '', clubMember: false, clubName: '', firstTime: false })
  }
  if (crewMembers.value.length > target) {
    crewMembers.value.splice(target)
  }
})

const submitted = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<RegistrationResult | null>(null)

const myVehicles = ref<VehicleData[]>([])
const selectedVehicleId = ref<number | null>(null)
const saveToFleet = ref(false)
const vehiclesLoaded = ref(false)

onMounted(async () => {
  if (isLoggedIn.value) {
    try {
      const profile = await fetchRacerProfile(authHeaders())
      form.value.firstName = profile.firstName
      form.value.lastName = profile.lastName
      form.value.email = profile.email
      form.value.phone = profile.phone
    } catch { /* not critical */ }
    try {
      myVehicles.value = await fetchVehicles(authHeaders())
    } catch { /* not critical */ }
    vehiclesLoaded.value = true
  }
})

function selectVehicle(id: number | string) {
  const v = myVehicles.value.find(x => x.id === Number(id))
  if (!v) return
  form.value.vehicleCategory = v.vehicleCategory
  form.value.vehicleMake = v.vehicleMake
  form.value.vehiclePlate = v.vehiclePlate
  form.value.vehicleYear = v.vehicleYear
}

watch(selectedVehicleId, (id) => {
  if (id) selectVehicle(id)
})

const hasVehicles = computed(() => myVehicles.value.length > 0)

let emailLookupTimer: ReturnType<typeof setTimeout> | null = null

async function lookupEmail(email: string, target: { firstName: string; lastName: string; phone: string }) {
  if (!email || !email.includes('@')) return
  try {
    const user = await lookupUserByEmail(email)
    if (user && user.firstName) {
      target.firstName = target.firstName || user.firstName
      target.lastName = target.lastName || user.lastName
      target.phone = target.phone || user.phone
    }
  } catch { /* ignore */ }
}

function onEmailInput(email: string, target: { firstName: string; lastName: string; phone: string }) {
  if (emailLookupTimer) clearTimeout(emailLookupTimer)
  emailLookupTimer = setTimeout(() => lookupEmail(email, target), 500)
}

const selectedVariant = computed(() => VARIANTS.find(v => v.value === form.value.variant))

const feeConfig = computed(() => FEE[form.value.variant])

const isVintage = computed(() => form.value.vehicleYear < 1945)

const totalFee = computed(() => {
  if (!feeConfig.value) return 0
  const base = isVintage.value ? feeConfig.value.baseDo1945 : feeConfig.value.baseOd1946
  return base + feeConfig.value.extraPerson * Math.max(0, form.value.crewCount - 1)
})

const feeBreakdown = computed(() => {
  if (!feeConfig.value) return null
  const base = isVintage.value ? feeConfig.value.baseDo1945 : feeConfig.value.baseOd1946
  return { base, extra: form.value.crewCount > 1 ? feeConfig.value.extraPerson * (form.value.crewCount - 1) : 0 }
})

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    result.value = await submitRegistration({
      teamName: form.value.teamName,
      email: form.value.email,
      phone: form.value.phone,
      vehicleCategory: form.value.vehicleCategory,
      vehiclePlate: form.value.vehiclePlate,
      vehicleYear: form.value.vehicleYear,
      crewCount: form.value.crewCount,
      variant: form.value.variant,
      firstName: form.value.firstName,
      lastName: form.value.lastName,
      driverAge: form.value.driverAge,
      gender: form.value.gender,
      address: form.value.address,
      club: driverClubMember.value ? 'ano' : '',
      firstTime: form.value.firstTime,
      crewMembers: crewMembers.value.filter(m => m.firstName && m.lastName && m.email),
    })
    submitted.value = true
    if (saveToFleet.value && isLoggedIn.value) {
      try {
        await createVehicle({
          vehicleCategory: form.value.vehicleCategory,
          vehicleMake: form.value.vehicleMake,
          vehiclePlate: form.value.vehiclePlate,
          vehicleYear: form.value.vehicleYear,
        }, authHeaders())
      } catch { /* non-critical */ }
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba při odesílání'
  } finally {
    loading.value = false
  }
}

const qrUrl = computed(() => {
  if (!result.value) return ''
  const sn = result.value.startNumber > 0 ? result.value.startNumber : result.value.id
  const data = `bydzov-ctverec:${sn}:${result.value.id}`
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(data)}`
})
</script>

<template>
  <div>
    <h1 class="text-page-title text-text">Přihláška do soutěže</h1>
    <p class="mt-2 text-body-lg text-text-muted">30. ročník Novobydžovského čtverce – Memoriál Elišky Junkové</p>

    <!-- Success -->
    <div v-if="submitted && result" class="mt-8 space-y-6 max-w-form">
      <div class="alert alert-success">
        <p class="font-semibold">Přihláška přijata ✓</p>
        <p class="mt-1">Děkujeme za přihlášení.</p>
        <p class="mt-2" v-if="result.startNumber > 0">
          Vaše startovní číslo: <span class="text-kpi text-primary">#{{ result.startNumber }}</span>
        </p>
        <p class="mt-2" v-else>
          Startovní číslo bude přiděleno po zaplacení.
        </p>
        <p class="mt-2">Všem členům posádky byl vytvořen uživatelský účet. Přihlašovací údaje byly odeslány na jejich e-maily.</p>
      </div>

      <div class="card">
        <h2 class="text-subsection text-text">Platební údaje</h2>
        <div class="mt-4 space-y-2">
          <p class="text-body text-text-muted">Částka: <strong class="text-text">{{ result.startFee }} Kč</strong></p>
          <p class="text-body text-text-muted">Bankovní účet: <span class="font-mono font-semibold text-text">1086360369/0800</span></p>
          <p class="text-body text-text-muted">Variabilní symbol: <span class="font-mono font-semibold text-text">{{ result.paymentReference }}</span></p>
          <p class="text-meta text-text-soft mt-2">Splatnost startovného je 14 dnů od vyplnění přihlášky, nejpozději však do uzávěrky přihlášky.</p>
        </div>
      </div>

      <div class="card text-center">
        <h2 class="text-subsection text-text mb-4">QR kód pro prezenci</h2>
        <img :src="qrUrl" alt="QR kód" class="mx-auto rounded-md" width="200" height="200" />
        <p class="mt-3 text-meta text-text-soft">Předložte při prezenci v den závodu</p>
      </div>
    </div>

    <!-- Form -->
    <form v-else @submit.prevent="handleSubmit" class="mt-6 space-y-5 max-w-form">
      <!-- Variant -->
      <div>
        <label class="input-label">Varianta závodu</label>
        <div class="space-y-2">
          <label v-for="v in VARIANTS" :key="v.value"
            class="flex items-start gap-3 rounded-lg border border-border p-4 cursor-pointer transition-colors"
            :class="form.variant === v.value ? 'border-primary bg-primary/5' : 'hover:bg-bg-alt'"
          >
            <input type="radio" :value="v.value" v-model="form.variant" class="mt-0.5 accent-primary" />
            <div>
              <span class="font-medium text-text">{{ v.label }}</span>
              <p class="text-meta text-text-soft">Uzávěrka přihlášek: {{ v.deadline }}</p>
            </div>
          </label>
        </div>
      </div>

      <!-- Team name -->
      <div>
        <label class="input-label">Název posádky</label>
        <input v-model="form.teamName" required class="input-field" placeholder="např. Tým Pardubice" />
      </div>

      <!-- Driver name -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="input-label">Jméno řidiče</label>
          <input v-model="form.firstName" required class="input-field" placeholder="Jan" />
        </div>
        <div>
          <label class="input-label">Příjmení</label>
          <input v-model="form.lastName" required class="input-field" placeholder="Novák" />
        </div>
      </div>

      <!-- Driver mandatory info -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div>
          <label class="input-label">Věk v den závodu</label>
          <input v-model.number="form.driverAge" type="number" required min="1" class="input-field" placeholder="např. 35" />
        </div>
        <div>
          <label class="input-label">Pohlaví</label>
          <select v-model="form.gender" required class="input-field">
            <option value="" disabled>Vyberte</option>
            <option value="M">Muž</option>
            <option value="Z">Žena</option>
          </select>
        </div>
        <div>
          <label class="input-label">Bydliště</label>
          <input v-model="form.address" required class="input-field" placeholder="Město" />
        </div>
      </div>

      <div class="flex flex-wrap gap-6">
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" v-model="form.firstTime" class="accent-primary" />
          <span class="text-body-sm text-text-muted">Jedete závod poprvé?</span>
        </label>
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" v-model="driverClubMember" class="accent-primary" />
          <span class="text-body-sm text-text-muted">Jste členem klubu?</span>
        </label>
      </div>
      <div v-if="driverClubMember">
        <label class="input-label">Název klubu</label>
        <input v-model="form.club" class="input-field" placeholder="např. AMK Pardubice" />
      </div>

      <!-- Contact -->
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="input-label">E-mail</label>
          <input v-model="form.email" type="email" required class="input-field" :class="{ 'opacity-60': isLoggedIn }" placeholder="posadka@example.cz" :readonly="isLoggedIn" @input="!isLoggedIn && onEmailInput(form.email, form)" />
          <p v-if="isLoggedIn" class="text-meta text-text-soft mt-1">E-mail je převzat z tvého účtu</p>
        </div>
        <div>
          <label class="input-label">Telefon</label>
          <input v-model="form.phone" type="tel" required class="input-field" placeholder="+420 777 123 456" />
        </div>
      </div>

      <!-- Vehicle -->
      <div v-if="hasVehicles" class="rounded-lg border border-border bg-bg-alt p-4">
        <label class="input-label">Vybrat z mých vozidel</label>
        <select v-model="selectedVehicleId" class="input-field w-full mt-1">
          <option :value="null">– Nové vozidlo –</option>
          <option v-for="v in myVehicles" :key="v.id" :value="v.id">
            {{ v.vehicleMake }} ({{ v.vehiclePlate }}) – {{ v.vehicleYear }}
          </option>
        </select>
      </div>

      <div>
        <label class="input-label">Typ vozidla</label>
        <div class="flex flex-col sm:flex-row gap-3">
          <label v-for="t in VEHICLE_TYPES" :key="t.value"
            class="flex-1 flex items-center justify-center gap-2 rounded-lg border border-border p-3 cursor-pointer transition-colors"
            :class="form.vehicleCategory === t.value ? 'border-primary bg-primary/5 text-primary' : 'hover:bg-bg-alt'"
          >
            <input type="radio" :value="t.value" v-model="form.vehicleCategory" required class="accent-primary" />
            <span class="font-medium">{{ t.label }}</span>
          </label>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="input-label">Značka a typ vozidla</label>
          <input v-model="form.vehicleMake" required class="input-field" placeholder="Škoda 1000 MB" />
        </div>
        <div>
          <label class="input-label">SPZ</label>
          <input v-model="form.vehiclePlate" required class="input-field" placeholder="5H1 2345" />
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="input-label">Rok výroby</label>
          <input v-model.number="form.vehicleYear" type="number" required min="1900" max="1989" class="input-field" />
          <p class="text-meta text-text-soft mt-1">Vozidlo musí být do roku 1989</p>
        </div>
        <div>
          <label class="input-label">Počet členů posádky</label>
          <input v-model.number="form.crewCount" type="number" required min="1" max="10" class="input-field" />
        </div>
      </div>

      <label v-if="isLoggedIn" class="flex items-center gap-2 cursor-pointer">
        <input type="checkbox" v-model="saveToFleet" class="accent-primary" />
        <span class="text-body-sm text-text-muted">Uložit vozidlo do mého vozového parku</span>
      </label>

      <!-- Crew members -->
      <div v-if="crewMembers.length > 0" class="space-y-4">
        <p class="text-label text-text">Ostatní členové posádky</p>
        <p class="text-meta text-text-soft -mt-2">Každému bude vytvořen uživatelský účet a na e-mail obdrží přihlašovací údaje.</p>
        <div v-for="(cm, i) in crewMembers" :key="i"
          class="rounded-lg border border-border bg-bg-alt p-4 space-y-3">
          <p class="text-meta font-semibold text-text-muted">Člen posádky {{ i + 1 }}</p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label class="input-label">Jméno</label>
              <input v-model="cm.firstName" required class="input-field" placeholder="Karel" />
            </div>
            <div>
              <label class="input-label">Příjmení</label>
              <input v-model="cm.lastName" required class="input-field" placeholder="Novák" />
            </div>
          </div>
          <div>
            <label class="input-label">E-mail</label>
            <input v-model="cm.email" type="email" required class="input-field" placeholder="clen@example.cz" @input="onEmailInput(cm.email, cm)" />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label class="input-label">Věk v den závodu</label>
              <input v-model.number="cm.driverAge" type="number" required min="1" class="input-field" />
            </div>
            <div>
              <label class="input-label">Pohlaví</label>
              <select v-model="cm.gender" required class="input-field">
                <option value="" disabled>Vyberte</option>
                <option value="M">Muž</option>
                <option value="Z">Žena</option>
              </select>
            </div>
            <div>
              <label class="input-label">Bydliště</label>
              <input v-model="cm.address" required class="input-field" placeholder="Město" />
            </div>
          </div>
          <div class="flex flex-wrap gap-6">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" v-model="cm.firstTime" class="accent-primary" />
              <span class="text-body-sm text-text-muted">První účast?</span>
            </label>
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" v-model="cm.clubMember" class="accent-primary" />
              <span class="text-body-sm text-text-muted">Člen klubu?</span>
            </label>
          </div>
          <div v-if="cm.clubMember">
            <label class="input-label">Název klubu</label>
            <input v-model="cm.clubName" class="input-field" placeholder="např. AMK Pardubice" />
          </div>
        </div>
      </div>

      <!-- Fee breakdown -->
      <div v-if="selectedVariant && form.vehicleCategory && form.vehicleYear" class="rounded-lg border border-border bg-bg-alt p-4 space-y-1 text-body-sm">
        <div class="flex items-center justify-between">
          <span class="text-text-soft">Varianta:</span>
          <span class="text-text font-medium">{{ selectedVariant.label }}</span>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-text-soft">Vozidlo:</span>
          <span class="text-text" :class="isVintage ? 'text-red font-semibold' : ''">{{ isVintage ? 'do 1945 (veterán)' : 'od 1946' }}</span>
        </div>
        <hr class="border-border my-1" />
        <div class="flex items-center justify-between">
          <span class="text-text-soft">Základ (řidič):</span>
          <span class="text-text font-mono">{{ feeBreakdown?.base }} Kč</span>
        </div>
        <div v-if="feeBreakdown?.extra" class="flex items-center justify-between">
          <span class="text-text-soft">{{ form.crewCount - 1 }} osoba/y navíc:</span>
          <span class="text-text font-mono">+ {{ feeBreakdown.extra }} Kč</span>
        </div>
        <hr class="border-border my-1" />
        <div class="flex items-center justify-between">
          <span class="font-semibold text-text">Celkem:</span>
          <span class="text-kpi text-primary">{{ totalFee }} Kč</span>
        </div>
      </div>

      <!-- Consent -->
      <label class="flex items-start gap-3 cursor-pointer">
        <input type="checkbox" v-model="form.consent" required class="mt-1 accent-primary" />
        <span class="text-body-sm text-text-muted">
          Souhlasím se zpracováním osobních údajů pro účely organizace závodu.
        </span>
      </label>

      <p v-if="error" class="text-body-sm text-error">{{ error }}</p>

      <button type="submit" :disabled="loading || !form.consent" class="btn-primary w-full">
        {{ loading ? 'Odesílám…' : 'Odeslat přihlášku' }}
      </button>
    </form>
  </div>
</template>
