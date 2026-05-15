<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { fetchArchive, type ArchiveRow } from '@/api'

const route = useRoute()
const yearFilter = ref(route.params.rok ? Number(route.params.rok) : 0)
const nameFilter = ref('')
const vehicleFilter = ref('')

const allResults = ref<ArchiveRow[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const years = computed(() => {
  const ys = new Set(allResults.value.map(r => r.editionYear))
  return Array.from(ys).sort((a, b) => b - a)
})

const filtered = computed(() => {
  return allResults.value.filter(r => {
    if (yearFilter.value && r.editionYear !== yearFilter.value) return false
    if (nameFilter.value && !r.racerName.toLowerCase().includes(nameFilter.value.toLowerCase())) return false
    if (vehicleFilter.value && (!r.vehicle || !r.vehicle.toLowerCase().includes(vehicleFilter.value.toLowerCase()))) return false
    return true
  })
})

onMounted(async () => {
  try {
    const data = await fetchArchive()
    allResults.value = data.results
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-white">Archiv výsledků</h1>
      <div class="text-xs text-slate-600">
        {{ allResults.length }} záznamů
      </div>
    </div>

    <div class="mt-6 flex flex-wrap gap-3">
      <select
        v-model="yearFilter"
        class="rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white"
      >
        <option :value="0">Všechny ročníky</option>
        <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
      </select>
      <input
        v-model="nameFilter"
        placeholder="Jméno závodníka"
        class="rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500"
      />
      <input
        v-model="vehicleFilter"
        placeholder="Vozidlo"
        class="rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500"
      />
    </div>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="error" class="mt-8 text-red-400">{{ error }}</p>

    <div v-else-if="filtered.length === 0" class="mt-8 text-slate-500">
      Žádné výsledky neodpovídají filtrům.
    </div>

    <div v-else class="mt-6 overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead>
          <tr class="border-b border-slate-800 text-slate-500">
            <th class="py-2 pr-3 font-medium">Ročník</th>
            <th class="py-2 pr-2 font-medium w-8">#</th>
            <th class="py-2 pr-4 font-medium">Jméno</th>
            <th class="py-2 pr-3 font-medium">Vozidlo</th>
            <th class="py-2 pr-2 font-medium text-right">Body</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(r, i) in filtered"
            :key="r.editionYear + '-' + r.rank + '-' + i"
            class="border-b border-slate-800/50 transition hover:bg-slate-900/40"
          >
            <td class="py-3 pr-3 font-mono text-amber-400">{{ r.editionYear }}</td>
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
            <td class="py-3 pr-4 font-medium text-white">{{ r.racerName }}</td>
            <td class="py-3 pr-3 text-slate-400">{{ r.vehicle || '—' }}</td>
            <td class="py-3 pr-2 text-right font-mono font-bold text-white">{{ r.points }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
