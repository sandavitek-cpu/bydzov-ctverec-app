<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchResults, type ResultRow } from '@/api'

const route = useRoute()
const year = Number(route.params.rok) || 2026

const results = ref<ResultRow[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
let interval: ReturnType<typeof setInterval> | null = null

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

onMounted(() => {
  load()
  interval = setInterval(load, 10000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Moto', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-white">Výsledky {{ year }}</h1>
        <p class="text-sm text-slate-500">Aktualizace každých 10 s</p>
      </div>
      <div v-if="results.length > 0" class="text-xs text-slate-600">
        {{ results.length }} závodník{{ results.length !== 1 ? 'ů' : '' }}
      </div>
    </div>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="error" class="mt-8 text-red-400">{{ error }}</p>

    <div v-else-if="results.length === 0" class="mt-8 text-slate-500">
      Zatím žádné výsledky.
    </div>

    <div v-else class="mt-6 overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead>
          <tr class="border-b border-slate-800 text-slate-500">
            <th class="py-2 pr-2 font-medium w-8">#</th>
            <th class="py-2 pr-3 font-medium">Start. č.</th>
            <th class="py-2 pr-4 font-medium">Posádka</th>
            <th class="hidden sm:table-cell py-2 pr-3 font-medium">Vozidlo</th>
            <th class="py-2 pr-2 font-medium text-right">Body</th>
            <th class="hidden md:table-cell py-2 pr-2 font-medium text-right">Jízdy</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in results"
            :key="r.startNumber"
            class="border-b border-slate-800/50 transition hover:bg-slate-900/40"
          >
            <td class="py-3 pr-2 text-center">
              <span
                class="inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold"
                :class="r.rank === 1 ? 'bg-amber-500/20 text-amber-400' :
                  r.rank === 2 ? 'bg-slate-400/20 text-slate-300' :
                  r.rank === 3 ? 'bg-amber-700/20 text-amber-600' :
                  'text-slate-500'"
              >
                {{ r.rank }}
              </span>
            </td>
            <td class="py-3 pr-3 font-mono text-amber-400">{{ r.startNumber }}</td>
            <td class="py-3 pr-4">
              <div class="font-medium text-white">{{ r.teamName }}</div>
              <div class="text-xs text-slate-500 sm:hidden">{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }} · {{ r.vehiclePlate }}</div>
            </td>
            <td class="hidden sm:table-cell py-3 pr-3 text-slate-400">
              <div>{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }}</div>
              <div class="text-xs text-slate-600">{{ r.vehiclePlate }}</div>
            </td>
            <td class="py-3 pr-2 text-right font-mono text-lg font-bold text-white">{{ r.totalPoints }}</td>
            <td class="hidden md:table-cell py-3 pr-2 text-right">
              <span
                v-for="run in r.runs"
                :key="run.runNumber"
                class="ml-1 inline-block rounded bg-slate-800 px-1.5 py-0.5 text-xs text-slate-400"
              >
                {{ run.runNumber }}:{{ run.points }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
