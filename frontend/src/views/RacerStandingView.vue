<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders, logout } = useAuth()

interface ScoreRow {
  runNumber: number
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
const loading = ref(true)
const error = ref<string | null>(null)
const registered = ref(true)
let interval: ReturnType<typeof setInterval> | null = null

async function load() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/racer/registration`, { headers: authHeaders() })
    if (res.status === 403) {
      logout()
      router.push('/admin/login')
      return
    }
    const body = await res.json()
    if (Array.isArray(body)) {
      registered.value = false
    } else if (body.error) {
      registered.value = false
    } else {
      data.value = body as StandingData
      registered.value = true
    }
    error.value = null
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  interval = setInterval(load, 10000)
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
    </div>

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <template v-else-if="data">
      <div class="grid grid-cols-3 gap-4 mb-6">
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

      <div v-if="data.scores.length > 0">
        <h2 class="text-subsection text-text mb-4">Jízdy</h2>
        <div class="space-y-2">
          <div v-for="s in data.scores" :key="s.runNumber"
            class="card !p-4 flex items-center justify-between"
          >
            <span class="text-text-muted">{{ s.runNumber }}. jízda</span>
            <span class="font-mono font-bold text-text text-kpi">{{ s.points }} b.</span>
          </div>
        </div>
      </div>
      <p v-else class="text-body text-text-soft text-center py-8">Zatím žádné body.</p>
    </template>

    <div v-else-if="!registered" class="card text-center">
      <p class="text-subsection text-warning">Nejste přihlášen k závodu</p>
      <p class="text-body text-text-muted mt-2">Zaregistrujte se přes formulář.</p>
    </div>
  </div>
</template>
