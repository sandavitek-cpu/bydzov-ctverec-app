<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { apiBaseUrl, fetchResults, type ResultRow } from '@/api'
import { Client, type IFrame } from '@stomp/stompjs'

const route = useRoute()
const year = Number(route.params.rok) || 2026

const results = ref<ResultRow[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const wsConnected = ref(false)
let interval: ReturnType<typeof setInterval> | null = null
let stompClient: Client | null = null

const wsUrl = apiBaseUrl.replace(/^https?/, 'ws') + '/ws/results'

async function load() {
  try {
    const data = await fetchResults(year)
    results.value = data.results
    error.value = null
  } catch (e) {
    if (!error.value) {
      error.value = e instanceof Error ? e.message : 'Chyba načítání'
    }
  } finally {
    loading.value = false
  }
}

function onWsMessage(frame: IFrame) {
  try {
    const data = JSON.parse(frame.body)
    if (data.year === year) {
      results.value = data.results
      error.value = null
    }
  } catch {
  }
}

onMounted(() => {
  load()

  stompClient = new Client({
    brokerURL: wsUrl,
    reconnectDelay: 5000,
  })

  stompClient.onConnect = () => {
    wsConnected.value = true
    stompClient!.subscribe('/topic/results', onWsMessage)
  }

  stompClient.onDisconnect = () => {
    wsConnected.value = false
  }

  stompClient.onStompError = () => {
    wsConnected.value = false
  }

  stompClient.activate()

  interval = setInterval(load, 15000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
  if (stompClient) stompClient.deactivate()
})

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Moto', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-page-title text-text">Výsledky {{ year }}</h1>
        <p class="text-body-sm text-text-soft flex items-center gap-2">
          <span :class="wsConnected ? 'text-success' : 'text-text-soft'" class="inline-block h-2 w-2 rounded-full"></span>
          {{ wsConnected ? 'Live — WebSocket' : 'Aktualizace každých 15 s' }}
        </p>
      </div>
      <div v-if="results.length > 0" class="text-meta text-text-soft">
        {{ results.length }} závodník{{ results.length !== 1 ? 'ů' : '' }}
      </div>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-12 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <div v-else-if="results.length === 0" class="py-12 text-center">
      <p class="text-section-title text-text-soft">Zatím žádné výsledky</p>
      <p class="text-body text-text-soft mt-2">Jakmile budou první body zapsány, objeví se zde.</p>
    </div>

    <div v-else class="overflow-x-auto rounded-xl border border-border">
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th class="w-10 text-center">#</th>
            <th>Start. č.</th>
            <th>Posádka</th>
            <th class="hidden sm:table-cell">Vozidlo</th>
            <th class="text-right">Body</th>
            <th class="hidden md:table-cell text-right">Jízdy</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in results" :key="r.startNumber" class="table-row">
            <td class="text-center">
              <span
                class="inline-flex h-7 w-7 items-center justify-center rounded-full text-body-sm font-bold"
                :class="r.rank === 1 ? 'rank-1' : r.rank === 2 ? 'rank-2' : r.rank === 3 ? 'rank-3' : 'text-text-soft'"
              >{{ r.rank }}</span>
            </td>
            <td class="font-mono font-bold text-primary">{{ r.startNumber }}</td>
            <td>
              <div class="font-medium text-text">{{ r.teamName }}</div>
              <div class="text-meta text-text-soft sm:hidden">{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }} · {{ r.vehiclePlate }}</div>
            </td>
            <td class="hidden sm:table-cell">
              <div class="text-text-muted">{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }}</div>
              <div class="text-meta text-text-soft">{{ r.vehiclePlate }}</div>
            </td>
            <td class="text-right font-mono text-kpi text-text">{{ r.totalPoints }}</td>
            <td class="hidden md:table-cell text-right">
              <span
                v-for="run in r.runs"
                :key="run.runNumber"
                class="ml-1 inline-block rounded-md bg-surface-strong px-2 py-0.5 text-meta text-text-muted"
              >{{ run.runNumber }}:{{ run.points }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
