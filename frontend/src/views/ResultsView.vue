<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

import { useLiveResults } from '@/composables/useLiveResults'

const route = useRoute()
const router = useRouter()

const year = computed(() => Number(route.params.rok) || 2026)
const availableYears = [2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018]

const loading = ref(true)
const error = ref<string | null>(null)

const { results, connected: wsConnected, start } = useLiveResults(year.value)

onMounted(async () => {
  await start()
  loading.value = false
})

function goToYear(y: number) {
  router.push(`/vysledky/${y}`)
}

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Moto', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
}
</script>

<template>
  <div>
    <div class="flex items-start sm:items-center justify-between gap-4 mb-6 flex-col sm:flex-row">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-page-title text-text">Výsledky</h1>
          <select :value="year" @change="goToYear(Number(($event.target as HTMLSelectElement).value))"
            class="h-9 rounded-lg border border-border bg-surface px-3 text-body-sm font-semibold text-text cursor-pointer focus:outline-none focus:border-primary"
          >
            <option v-for="y in availableYears" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>
        <p class="text-body-sm text-text-soft flex items-center gap-2 mt-1">
          <span :class="wsConnected ? 'text-success' : 'text-text-soft'" class="inline-block h-2 w-2 rounded-full"></span>
          {{ wsConnected ? 'Live — WebSocket' : 'Aktualizace každých 15 s' }}
        </p>
      </div>
      <div class="flex items-center gap-3">
        <span v-if="results.length > 0" class="text-meta text-text-soft">{{ results.length }} závodník{{ results.length !== 1 ? 'ů' : '' }}</span>
        <RouterLink :to="`/ceremoniál/${year}`" class="btn-secondary btn-xs no-underline">
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 3h12v4a6 6 0 11-12 0V3zm0 0H3v2a4 4 0 004 4h2m10-6h3v2a4 4 0 01-4 4h-2m-4 10v2m0-2a4 4 0 01-4-4h8a4 4 0 01-4 4z"/></svg>
          Vyhlášení
        </RouterLink>
      </div>
    </div>

    <LoadingSpinner v-if="loading" text="Načítám výsledky…" />
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <div v-else-if="results.length === 0" class="py-16 text-center">
      <svg class="mx-auto h-12 w-12 text-text-soft mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
      <p class="text-section-title text-text-soft">Zatím žádné výsledky</p>
      <p class="text-body text-text-soft mt-2">Jakmile budou první body zapsány, objeví se zde.</p>
    </div>

    <div v-else class="overflow-x-auto rounded-xl border border-border">
      <div class="p-3 border-b border-border bg-surface-2 flex items-center justify-between">
        <span class="text-meta text-text-soft">Průběžné pořadí</span>
      </div>
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th class="w-10 text-center">#</th>
            <th>Start. č.</th>
            <th>Posádka</th>
            <th class="hidden sm:table-cell">Vozidlo</th>
            <th class="text-right">Body</th>
            <th class="hidden md:table-cell text-right">Stanoviště</th>
          </tr>
        </thead>
        <TransitionGroup tag="tbody" name="result-row">
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
                :key="run.checkpointOrder"
                class="ml-1 inline-block rounded-md bg-surface-strong px-2 py-0.5 text-meta text-text-muted"
              >{{ run.checkpointName }}:{{ run.points }}</span>
            </td>
          </tr>
        </TransitionGroup>
      </table>
    </div>
  </div>
</template>

<style scoped>
.result-row-enter-active {
  transition: all 0.3s ease-out;
}
.result-row-leave-active {
  transition: all 0.2s ease-in;
}
.result-row-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}
.result-row-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
.result-row-move {
  transition: transform 0.3s ease-out;
}
</style>
