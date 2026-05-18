<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

interface ScheduleItem {
  id?: number
  time: string
  label: string
  description: string | null
  sortOrder?: number
}

const items = ref<ScheduleItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const reg = ref<{ teamName: string | null; startNumber: number | null } | null>(null)
let ws: WebSocket | null = null

const now = new Date()
const today = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
const currentIndex = computed(() =>
  items.value.findIndex(i => i.time > today)
)

async function load() {
  try {
    const [scheduleRes, regRes] = await Promise.all([
      fetch(`${apiBaseUrl}/api/racer/schedule`, { headers: authHeaders() }),
      fetch(`${apiBaseUrl}/api/racer/registration`, { headers: authHeaders() }),
    ])
    if (scheduleRes.ok) items.value = await scheduleRes.json()
    if (regRes.ok) reg.value = await regRes.json()
    if (!scheduleRes.ok && !regRes.ok) {
      error.value = `API ${scheduleRes.status}`
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function connectWebSocket() {
  const wsUrl = apiBaseUrl.replace(/^http/, 'ws') + '/ws/results'
  ws = new WebSocket(wsUrl)
  ws.onopen = () => {
    const connectFrame = `CONNECT\naccept-version:1.2\nhost:${new URL(apiBaseUrl).host}\n\n\x00`
    ws?.send(connectFrame)
    const subFrame = `SUBSCRIBE\nid:sub-schedule\ndestination:/topic/schedule\n\n\x00`
    ws?.send(subFrame)
  }
  ws.onmessage = (event: MessageEvent) => {
    const data = event.data as string
    if (data.includes('/topic/schedule') || data.includes('MESSAGE')) {
      try {
        const bodyMatch = data.match(/({.*})/)
        if (bodyMatch) {
          const parsed = JSON.parse(bodyMatch[1])
          if (parsed.items) {
            items.value = parsed.items
          }
        }
      } catch { /* ignore parse errors */ }
    }
  }
  ws.onclose = () => {
    setTimeout(connectWebSocket, 3000)
  }
}

onMounted(() => {
  load()
  connectWebSocket()
})

onUnmounted(() => {
  if (ws) {
    ws.onclose = null
    ws.close()
  }
})

if (!isLoggedIn.value) {
  router.push('/admin/login')
}
</script>

<template>
  <div class="max-w-form mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-page-title text-text">Itinerář</h1>
        <p v-if="reg?.teamName" class="text-body text-text-muted">{{ reg.teamName }}</p>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <div v-else class="relative">
      <div class="absolute left-5 top-0 bottom-0 w-px bg-border"></div>

      <div v-for="(item, i) in items" :key="item.id ?? i" class="relative flex gap-5 pb-8">
        <div class="relative z-10 flex flex-col items-center">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full text-label font-bold border-2 transition-all"
            :class="i < (currentIndex >= 0 ? currentIndex : items.length)
              ? 'bg-surface-strong border-border text-text-soft'
              : i === currentIndex
                ? 'bg-surface border-primary text-primary shadow-md'
                : 'bg-surface-2 border-border text-text-muted'"
          >{{ item.time.replace(':', '') }}</div>
        </div>
        <div class="flex-1 pt-1.5">
          <p
            class="font-semibold"
            :class="i === currentIndex ? 'text-primary' : 'text-text'"
          >{{ item.label }}</p>
          <p v-if="item.description" class="mt-0.5 text-body-sm text-text-muted">{{ item.description }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
