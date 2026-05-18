<script setup lang="ts">
withDefaults(defineProps<{ small?: boolean }>(), { small: false })
</script>

<template>
  <div class="flex items-center justify-center py-12">
    <div :class="['spinner', { 'spinner-sm': small }]"></div>
  </div>
</template>
