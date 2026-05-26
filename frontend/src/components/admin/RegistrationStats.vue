<script setup lang="ts">
import type { AdminStats } from '@/views/admin/AdminDashboardView.vue'

defineProps<{
  stats: AdminStats | null
}>()
</script>

<template>
  <div v-if="stats" class="mb-8 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
    <div class="card !p-4 text-center">
      <p class="text-kpi text-primary">{{ stats.totalCrews }}</p>
      <p class="text-meta text-text-soft mt-0.5">Posádek</p>
    </div>
    <div class="card !p-4 text-center">
      <p class="text-kpi text-primary">{{ stats.totalMembers }}</p>
      <p class="text-meta text-text-soft mt-0.5">Členů</p>
    </div>
    <div class="card !p-4 text-center">
      <p class="text-kpi" :class="stats.paid === stats.totalCrews ? 'text-success' : 'text-red'">{{ stats.paid }}<span class="text-body-sm text-text-soft">/{{ stats.totalCrews }}</span></p>
      <p class="text-meta text-text-soft mt-0.5">Přihlášeno a zaplaceno</p>
    </div>
    <div class="card !p-4 text-center">
      <p class="text-kpi" :class="stats.arrived === stats.totalCrews ? 'text-success' : 'text-text-muted'">{{ stats.arrived }}</p>
      <p class="text-meta text-text-soft mt-0.5">Přijelo</p>
    </div>
    <div class="card !p-4 text-center">
      <p class="text-kpi text-text-muted">{{ stats.cars }}</p>
      <p class="text-meta text-text-soft mt-0.5">Automobilů</p>
    </div>
    <div class="card !p-4 text-center">
      <p class="text-kpi text-text-muted">{{ stats.motos }}</p>
      <p class="text-meta text-text-soft mt-0.5">Motocyklů</p>
    </div>
  </div>
</template>
