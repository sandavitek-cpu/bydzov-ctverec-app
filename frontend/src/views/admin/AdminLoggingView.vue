<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchLogLevel, setLogLevel, downloadLog } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()

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
    <div class="flex items-center justify-between gap-4 mb-6">
      <h1 class="text-page-title text-text">Logování</h1>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>

    <div v-else class="grid gap-6 lg:grid-cols-2">
      <div class="card">
        <h2 class="text-subsection text-text mb-4">Úroveň logování</h2>
        <div class="flex items-center gap-4">
          <span class="text-body text-text-muted">
            Aktuální:
            <span class="badge ml-2" :class="currentLevel === 'DEBUG' ? 'badge-admin' : 'badge-racer'">
              {{ currentLevel }}
            </span>
          </span>
          <button @click="toggleLevel" :disabled="setting" class="btn-primary btn-sm">
            {{ setting ? 'Nastavuji…' : `Přepnout na ${currentLevel === 'INFO' ? 'DEBUG' : 'INFO'}` }}
          </button>
        </div>
        <p v-if="error" class="mt-3 text-body-sm text-error">{{ error }}</p>
      </div>

      <div class="card">
        <h2 class="text-subsection text-text mb-4">Stáhnout log</h2>
        <p class="text-body-sm text-text-muted mb-4">
          Stáhne záznamy z posledních 10 minut.
        </p>
        <button @click="handleDownload" :disabled="downloading" class="btn-secondary btn-sm">
          {{ downloading ? 'Stahuji…' : 'Stáhnout aktuální log' }}
        </button>
      </div>
    </div>
  </div>
</template>
