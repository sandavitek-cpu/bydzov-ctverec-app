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
    const res = await fetch(`${apiBaseUrl}/api/racer/scores`, { headers: authHeaders() })
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
  <div class="mx-auto max-w-lg">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-white">Můj stav</h1>
      <button
        @click="logout(); router.push('/admin/login')"
        class="text-sm text-slate-500 hover:text-slate-300"
      >
        Odhlásit
      </button>
    </div>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="error" class="mt-8 text-red-400">{{ error }}</p>

    <template v-else-if="data">
      <div class="mt-6 grid grid-cols-3 gap-3">
        <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4 text-center">
          <p class="text-sm text-slate-500">Pořadí</p>
          <p class="mt-1 text-3xl font-bold text-amber-400">
            {{ data.rank }}<span class="text-lg text-slate-500">/{{ data.totalRacers }}</span>
          </p>
        </div>
        <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4 text-center">
          <p class="text-sm text-slate-500">Body</p>
          <p class="mt-1 text-3xl font-bold text-white">{{ data.totalPoints }}</p>
        </div>
        <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-4 text-center">
          <p class="text-sm text-slate-500">#{{ data.startNumber }}</p>
          <p class="mt-1 text-sm font-medium text-white truncate">{{ data.teamName }}</p>
        </div>
      </div>

      <div v-if="data.scores.length > 0" class="mt-6">
        <h2 class="text-sm font-medium text-slate-400">Jízdy</h2>
        <div class="mt-2 space-y-2">
          <div
            v-for="s in data.scores"
            :key="s.runNumber"
            class="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-900/40 px-4 py-3"
          >
            <span class="text-slate-400">{{ s.runNumber }}. jízda</span>
            <span class="font-mono text-lg font-bold text-white">{{ s.points }} b.</span>
          </div>
        </div>
      </div>
      <p v-else class="mt-6 text-sm text-slate-500">Zatím žádné body.</p>
    </template>

    <div v-else-if="!registered" class="mt-8 rounded-xl border border-amber-800 bg-amber-900/20 p-6 text-center">
      <p class="text-amber-400">Nejste přihlášen k závodu.</p>
      <p class="mt-1 text-sm text-slate-500">Zaregistrujte se přes formulář.</p>
    </div>
  </div>
</template>
