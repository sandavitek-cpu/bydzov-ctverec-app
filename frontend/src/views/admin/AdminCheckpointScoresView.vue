<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

interface CheckpointInfo {
  sortOrder: number
  name: string
}

interface ScoreEntry {
  checkpointOrder: number
  checkpointName: string
  points: number
}

interface ResultRow {
  rank: number
  startNumber: number
  teamName: string
  vehicleCategory: string
  vehiclePlate: string
  totalPoints: number
  scores: ScoreEntry[]
}

const results = ref<ResultRow[]>([])
const checkpoints = ref<CheckpointInfo[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/results/2026`, {
      headers: authHeaders(),
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    const data = await res.json()
    checkpoints.value = data.checkpoints ?? []
    results.value = data.results ?? data
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Přehled bodování</h1>
        <p class="text-body-sm text-text-soft">{{ results.length }} posádek</p>
      </div>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-12 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error mb-4">{{ error }}</p>

    <div v-else-if="results.length === 0" class="card text-center text-text-soft py-8">
      Žádné výsledky
    </div>

    <div v-else class="overflow-x-auto rounded-xl border border-border">
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th class="w-8 text-center">#</th>
            <th>Posádka</th>
            <th class="hidden sm:table-cell">Vozidlo</th>
            <th v-for="cp in checkpoints" :key="cp.sortOrder" class="text-center text-body-sm">{{ cp.name }}</th>
            <th class="text-center">Celkem</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in results" :key="r.startNumber" class="table-row">
            <td class="text-center font-mono font-bold text-primary">{{ r.startNumber }}</td>
            <td class="font-medium text-text">{{ r.teamName }}</td>
            <td class="hidden sm:table-cell text-text-muted text-body-sm">{{ r.vehiclePlate }}</td>
            <td v-for="cp in checkpoints" :key="cp.sortOrder" class="text-center">
              <span v-if="r.scores.find(s => s.checkpointOrder === cp.sortOrder)" class="font-semibold text-text">
                {{ r.scores.find(s => s.checkpointOrder === cp.sortOrder)!.points }}
              </span>
              <span v-else class="text-text-soft">—</span>
            </td>
            <td class="text-center font-bold text-primary">{{ r.totalPoints }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
