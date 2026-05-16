<template>
  <div class="animate-pulse space-y-3" :class="type === 'card' ? 'rounded-xl border border-border bg-surface p-6' : ''">
    <div v-for="i in lines" :key="i" class="h-4 rounded bg-surface-strong" :style="{ width: widths[(i - 1) % widths.length] }"></div>
    <div v-if="type === 'card'" class="mt-4 grid grid-cols-2 gap-3">
      <div v-for="i in 4" :key="'b' + i" class="h-20 rounded-lg bg-surface-strong"></div>
    </div>
    <div v-if="type === 'table'" class="mt-4 space-y-2">
      <div v-for="i in 5" :key="'t' + i" class="h-12 rounded-lg bg-surface-strong"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  lines?: number
  type?: 'text' | 'card' | 'table'
}>(), {
  lines: 3,
  type: 'text',
})

const widths = ['40%', '70%', '55%', '85%', '30%', '60%']
</script>
