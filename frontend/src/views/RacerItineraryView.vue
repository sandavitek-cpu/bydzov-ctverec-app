<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchRacerItinerary } from '@/api'
import type { ItineraryResponse } from '@/api'
import { apiBaseUrl } from '@/api'
import { Client } from '@stomp/stompjs'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

const data = ref<ItineraryResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const nowStr = ref('')
let stompClient: Client | null = null
let timeInterval: ReturnType<typeof setInterval> | null = null

function updateNow() {
  const n = new Date()
  nowStr.value = `${String(n.getHours()).padStart(2, '0')}:${String(n.getMinutes()).padStart(2, '0')}`
}

const currentIndex = computed(() => {
  if (!data.value) return -1
  return data.value.schedule.findIndex(i => i.time > nowStr.value)
})

const progressPct = computed(() => {
  if (!data.value || data.value.checkpoints.length === 0) return 0
  return Math.round((data.value.passedCount / data.value.checkpoints.length) * 100)
})

const totalScore = computed(() => {
  if (!data.value) return 0
  return data.value.checkpoints.reduce((sum, c) => sum + (c.scorePoints ?? 0), 0)
})

const maxPossibleScore = computed(() => {
  if (!data.value) return 0
  return data.value.checkpoints.reduce((sum, c) => sum + (c.maxPoints ?? 0), 0)
})

