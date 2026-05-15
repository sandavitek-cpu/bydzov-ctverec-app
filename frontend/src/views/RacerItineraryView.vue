<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

interface ScheduleItem {
  time: string
  label: string
  description: string | null
}

const items = ref<ScheduleItem[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const reg = ref<{ teamName: string | null; startNumber: number | null } | null>(null)

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

onMounted(load)

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

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <div v-else class="relative">
      <!-- Timeline line -->
      <div class="absolute left-5 top-0 bottom-0 w-px bg-border"></div>

      <div v-for="(item, i) in items" :key="i" class="relative flex gap-5 pb-8">
        <!-- Time circle -->
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
        <!-- Content -->
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
