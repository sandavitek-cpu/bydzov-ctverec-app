<script setup lang="ts">
defineProps<{
  filterVariant: string
  filterStatus: string
  filterSearch: string
  variants: { value: string; label: string }[]
  selectedIdsSize: number
  batchProcessing: boolean
}>()

const emit = defineEmits<{
  (e: 'update:filterVariant', value: string): void
  (e: 'update:filterStatus', value: string): void
  (e: 'update:filterSearch', value: string): void
  (e: 'batchAction', action: 'paid' | 'pending'): void
  (e: 'clearSelection'): void
}>()
</script>

<template>
  <div class="flex flex-wrap gap-3 mb-6">
    <select :value="filterVariant" @change="emit('update:filterVariant', ($event.target as HTMLSelectElement).value)" class="input-field !w-auto !h-[36px] text-body-sm">
      <option value="all">Všechny varianty</option>
      <option v-for="opt in variants" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
    </select>
    <select :value="filterStatus" @change="emit('update:filterStatus', ($event.target as HTMLSelectElement).value)" class="input-field !w-auto !h-[36px] text-body-sm">
      <option value="all">Všechny stavy</option>
      <option value="PAID">Přihlášen a zaplaceno</option>
      <option value="PENDING">Přihlášen, nezaplaceno</option>
    </select>
    <input :value="filterSearch" @input="emit('update:filterSearch', ($event.target as HTMLInputElement).value)" placeholder="Hledat tým, SPZ, email…" class="input-field !w-full sm:!w-auto sm:!min-w-[200px] !h-[36px] text-body-sm flex-1" />
  </div>

  <div v-if="selectedIdsSize > 0" class="mb-4 flex items-center gap-3 rounded-xl border border-primary/30 bg-primary/5 px-4 py-2">
    <span class="text-body-sm font-semibold text-text">{{ selectedIdsSize }} vybráno</span>
    <button @click="emit('batchAction', 'paid')" :disabled="batchProcessing" class="btn-secondary btn-xs">Označit zaplaceno</button>
    <button @click="emit('batchAction', 'pending')" :disabled="batchProcessing" class="btn-ghost btn-xs">Vrátit na čeká</button>
    <button @click="emit('clearSelection')" class="btn-ghost btn-xs text-text-soft">Zrušit výběr</button>
  </div>
</template>
