<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl } from '@/api'
import { Client } from '@stomp/stompjs'

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
const nowStr = ref('')
let stompClient: Client | null = null
let timeInterval: ReturnType<typeof setInterval> | null = null

function updateNow() {
  const n = new Date()
  nowStr.value = `${String(n.getHours()).padStart(2, '0')}:${String(n.getMinutes()).padStart(2, '0')}`
}
const currentIndex = computed(() =>
  items.value.findIndex(i => i.time > nowStr.value)
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

function onScheduleUpdate(frame: { body: string }) {
  try {
    const data = JSON.parse(frame.body)
    if (data.items) {
      items.value = data.items
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
  stompClient.onConnect = () => stompClient!.subscribe('/topic/schedule', onScheduleUpdate)
  stompClient.activate()

  timeInterval = setInterval(updateNow, 30000)
})

onUnmounted(() => {
  if (stompClient) stompClient.deactivate()
  if (timeInterval) clearInterval(timeInterval)
})
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
