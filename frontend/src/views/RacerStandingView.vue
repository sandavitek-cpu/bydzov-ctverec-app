<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, fetchRacerStatus } from '@/api'
import QrPayment from '@/components/QrPayment.vue'

const router = useRouter()
const { isLoggedIn, authHeaders, logout } = useAuth()

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

const data = ref<StandingData | null>(null)
const regStatus = ref<{
  id: number; startFee: number; status: string; variant: string
  startNumber: number; teamName: string
} | null>(null)
const raceStatus = ref<{ started: boolean; finished: boolean } | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const registered = ref(true)
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

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>
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

      <!-- Payment -->
      <div v-if="regStatus" class="card !p-6">
        <h2 class="text-subsection text-text mb-4">Startovné</h2>
        <div class="flex items-center justify-between mb-4">
          <span class="text-body text-text-muted">Částka k úhradě</span>
          <span class="text-kpi text-primary">{{ regStatus.startFee }} Kč</span>
        </div>
        <div class="flex items-center justify-between mb-4">
          <span class="text-body text-text-muted">Stav platby</span>
          <span v-if="regStatus.status === 'PAID'" class="badge !bg-success/10 !text-success">Zaplaceno</span>
          <span v-else class="badge badge-admin">Čeká na platbu</span>
        </div>

        <template v-if="regStatus.status !== 'PAID'">
          <div class="flex flex-col items-center gap-4 mt-4 pt-4 border-t border-border">
            <QrPayment
              :amount="regStatus.startFee"
              :variable-symbol="regStatus.startNumber > 0 ? regStatus.startNumber : regStatus.id"
              :message="'VS:' + (regStatus.startNumber > 0 ? regStatus.startNumber : regStatus.id)"
            />
            <p class="text-body-sm text-text-soft text-center">
              Načtěte QR kód v mobilním bankovnictví nebo zašlete platbu na účet
              <strong class="text-text">1086360369/0800</strong>
              s variabilním symbolem <strong class="text-text">{{ regStatus.startNumber > 0 ? regStatus.startNumber : regStatus.id }}</strong>.
            </p>
            <button disabled class="btn-primary w-full opacity-50 cursor-not-allowed py-4">
              Zaplatit kartou
            </button>
            <p class="text-meta text-text-soft text-center">Online platba bude brzy dostupná.</p>
          </div>
        </template>
      </div>
    </template>

    <div v-else-if="!registered" class="card text-center">
      <p class="text-subsection text-warning">Nejste přihlášen k závodu</p>
      <p class="text-body text-text-muted mt-2">Zaregistrujte se přes formulář.</p>
    </div>
  </div>
</template>
