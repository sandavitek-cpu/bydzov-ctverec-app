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
    <h1 class="text-page-title text-text">Archiv výsledků</h1>
    <p class="mt-2 text-body-lg text-text-muted">Prohlédněte si historii všech ročníků</p>

    <div class="mt-6 flex flex-wrap gap-3 items-center">
      <select v-model="yearFilter" class="input-field w-auto">
        <option :value="0">Všechny ročníky</option>
        <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
      </select>
      <input v-model="nameFilter" placeholder="Jméno závodníka" class="input-field w-auto min-w-[200px]" />
      <input v-model="vehicleFilter" placeholder="Vozidlo" class="input-field w-auto min-w-[180px]" />
      <div class="text-meta text-text-soft">
        {{ allResults.length }} záznamů
      </div>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-12 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error mt-6">{{ error }}</p>

    <div v-else-if="filtered.length === 0" class="py-12 text-center">
      <p class="text-section-title text-text-soft">Žádné výsledky</p>
      <p class="text-body text-text-soft mt-2">Zkuste změnit filtry.</p>
    </div>

    <div v-else class="mt-6 overflow-x-auto rounded-xl border border-border">
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th>Ročník</th>
            <th class="w-10 text-center">#</th>
            <th>Jméno</th>
            <th>Vozidlo</th>
            <th class="text-right">Body</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in filtered" :key="r.editionYear + '-' + r.rank + '-' + i" class="table-row">
            <td class="font-display text-2xl tracking-[0.04em] text-red">{{ r.editionYear }}</td>
            <td class="text-center">
              <span
                class="inline-flex h-7 w-7 items-center justify-center rounded-full text-body-sm font-bold"
                :class="r.rank === 1 ? 'rank-1' : r.rank === 2 ? 'rank-2' : r.rank === 3 ? 'rank-3' : 'text-text-soft'"
              >{{ r.rank }}</span>
            </td>
            <td class="font-medium text-text">{{ r.racerName }}</td>
            <td class="text-text-muted">{{ r.vehicle || '—' }}</td>
            <td class="text-right font-mono font-bold text-text">{{ r.points }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
