<script setup lang="ts">
withDefaults(defineProps<{ small?: boolean; text?: string; inline?: boolean }>(), {
  small: false,
  text: '',
  inline: false,
})
</script>

<template>
  <div :class="inline ? 'flex items-center gap-2' : 'flex flex-col items-center justify-center py-12 gap-3'">
    <div :class="['spinner', { 'spinner-sm': small }]"></div>
    <p v-if="text" class="text-body-sm text-text-soft">{{ text }}</p>
  </div>
</template>
