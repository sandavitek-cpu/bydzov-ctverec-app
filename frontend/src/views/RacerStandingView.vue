<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl, fetchRacerStatus } from '@/api'
import QrPayment from '@/components/QrPayment.vue'

const router = useRouter()
const { isLoggedIn, authHeaders, logout } = useAuth()
const { show: showToast } = useToast()

interface ScoreRow {
  checkpointName: string
  checkpointOrder: number
  points: number
}

interface StandingData {
  teamName: string
  startNumber: number
  totalPoints: number
  rank: number
  totalRacers: number
  scores: ScoreRow[]
}

const FEE: Record<string, { baseDo1945: number; baseOd1946: number; extraPerson: number }> = {
  JEDNODENNI: { baseDo1945: 500, baseOd1946: 800, extraPerson: 500 },
  DVODENNI_UZAVRENO: { baseDo1945: 1000, baseOd1946: 1200, extraPerson: 1000 },
  DVODENNI_BEZ_UBYTOVANI: { baseDo1945: 600, baseOd1946: 900, extraPerson: 600 },
}

const data = ref<StandingData | null>(null)
const regStatus = ref<{
  id: number; paymentReference: number; startFee: number; paidAmount: number | null; status: string; variant: string
  startNumber: number; teamName: string; vehicleYear: number; vehicleMake: string; crewCount: number
  vehicleCategory: string; vehiclePlate: string; cancelledAt: string | null; refundAmount: number | null
} | null>(null)
const raceStatus = ref<{ started: boolean; finished: boolean } | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const registered = ref(true)
const editing = ref(false)
const editForm = ref<Record<string, any>>({})
const saving = ref(false)
const editMessage = ref<string | null>(null)
let interval: ReturnType<typeof setInterval> | null = null

async function load() {
  try {
    const [regRes, statusRes] = await Promise.all([
      fetch(`${apiBaseUrl}/api/racer/registration`, { headers: authHeaders() }),
      fetchRacerStatus(authHeaders()),
    ])
    if (regRes.status === 403) {
      logout()
      router.push('/admin/login')
      return
    }
    const body = await regRes.json()
    if (body.error) {
      registered.value = false
    } else {
      data.value = body as StandingData
      registered.value = true
      regStatus.value = statusRes
    }
    error.value = null
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function fetchRaceStatus() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/public/info`)
    if (res.ok) {
      const info = await res.json()
      raceStatus.value = info.race
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  load()
  fetchRaceStatus()
  interval = setInterval(() => {
    load()
    fetchRaceStatus()
  }, 10000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})

const previewFee = computed(() => {
  const variant = editForm.value.variant
  const cfg = variant && FEE[variant] ? FEE[variant] : null
  if (!cfg) return 0
  const year = editForm.value.vehicleYear || 0
  const base = year < 1945 ? cfg.baseDo1945 : cfg.baseOd1946
  return base + cfg.extraPerson * Math.max(0, (editForm.value.crewCount || 1) - 1)
})

function startEdit() {
  if (!regStatus.value) return
  editForm.value = {
    variant: regStatus.value.variant,
    vehicleMake: regStatus.value.vehicleMake,
    vehicleYear: regStatus.value.vehicleYear,
    crewCount: regStatus.value.crewCount,
  }
  editMessage.value = null
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  editMessage.value = null
}

async function saveEdit() {
  saving.value = true
  editMessage.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/racer/registration`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(editForm.value),
    })
    if (!res.ok) throw new Error()
    const result = await res.json()
    if (regStatus.value) {
      regStatus.value.startFee = result.startFee
      regStatus.value.status = result.status
      regStatus.value.variant = result.variant || ''
      regStatus.value.vehicleMake = result.vehicleMake || ''
      regStatus.value.vehicleYear = result.vehicleYear
      regStatus.value.crewCount = result.crewCount
    }
    if (result.message) {
      editMessage.value = result.message
      showToast(result.message, result.feeChanged ? 'info' : 'success')
    } else {
      showToast('Údaje uloženy', 'success')
    }
    editing.value = false
    await load()
  } catch {
    showToast('Nepodařilo se uložit změny', 'error')
  } finally {
    saving.value = false
  }
}

