<script setup lang="ts">
import type { AdminStats } from '@/views/admin/AdminDashboardView.vue'

defineProps<{
  stats: AdminStats | null
}>()
</script>

<template>
  <div v-if="stats" class="mb-8 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
    <div class="card !p-4 text-center relative overflow-hidden">
      <div class="absolute top-0 right-0 w-16 h-16 opacity-[0.06] pointer-events-none">
        <svg class="w-full h-full text-primary" fill="currentColor" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87m-4-12a4 4 0 010 7.75"/></svg>
      </div>
      <p class="text-kpi text-primary relative">{{ stats.totalCrews }}</p>
      <p class="text-meta text-text-soft mt-0.5">Posádek</p>
    </div>
    <div class="card !p-4 text-center relative overflow-hidden">
      <div class="absolute top-0 right-0 w-16 h-16 opacity-[0.06] pointer-events-none">
        <svg class="w-full h-full text-primary" fill="currentColor" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87m-4-12a4 4 0 010 7.75"/></svg>
      </div>
      <p class="text-kpi text-primary relative">{{ stats.totalMembers }}</p>
      <p class="text-meta text-text-soft mt-0.5">Členů</p>
    </div>
    <div class="card !p-4 text-center relative overflow-hidden" :class="stats.paid === stats.totalCrews ? 'border-l-success' : 'border-l-red'">
      <p class="text-kpi relative" :class="stats.paid === stats.totalCrews ? 'text-success' : 'text-red'">{{ stats.paid }}<span class="text-body-sm text-text-soft">/{{ stats.totalCrews }}</span></p>
      <p class="text-meta text-text-soft mt-0.5">Přihlášeno a zaplaceno</p>
    </div>
    <div class="card !p-4 text-center relative overflow-hidden" :class="stats.arrived === stats.totalCrews ? 'border-l-success' : ''">
      <p class="text-kpi relative" :class="stats.arrived === stats.totalCrews ? 'text-success' : 'text-text-muted'">{{ stats.arrived }}</p>
      <p class="text-meta text-text-soft mt-0.5">Přijelo</p>
    </div>
    <div class="card !p-4 text-center relative overflow-hidden">
      <p class="text-kpi text-text-muted relative">{{ stats.cars }}</p>
      <p class="text-meta text-text-soft mt-0.5">Automobilů</p>
    </div>
    <div class="card !p-4 text-center relative overflow-hidden">
      <p class="text-kpi text-text-muted relative">{{ stats.motos }}</p>
      <p class="text-meta text-text-soft mt-0.5">Motocyklů</p>
    </div>
  </div>
</template>
