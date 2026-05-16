<script setup lang="ts">
defineProps<{
  title: string
  message: string
  confirmLabel?: string
  confirmClass?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="emit('cancel')">
    <div class="mx-4 w-full max-w-sm rounded-xl border border-border bg-surface p-6 shadow-lg">
      <h3 class="text-subsection text-text mb-2">{{ title }}</h3>
      <p class="text-body-sm text-text-muted mb-6">{{ message }}</p>
      <div class="flex gap-3 justify-end">
        <button @click="emit('cancel')" class="btn-secondary btn-sm">Zrušit</button>
        <button @click="emit('confirm')" class="btn-sm" :class="confirmClass || 'btn-primary'">{{ confirmLabel || 'Smazat' }}</button>
      </div>
    </div>
  </div>
</template>
