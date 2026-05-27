<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts, remove } = useToast()

const iconMap: Record<string, string> = {
  success: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  error: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z',
  info: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
}
</script>

<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-sm">
    <div v-for="t in toasts" :key="t.id"
      class="relative flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg transition-all animate-slide-in cursor-pointer overflow-hidden"
      :class="{
        'bg-success/10 border-success text-success': t.type === 'success',
        'bg-error/10 border-error text-error': t.type === 'error',
        'bg-info/10 border-info text-info': t.type === 'info',
      }"
      @click="remove(t.id)"
    >
      <svg class="w-5 h-5 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="iconMap[t.type]" />
      </svg>
      <span class="text-body-sm flex-1">{{ t.message }}</span>
      <button @click.stop="remove(t.id)" class="text-current opacity-40 hover:opacity-100 leading-none text-lg">&times;</button>
      <span
        class="absolute bottom-0 left-0 h-0.5 bg-current opacity-25 toast-progress"
        :style="{ animationDuration: t.duration + 'ms' }"
      ></span>
    </div>
  </div>
</template>

<style scoped>
.toast-progress {
  animation: toast-shrink linear forwards;
}
@keyframes toast-shrink {
  from { width: 100%; }
  to { width: 0%; }
}
</style>
