<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts, remove } = useToast()
</script>

<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-sm">
    <div v-for="t in toasts" :key="t.id"
      class="flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg transition-all animate-slide-in cursor-pointer"
      :class="{
        'bg-success/10 border-success text-success': t.type === 'success',
        'bg-error/10 border-error text-error': t.type === 'error',
        'bg-info/10 border-info text-info': t.type === 'info',
      }"
      @click="remove(t.id)"
    >
      <span class="text-body-sm flex-1">{{ t.message }}</span>
      <button @click.stop="remove(t.id)" class="text-current opacity-60 hover:opacity-100 leading-none">&times;</button>
    </div>
  </div>
</template>
