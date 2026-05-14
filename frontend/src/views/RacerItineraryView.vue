<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders, logout } = useAuth()

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
    if (scheduleRes.status === 403 || regRes.status === 403) {
      logout()
      router.push('/admin/login')
      return
    }
    items.value = await scheduleRes.json()
    reg.value = await regRes.json()
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
  <div class="mx-auto max-w-md">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-white">Itinerář</h1>
      <button
        @click="logout(); router.push('/admin/login')"
        class="text-sm text-slate-500 hover:text-slate-300"
      >
        Odhlásit
      </button>
    </div>
    <p v-if="reg?.teamName" class="mt-1 text-sm text-slate-400">{{ reg.teamName }}</p>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="error" class="mt-8 text-red-400">{{ error }}</p>

    <div v-else class="mt-6 space-y-0">
      <div
        v-for="(item, i) in items"
        :key="i"
        class="flex gap-4 border-l-2 px-4 pb-6 pt-0"
        :class="i < (currentIndex >= 0 ? currentIndex : items.length)
          ? 'border-slate-700'
          : i === currentIndex
            ? 'border-amber-500'
            : 'border-slate-700'"
      >
        <div class="flex flex-col items-center">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold"
            :class="i < (currentIndex >= 0 ? currentIndex : items.length)
              ? 'bg-slate-800 text-slate-500'
              : i === currentIndex
                ? 'bg-amber-500/20 text-amber-400 ring-2 ring-amber-500'
                : 'bg-slate-800 text-slate-400'"
          >
            {{ item.time }}
          </div>
        </div>
        <div class="flex-1 pt-1.5">
          <p
            class="font-medium"
            :class="i === currentIndex ? 'text-amber-400' : 'text-white'"
          >
            {{ item.label }}
          </p>
          <p v-if="item.description" class="mt-0.5 text-sm text-slate-500">
            {{ item.description }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
