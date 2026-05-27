<script setup lang="ts">
defineProps<{
  raceStatus: { started: boolean; finished: boolean } | null
  loading?: 'start' | 'finish' | 'reset' | null
}>()

const emit = defineEmits<{
  (e: 'start'): void
  (e: 'finish'): void
  (e: 'reset'): void
}>()
</script>

<template>
  <div class="mb-6 rounded-xl border p-4 transition-colors duration-300" :class="{
    'border-success/30 bg-success/5': raceStatus?.started && !raceStatus?.finished,
    'border-border bg-bg-alt': !raceStatus?.started,
    'border-info/30 bg-info/5': raceStatus?.finished,
  }">
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <span class="text-label">Režim závodu:</span>
        <span class="inline-flex items-center gap-1.5">
          <span v-if="!raceStatus?.started" class="badge !bg-text-soft/10 !text-text-soft">Nezahájen</span>
          <span v-else-if="!raceStatus?.finished" class="badge !bg-success/10 !text-success">
            <span class="h-1.5 w-1.5 rounded-full bg-success animate-pulse mr-1"></span>
            Probíhá
          </span>
          <span v-else class="badge !bg-info/10 !text-info">Ukončen</span>
        </span>
      </div>
      <div class="flex gap-2 flex-wrap">
        <button v-if="!raceStatus?.started" @click="emit('start')" :disabled="loading === 'start'" class="btn-primary btn-xs">
          <svg v-if="loading === 'start'" class="h-3 w-3 animate-spin mr-1" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ loading === 'start' ? 'Spouštím…' : 'Zahájit závod' }}
        </button>
        <button v-if="raceStatus?.started && !raceStatus?.finished" @click="emit('finish')" :disabled="loading === 'finish'" class="btn-secondary btn-xs">
          <svg v-if="loading === 'finish'" class="h-3 w-3 animate-spin mr-1" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ loading === 'finish' ? 'Ukončuji…' : 'Ukončit závod' }}
        </button>
        <button v-if="raceStatus?.started" @click="emit('reset')" :disabled="loading === 'reset'" class="btn-ghost btn-xs">
          {{ loading === 'reset' ? 'Resetuji…' : 'Reset' }}
        </button>
      </div>
    </div>
  </div>
</template>
