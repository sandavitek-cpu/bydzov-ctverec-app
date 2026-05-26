<script setup lang="ts">
defineProps<{
  raceStatus: { started: boolean; finished: boolean } | null
}>()

const emit = defineEmits<{
  (e: 'start'): void
  (e: 'finish'): void
  (e: 'reset'): void
}>()
</script>

<template>
  <div class="mb-6 rounded-xl border p-4" :class="{
    'border-success/30 bg-success/5': raceStatus?.started && !raceStatus?.finished,
    'border-border bg-bg-alt': !raceStatus?.started,
    'border-info/30 bg-info/5': raceStatus?.finished,
  }">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <span class="text-label">Režim závodu:</span>
        <span v-if="!raceStatus?.started" class="badge !bg-text-soft/10 !text-text-soft">Nezahájen</span>
        <span v-else-if="!raceStatus?.finished" class="badge !bg-success/10 !text-success">Probíhá</span>
        <span v-else class="badge !bg-info/10 !text-info">Ukončen</span>
      </div>
      <div class="flex gap-2 flex-wrap">
        <button v-if="!raceStatus?.started" @click="emit('start')" class="btn-primary btn-xs">Zahájit závod</button>
        <button v-if="raceStatus?.started && !raceStatus?.finished" @click="emit('finish')" class="btn-secondary btn-xs">Ukončit závod</button>
        <button v-if="raceStatus?.started" @click="emit('reset')" class="btn-ghost btn-xs">Reset</button>
      </div>
    </div>
  </div>
</template>
