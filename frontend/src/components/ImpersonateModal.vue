<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl, impersonateUser } from '@/api'
import type { AdminUser } from '@/api'

const router = useRouter()
const { authHeaders, impersonateAs } = useAuth()

const emit = defineEmits<{
  'close': []
}>()

const users = ref<AdminUser[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const searchQuery = ref('')
const selectedUserId = ref<number | null>(null)
const impersonatingId = ref<number | null>(null)

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  const q = searchQuery.value.toLowerCase()
  return users.value.filter((u: AdminUser) =>
    u.name.toLowerCase().includes(q) ||
    u.email.toLowerCase().includes(q) ||
    u.username.toLowerCase().includes(q)
  )
})

async function doImpersonate() {
  if (selectedUserId.value == null) return
  impersonatingId.value = selectedUserId.value
  try {
    const result = await impersonateUser(selectedUserId.value, authHeaders())
    impersonateAs(result.accessToken, result.role, result.name, result.username)
    emit('close')
    router.push('/')
  } catch {
    impersonatingId.value = null
  }
}

onMounted(async () => {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users`, {
      headers: { ...authHeaders() },
    })
    if (res.ok) {
      users.value = await res.json()
    } else if (res.status === 403) {
      error.value = 'Nemáte oprávnění (403)'
    } else {
      error.value = `API vrátilo chybu ${res.status}`
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání uživatelů'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="fixed inset-0 z-[9999] flex items-center justify-center bg-black/40" @click.self="emit('close')">
    <div class="mx-4 w-full max-w-form rounded-xl border border-border bg-surface p-6 shadow-lg">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-subsection text-text">Přihlásit jako uživatel</h2>
        <button @click="emit('close')" class="text-text-soft hover:text-text text-xl leading-none">&times;</button>
      </div>

      <div class="relative mb-4">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input v-model="searchQuery" placeholder="Hledat podle jména nebo emailu…" class="input-field pl-10" />
      </div>

      <LoadingSpinner v-if="loading" :small="true" />

      <div v-else-if="error" class="text-body-sm text-error text-center py-4">{{ error }}</div>

      <div v-else class="max-h-64 overflow-y-auto space-y-1 -mx-2 px-2">
        <button
          v-for="u in filteredUsers" :key="u.id"
          @click="selectedUserId = u.id"
          class="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-left transition"
          :class="selectedUserId === u.id ? 'bg-primary/10 ring-1 ring-primary' : 'hover:bg-bg-alt'"
        >
          <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-strong text-meta font-semibold text-text-muted">
            {{ (u.firstName?.[0] || '').toUpperCase() }}{{ (u.lastName?.[0] || '').toUpperCase() }}
          </div>
          <div class="min-w-0 flex-1">
            <span class="text-body-sm font-medium" :class="selectedUserId === u.id ? 'text-primary' : 'text-text'">{{ u.name }}</span>
            <span class="text-meta text-text-soft ml-2">@{{ u.username }}</span>
            <div class="text-meta text-text-soft">{{ u.email }}</div>
          </div>
          <div v-if="u.appRoles?.length" class="flex gap-1 shrink-0">
            <span v-for="r in u.appRoles" :key="r.id"
              class="badge"
              :class="r.name === 'ADMIN' ? 'badge-admin' : r.name === 'JUDGE' ? 'badge-judge' : 'badge-racer'"
            >{{ r.displayName }}</span>
          </div>
        </button>
        <p v-if="filteredUsers.length === 0" class="text-body-sm text-text-soft text-center py-4">
          Žádní uživatelé
        </p>
      </div>

      <div class="mt-4 flex items-center justify-between pt-4 border-t border-border">
        <span v-if="selectedUserId != null" class="text-meta text-text-soft">1 vybráno</span>
        <span v-else class="text-meta text-text-soft">Vyberte uživatele</span>
        <div class="flex gap-3">
          <button @click="emit('close')" class="btn-secondary btn-sm">Zrušit</button>
          <button @click="doImpersonate" :disabled="selectedUserId == null || impersonatingId != null"
            class="btn-primary btn-sm"
          >{{ impersonatingId != null ? 'Přepínám…' : 'Přihlásit' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