async function handleCancel() {
  if (!confirm('Opravdu chcete stornovat přihlášku? Tuto akci nelze vrátit.')) return
  try {
    const res = await fetch(`${apiBaseUrl}/api/racer/registration/cancel`, {
      method: 'POST',
      headers: authHeaders(),
    })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error ?? 'Storno selhalo')
    }
    const result = await res.json()
    showToast('Přihláška stornována', 'info')
    if (regStatus.value) {
      regStatus.value.status = result.status
      regStatus.value.cancelledAt = result.cancelledAt
      regStatus.value.refundAmount = result.refundAmount
    }
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Storno selhalo', 'error')
  }
}

if (!isLoggedIn.value) {
  router.push('/admin/login')
}
</script>

<template>
  <div class="max-w-form mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-page-title text-text">Můj stav</h1>
      <span v-if="raceStatus" class="text-meta" :class="raceStatus.started ? 'text-success' : 'text-text-soft'">
        {{ raceStatus.finished ? '🏁 Ukončeno' : raceStatus.started ? '🏁 Probíhá' : '⏳ Před startem' }}
      </span>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <template v-else-if="data">
      <!-- Standing -->
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-6">
        <div class="card text-center">
          <p class="text-meta text-text-soft uppercase tracking-[0.05em]">Pořadí</p>
          <p class="text-kpi text-primary mt-1">
            {{ data.rank }}<span class="text-body text-text-soft">/{{ data.totalRacers }}</span>
          </p>
        </div>
        <div class="card text-center">
          <p class="text-meta text-text-soft uppercase tracking-[0.05em]">Body</p>
          <p class="text-kpi text-text mt-1">{{ data.totalPoints }}</p>
        </div>
        <div class="card text-center">
          <p class="text-meta text-text-soft uppercase tracking-[0.05em]">#{{ data.startNumber }}</p>
          <p class="text-body font-medium text-text truncate mt-1">{{ data.teamName }}</p>
        </div>
      </div>

      <!-- Scores -->
      <div v-if="data.scores.length > 0" class="mb-6">
        <h2 class="text-subsection text-text mb-4">Stanoviště</h2>
        <div class="space-y-2">
          <div v-for="s in data.scores" :key="s.checkpointOrder"
            class="card !p-4 flex items-center justify-between"
          >
            <span class="text-text-muted">{{ s.checkpointName }}</span>
            <span class="font-mono font-bold text-text text-kpi">{{ s.points }} b.</span>
          </div>
        </div>
      </div>
      <p v-else class="text-body text-text-soft text-center py-8 mb-6">Zatím žádné body.</p>

      <!-- Edit message -->
      <div v-if="editMessage" class="alert" :class="editMessage.includes('pozastavena') ? 'alert-warning' : 'alert-success'">
        {{ editMessage }}
      </div>

      <!-- Registration details & edit -->
      <div v-if="regStatus" class="card !p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-subsection text-text">Přihláška</h2>
          <button v-if="!editing" @click="startEdit" class="btn-secondary btn-xs">Upravit</button>
        </div>

        <div v-if="!editing" class="space-y-2 text-body-sm">
          <div class="flex justify-between"><span class="text-text-soft">Startovní číslo</span><span class="text-text font-bold text-primary">{{ regStatus.startNumber || '—' }}</span></div>
          <div class="flex justify-between"><span class="text-text-soft">Varianta</span><span class="text-text">{{ { JEDNODENNI: 'Jednodenní', DVODENNI_UZAVRENO: 'Dvoudenní', DVODENNI_BEZ_UBYTOVANI: 'Dvoudenní bez ubytování' }[regStatus.variant] || regStatus.variant }}</span></div>
          <div class="flex justify-between"><span class="text-text-soft">Kategorie</span><span class="text-text">{{ regStatus.vehicleCategory === 'AUTO' ? 'Automobil' : regStatus.vehicleCategory === 'MOTO' ? 'Motocykl' : regStatus.vehicleCategory }}</span></div>
          <div class="flex justify-between"><span class="text-text-soft">Vozidlo</span><span class="text-text">{{ regStatus.vehicleMake }} ({{ regStatus.vehiclePlate }})</span></div>
          <div class="flex justify-between"><span class="text-text-soft">Ročník</span><span class="text-text">{{ regStatus.vehicleYear }}</span></div>
          <div class="flex justify-between"><span class="text-text-soft">Počet osob</span><span class="text-text">{{ regStatus.crewCount }}</span></div>
        </div>

        <div v-else class="space-y-3">
          <div>
            <label class="text-label text-text-soft mb-1 block">Varianta</label>
            <select v-model="editForm.variant" class="input-field w-full">
              <option value="JEDNODENNI">Jednodenní</option>
              <option value="DVODENNI_UZAVRENO">Dvoudenní</option>
              <option value="DVODENNI_BEZ_UBYTOVANI">Dvoudenní bez ubytování</option>
            </select>
          </div>
          <div>
            <label class="text-label text-text-soft mb-1 block">Značka / typ</label>
            <input v-model="editForm.vehicleMake" class="input-field w-full" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="text-label text-text-soft mb-1 block">Ročník</label>
              <input type="number" v-model.number="editForm.vehicleYear" class="input-field w-full" />
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Počet osob</label>
              <input type="number" v-model.number="editForm.crewCount" class="input-field w-full" min="1" max="10" />
            </div>
          </div>
          <p v-if="previewFee !== regStatus.startFee" class="text-body-sm text-warning font-medium">
            Nová cena: {{ previewFee }} Kč
          </p>
          <div class="flex gap-2 pt-2">
            <button @click="saveEdit" :disabled="saving" class="btn-primary flex-1">
              {{ saving ? 'Ukládám…' : 'Uložit' }}
            </button>
            <button @click="cancelEdit" class="btn-ghost">Zrušit</button>
          </div>
        </div>
      </div>

      <!-- Payment -->
      <div v-if="regStatus" class="card !p-6">
        <h2 class="text-subsection text-text mb-4">Startovné</h2>

        <template v-if="regStatus.status === 'CANCELLED'">
          <div class="flex items-center justify-between mb-4">
            <span class="text-body text-text-muted">Stav</span>
            <span class="badge !bg-red/10 !text-red">Stornováno</span>
          </div>
          <div v-if="regStatus.refundAmount" class="flex items-center justify-between">
            <span class="text-body text-text-muted">Vráceno</span>
            <span class="text-kpi text-primary">{{ regStatus.refundAmount }} Kč</span>
          </div>
        </template>

        <template v-else>
          <div class="flex items-center justify-between mb-4">
            <span class="text-body text-text-muted">Částka k úhradě</span>
            <span class="text-kpi text-primary">{{ regStatus.startFee }} Kč</span>
          </div>
          <div class="flex items-center justify-between mb-4">
            <span class="text-body text-text-muted">Stav platby</span>
            <span v-if="regStatus.status === 'PAID'" class="badge !bg-success/10 !text-success">Přihlášen a zaplaceno</span>
            <span v-else class="badge badge-admin">Přihlášen, nezaplaceno</span>
          </div>

          <template v-if="regStatus.status !== 'PAID'">
            <div class="flex flex-col items-center gap-4 mt-4 pt-4 border-t border-border">
              <QrPayment
                :amount="regStatus.startFee"
                :variable-symbol="regStatus.paymentReference"
                :message="'VS:' + regStatus.paymentReference"
              />
              <p class="text-body-sm text-text-soft text-center">
                Načtěte QR kód v mobilním bankovnictví nebo zašlete platbu na účet
                <strong class="text-text">1086360369/0800</strong>
                s variabilním symbolem <strong class="text-text">{{ regStatus.paymentReference }}</strong>.
              </p>
              <button disabled class="btn-primary w-full opacity-50 cursor-not-allowed py-4">
                Zaplatit kartou
              </button>
              <p class="text-meta text-text-soft text-center">Online platba bude brzy dostupná.</p>
            </div>
          </template>
        </template>

        <div v-if="regStatus.status !== 'CANCELLED'" class="mt-4 pt-4 border-t border-border">
          <button @click="handleCancel" class="text-body-sm text-red hover:text-red/80">
            Stornovat přihlášku
          </button>
        </div>
      </div>
    </template>

    <div v-else-if="!registered" class="card text-center">
      <p class="text-subsection text-warning">Nejste přihlášen k závodu</p>
      <p class="text-body text-text-muted mt-2">Zaregistrujte se přes formulář.</p>
    </div>
  </div>
</template>