async function load() {
  try {
    const res = await fetchRacerItinerary(authHeaders())
    data.value = res
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function onResultsUpdate(_frame: { body: string }) {
  load()
}

function onScheduleUpdate(frame: { body: string }) {
  try {
    const parsed = JSON.parse(frame.body)
    if (parsed.items && data.value) {
      data.value.schedule = parsed.items
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.push('/admin/login')
    return
  }
  updateNow()
  load()

  const wsUrl = apiBaseUrl.replace(/^https?/, 'ws') + '/ws/results'
  stompClient = new Client({ brokerURL: wsUrl, reconnectDelay: 5000 })
  stompClient.onConnect = () => {
    stompClient!.subscribe('/topic/schedule', onScheduleUpdate)
    stompClient!.subscribe('/topic/results', onResultsUpdate)
  }
  stompClient.activate()

  timeInterval = setInterval(updateNow, 30000)
})

onUnmounted(() => {
  if (stompClient) stompClient.deactivate()
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <LoadingSpinner v-if="loading" />
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <template v-if="data">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-page-title text-text">Itinerář</h1>
          <p class="text-body text-text-muted">{{ data.teamName ?? 'Bez týmu' }} <span v-if="data.startNumber" class="ml-2 text-meta text-text-soft">#{{ data.startNumber }}</span></p>
        </div>
      </div>

      <div v-if="data.checkpoints.length > 0" class="card mb-6">
        <h2 class="text-section-title text-text mb-3">Postup kontrolami</h2>
        <div class="flex items-center gap-3 mb-4">
          <div class="flex-1 h-2 rounded-full bg-bg-alt overflow-hidden">
            <div class="h-full rounded-full bg-primary transition-all" :style="{ width: progressPct + '%' }"></div>
          </div>
          <span class="text-body-sm text-text-soft whitespace-nowrap">{{ data.passedCount }} / {{ data.checkpoints.length }} ({{ progressPct }} %)</span>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left">
            <thead>
              <tr class="text-meta text-text-soft border-b border-border">
                <th class="py-2 pr-3">#</th>
                <th class="py-2 pr-3">Kontrola</th>
                <th class="py-2 pr-3 text-right">Body</th>
                <th class="py-2 pr-3 text-right">Max</th>
                <th class="py-2 pr-3">Kontakt</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cp in data.checkpoints" :key="cp.sortOrder"
                class="border-b border-border/50 transition-colors hover:bg-bg-alt"
                :class="{ 'bg-success/5': cp.scorePoints != null }"
              >
                <td class="py-2 pr-3 text-body-sm text-text-muted">{{ cp.sortOrder }}</td>
                <td class="py-2 pr-3">
                  <p class="text-body-sm font-medium text-text">{{ cp.name }}</p>
                  <p v-if="cp.taskDescription" class="text-meta text-text-soft">{{ cp.taskDescription }}</p>
                </td>
                <td class="py-2 pr-3 text-right">
                  <span v-if="cp.scorePoints != null" class="text-body-sm font-semibold text-success">{{ cp.scorePoints }}</span>
                  <span v-else class="text-body-sm text-text-soft">—</span>
                </td>
                <td class="py-2 pr-3 text-right text-body-sm text-text-muted">{{ cp.maxPoints ?? '—' }}</td>
                <td class="py-2 pr-3 text-body-sm text-text-muted">
                  <template v-if="cp.phone || cp.volunteers.length > 0">
                    <p v-if="cp.phone" class="font-medium text-text">{{ cp.phone }}</p>
                    <p v-if="cp.volunteers.length > 0" class="text-meta">{{ cp.volunteers.join(', ') }}</p>
                  </template>
                  <span v-else class="text-text-soft">—</span>
                </td>
              </tr>
              <tr class="font-semibold text-text">
                <td colspan="2" class="py-2 pr-3 text-body-sm">Celkem</td>
                <td class="py-2 pr-3 text-right text-body-sm text-success">{{ totalScore }}</td>
                <td class="py-2 pr-3 text-right text-body-sm text-text">{{ maxPossibleScore }}</td>
                <td></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="data.route" class="card mb-6">
        <h2 class="text-section-title text-text mb-2">Trasa</h2>
        <p class="text-body text-text-muted">{{ data.route.name }}</p>
        <p class="text-body-sm text-text-soft mt-1">
          Délka: {{ (data.route.totalDistance / 1000).toFixed(1) }} km
          <span v-if="data.route.pointCount > 0"> &middot; {{ data.route.pointCount }} bodů trasy</span>
        </p>
      </div>

      <div v-if="data.contact" class="card mb-6">
        <h2 class="text-section-title text-text mb-2">Důležité kontakty</h2>
        <div v-if="data.contact.towPhone" class="flex items-center gap-3 p-3 rounded-lg bg-error/5 border border-error/20">
          <svg class="h-5 w-5 text-error shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 5.636a9 9 0 010 12.728m-2.829-2.829a5 5 0 010-7.07m-4.243 4.243a1 1 0 010-1.414" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
          </svg>
          <div>
            <p class="text-body-sm font-semibold text-text">Odtahová služba</p>
            <p class="text-body font-medium text-text"><a :href="'tel:' + data.contact.towPhone" class="text-primary hover:underline">{{ data.contact.towPhone }}</a></p>
            <p v-if="data.contact.towNote" class="text-meta text-text-soft">{{ data.contact.towNote }}</p>
          </div>
        </div>
      </div>

      <div v-if="data.schedule.length > 0" class="card">
        <h2 class="text-section-title text-text mb-4">Program</h2>
        <div class="relative">
          <div class="absolute left-5 top-0 bottom-0 w-px bg-border"></div>
          <div v-for="(item, i) in data.schedule" :key="item.id ?? i" class="relative flex gap-5 pb-8 last:pb-0">
            <div class="relative z-10 flex flex-col items-center">
              <div
                class="flex h-10 w-10 items-center justify-center rounded-full text-label font-bold border-2 transition-all"
                :class="i < (currentIndex >= 0 ? currentIndex : data.schedule.length)
                  ? 'bg-surface-strong border-border text-text-soft'
                  : i === currentIndex
                    ? 'bg-surface border-primary text-primary shadow-md'
                    : 'bg-surface-2 border-border text-text-muted'"
              >{{ item.time.replace(':', '') }}</div>
            </div>
            <div class="flex-1 pt-1.5">
              <p class="font-semibold" :class="i === currentIndex ? 'text-primary' : 'text-text'">{{ item.label }}</p>
              <p v-if="item.description" class="mt-0.5 text-body-sm text-text-muted">{{ item.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
