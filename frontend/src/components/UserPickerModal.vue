<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AdminUser } from '@/api'

const props = defineProps<{
  users: AdminUser[]
  modelValue: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'close': []
}>()

const searchQuery = ref('')
const selected = ref<string[]>([...props.modelValue])

const filteredUsers = computed(() => {
  if (!searchQuery.value) return props.users
  const q = searchQuery.value.toLowerCase()
  return props.users.filter(u =>
    u.name.toLowerCase().includes(q) ||
    u.email.toLowerCase().includes(q) ||
    u.username.toLowerCase().includes(q)
  )
})

function toggleUser(name: string) {
  const idx = selected.value.indexOf(name)
  if (idx >= 0) {
    selected.value = [...selected.value.slice(0, idx), ...selected.value.slice(idx + 1)]
  } else {
    selected.value = [...selected.value, name]
  }
}

function confirm() {
  emit('update:modelValue', selected.value)
  emit('close')
}
</script>

<template>
  <div class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40" @click.self="emit('close')">
    <div class="mx-4 w-full max-w-form rounded-xl border border-border bg-surface p-6 shadow-lg">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-subsection text-text">Výběr kontrolorů</h2>
        <button @click="emit('close')" class="text-text-soft hover:text-text text-xl leading-none">&times;</button>
      </div>

      <div class="relative mb-4">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input v-model="searchQuery" placeholder="Hledat podle jména nebo emailu…" class="input-field pl-10" />
      </div>

      <div class="max-h-64 overflow-y-auto space-y-1 -mx-2 px-2">
        <label v-for="u in filteredUsers" :key="u.id"
          class="flex items-center gap-3 cursor-pointer rounded-md px-3 py-2 transition hover:bg-bg-alt"
        >
          <input type="checkbox" :checked="selected.includes(u.name)" @change="toggleUser(u.name)" class="accent-primary" />
          <div class="min-w-0">
            <span class="text-body-sm text-text">{{ u.name }}</span>
            <span class="text-meta text-text-soft ml-2">{{ u.email }}</span>
          </div>
        </label>
        <p v-if="filteredUsers.length === 0" class="text-body-sm text-text-soft text-center py-4">
          Žádní uživatelé
        </p>
      </div>

      <div class="mt-4 flex items-center justify-between pt-4 border-t border-border">
        <span class="text-meta text-text-soft">{{ selected.length }} vybráno</span>
        <div class="flex gap-3">
          <button @click="emit('close')" class="btn-secondary btn-sm">Zrušit</button>
          <button @click="confirm" class="btn-primary btn-sm">Potvrdit</button>
        </div>
      </div>
    </div>
  </div>
</template>
