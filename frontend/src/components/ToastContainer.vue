<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from '@/composables/useToast'

const { toasts, remove } = useToast()

const copiedId = ref<number | null>(null)
let copyTimeout: ReturnType<typeof setTimeout> | null = null

function copyErrorCode(t: { id: number; errorCode?: string }) {
  if (!t.errorCode) return
  navigator.clipboard.writeText(t.errorCode)
  copiedId.value = t.id
  if (copyTimeout) clearTimeout(copyTimeout)
  copyTimeout = setTimeout(() => { copiedId.value = null }, 1500)
}

const iconMap: Record<string, string> = {
  success: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  error: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z',
  info: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
}
</script>

<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-sm">
    <div v-for="t in toasts" :key="t.id"
      class="relative flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg transition-all animate-slide-in overflow-hidden"
      :class="{
        'bg-success/10 border-success text-success': t.type === 'success',
        'bg-error/10 border-error text-error': t.type === 'error',
        'bg-info/10 border-info text-info': t.type === 'info',
        'cursor-pointer': !t.errorCode,
      }"
      @click="!t.errorCode && remove(t.id)"
    >
      <svg class="w-5 h-5 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="iconMap[t.type]" />
      </svg>
      <div class="flex-1 min-w-0">
        <span class="text-body-sm block">{{ t.message }}</span>
        <div v-if="t.errorCode" class="flex items-center gap-1.5 mt-1">
          <span class="text-xs font-mono opacity-60 select-all">{{ t.errorCode }}</span>
          <button @click.stop="copyErrorCode(t)"
            class="shrink-0 rounded p-0.5 transition-colors"
            :class="copiedId === t.id ? 'text-success' : 'opacity-40 hover:opacity-100'"
            :title="copiedId === t.id ? 'Zkopírováno' : 'Zkopírovat kód chyby'"
          >
            <svg v-if="copiedId === t.id" class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
            <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2" stroke-width="2" />
              <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" stroke-width="2" />
            </svg>
          </button>
        </div>
      </div>
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
