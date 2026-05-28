<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import SkeletonLoader from '@/components/SkeletonLoader.vue'
import { fetchJudgeOverview } from '@/api'
import type { JudgeRacer } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

const data = ref<{
  checkpoints: { id: number; name: string; sortOrder: number }[]
  racers: JudgeRacer[]
  stats: { total: number; scored: number; remaining: number }
} | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const filterMode = ref<'all' | 'scored' | 'unscored'>('all')

const filteredRacers = computed(() => {
  if (!data.value) return []
  if (filterMode.value === 'scored') return data.value.racers.filter(r => r.scored)
  if (filterMode.value === 'unscored') return data.value.racers.filter(r => !r.scored)
  return data.value.racers
})

async function load() {
  loading.value = true
  error.value = null
  try {
    data.value = await fetchJudgeOverview(authHeaders())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
  
  // Hide loader after a short delay if data loads quickly
  setTimeout(() => {
    if (loading.value) {
      loading.value = false
    }
  }, 300)
}

onMounted(() => {
  if (!isLoggedIn.value) {
    router.push('/admin/login')
    return
  }
  load()
})
</script>

<template>
<div class="max-w-4xl mx-auto">
  <div class="flex items-center justify-between mb-6">
    <h1 class="text-page-title text-text">Přehled bodování</h1>
  </div>

  <div v-if="loading" class="space-y-6">
    <SkeletonLoader type="table" :lines="5" class="mb-4" />
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-section-title text-text">Tvá stanoviště</h2>
    </div>
    <div class="flex items-center gap-6 mb-4">
      <div class="flex items-center gap-2">
        <span class="text-body-sm text-text-muted">Filtr:</span>
        <button class="rounded-lg px-3 py-1 text-body-sm transition-colors"
          :class="'bg-surface-strong text-text-muted hover:bg-border'"
        >Všechny (—)</button>
        <button class="rounded-lg px-3 py-1 text-body-sm transition-colors"
          :class="'bg-surface-strong text-text-muted hover:bg-border'"
        >Obodované (—)</button>
        <button class="rounded-lg px-3 py-1 text-body-sm transition-colors"
          :class="'bg-surface-strong text-text-muted hover:bg-border'"
        >Neobodované (—)</button>
      </div>
    </div>
  </div>

  <p v-else-if="error" class="alert alert-error">{{ error }}</p>

  <template v-else>
    <div class="card mb-6">
      <h2 class="text-section-title text-text mb-3">Tvá stanoviště</h2>
      <div v-if="!data || data.checkpoints.length === 0" class="text-body-sm text-text-soft">
        Nejsi přiřazen k žádnému stanovišti.
      </div>
      <div v-else class="flex flex-wrap gap-2">
        <span v-for="cp in data.checkpoints" :key="cp.id"
          class="rounded-full bg-primary/10 px-3 py-1 text-body-sm text-primary font-medium"
        >{{ cp.name }}</span>
      </div>
    </div>

    <div class="card mb-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-section-title text-text">Závodníci</h2>
        <RouterLink to="/komisari" class="btn-primary btn-sm no-underline">Bodovat</RouterLink>
      </div>

      <div class="flex items-center gap-6 mb-4">
        <div class="flex items-center gap-2">
          <span class="text-body-sm text-text-muted">Filtr:</span>
          <button @click="filterMode = 'all'"
            class="rounded-lg px-3 py-1 text-body-sm transition-colors"
            :class="filterMode === 'all' ? 'bg-primary text-white' : 'bg-surface-strong text-text-muted hover:bg-border'"
          >Všechny ({{ data?.stats.total ?? 0 }})</button>
          <button @click="filterMode = 'scored'"
            class="rounded-lg px-3 py-1 text-body-sm transition-colors"
            :class="filterMode === 'scored' ? 'bg-success text-white' : 'bg-surface-strong text-text-muted hover:bg-border'"
          >Obodované ({{ data?.stats.scored ?? 0 }})</button>
          <button @click="filterMode = 'unscored'"
            class="rounded-lg px-3 py-1 text-body-sm transition-colors"
            :class="filterMode === 'unscored' ? 'bg-error text-white' : 'bg-surface-strong text-text-muted hover:bg-border'"
          >Neobodované ({{ data?.stats.remaining ?? 0 }})</button>
        </div>
      </div>

      <div v-if="data?.stats.remaining > 0"
        class="mb-4 rounded-lg border border-warning/30 bg-warning/5 px-4 py-2 text-body-sm text-warning"
      >
        {{ data.stats.remaining }} posádek ještě čeká na obodování
      </div>
      <div v-else-if="data?.stats.total > 0"
        class="mb-4 rounded-lg border border-success/30 bg-success/5 px-4 py-2 text-body-sm text-success font-medium"
      >
        Všechny posádky obodovány! ({{ data.stats.total }} / {{ data.stats.total }})
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-left">
          <thead>
            <tr class="text-meta text-text-soft border-b border-border">
              <th class="py-2 pr-3">#</th>
              <th class="py-2 pr-3">Tým</th>
              <th class="py-2 pr-3">SPZ</th>
              <th class="py-2 pr-3">Kategorie</th>
              <th class="py-2 pr-3 text-right">Stav</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="racer in filteredRacers" :key="racer.id"
              class="border-b border-border/50 transition-colors hover:bg-bg-alt"
              :class="{ 'bg-success/5': racer.scored }"
            >
              <td class="py-2 pr-3 font-semibold text-text">{{ racer.startNumber }}</td>
              <td class="py-2 pr-3 text-body-sm text-text">{{ racer.teamName }}</td>
              <td class="py-2 pr-3 text-body-sm text-text-muted font-mono">{{ racer.vehiclePlate || '—' }}</td>
              <td class="py-2 pr-3 text-body-sm text-text-muted">{{ racer.vehicleCategory || '—' }}</td>
              <td class="py-2 pr-3 text-right">
                <span v-if="racer.scored" class="inline-flex items-center gap-1 text-body-sm text-success font-medium">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                  </svg>
                  Obodováno
                </span>
                <span v-else class="text-body-sm text-text-soft">Čeká</span>
              </td>
            </tr>
            <tr v-if="filteredRacers.length === 0">
              <td colspan="5" class="py-8 text-center text-body-sm text-text-soft">Žádní závodníci</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</div>
</template>
