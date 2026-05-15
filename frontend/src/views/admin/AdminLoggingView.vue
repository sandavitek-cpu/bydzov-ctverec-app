<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchLogLevel, setLogLevel, downloadLog } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const currentLevel = ref('INFO')
const loading = ref(true)
const error = ref<string | null>(null)
const setting = ref(false)
const downloading = ref(false)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function loadLevel() {
  try {
    const r = await fetchLogLevel(authHeaders())
    currentLevel.value = r.level
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function toggleLevel() {
  setting.value = true
  error.value = null
  const newLevel = currentLevel.value === 'INFO' ? 'DEBUG' : 'INFO'
  try {
    const r = await setLogLevel(newLevel, authHeaders())
    currentLevel.value = r.level
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba změny'
  } finally {
    setting.value = false
  }
}

async function handleDownload() {
  downloading.value = true
  error.value = null
  try {
    const blob = await downloadLog(authHeaders())
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'application.log'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba stažení'
  } finally {
    downloading.value = false
  }
}

onMounted(loadLevel)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4">
      <h1 class="text-2xl font-bold text-white">Logování</h1>
      <div class="flex gap-2">
        <RouterLink
          to="/admin/prihlaseni"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Přihlášky
        </RouterLink>
        <RouterLink
          to="/admin/stanoviste"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Stanoviště
        </RouterLink>
        <RouterLink
          to="/admin/komunikace"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Komunikace
        </RouterLink>
        <button
          @click="logout(); router.push('/admin/login')"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-500 transition hover:bg-slate-800"
        >
          Odhlásit
        </button>
      </div>
    </div>

    <div v-if="loading" class="mt-8 text-slate-500">Načítám…</div>

    <div v-else class="mt-6 grid gap-6 lg:grid-cols-2">
      <div class="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="mb-4 text-lg font-semibold text-white">Úroveň logování</h2>
        <div class="flex items-center gap-4">
          <span class="text-sm text-slate-400">Aktuální: 
            <span
              class="ml-1 rounded-full px-2.5 py-0.5 text-xs font-medium"
              :class="currentLevel === 'DEBUG' ? 'bg-amber-900/30 text-amber-400' : 'bg-emerald-900/30 text-emerald-400'"
            >
              {{ currentLevel }}
            </span>
          </span>
          <button
            @click="toggleLevel"
            :disabled="setting"
            class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50"
          >
            {{ setting ? 'Nastavuji…' : `Přepnout na ${currentLevel === 'INFO' ? 'DEBUG' : 'INFO'}` }}
          </button>
        </div>
        <p v-if="error" class="mt-3 text-sm text-red-400">{{ error }}</p>
      </div>

      <div class="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
        <h2 class="mb-4 text-lg font-semibold text-white">Stáhnout log</h2>
        <p class="mb-4 text-sm text-slate-400">
          Stáhne záznamy z posledních 10 minut.
        </p>
        <button
          @click="handleDownload"
          :disabled="downloading"
          class="rounded-lg bg-slate-700 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-600 disabled:opacity-50"
        >
          {{ downloading ? 'Stahuji…' : 'Stáhnout aktuální log' }}
        </button>
      </div>
    </div>
  </div>
</template>
