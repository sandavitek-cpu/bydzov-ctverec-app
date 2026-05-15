<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const roles = ref<any[]>([])
const usersByRole = ref<Record<number, any[]>>({})
const loading = ref(true)
const error = ref<string | null>(null)

const showForm = ref(false)
const editing = ref<any | null>(null)
const formName = ref('')
const formDisplayName = ref('')

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/roles`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    if (!res.ok) { const body = await res.json().catch(() => ({ error: `HTTP ${res.status}` })); throw new Error(body.error ?? `HTTP ${res.status}`) }
    roles.value = await res.json()

    const entries = await Promise.all(roles.value.map(async (r: any) => {
      const ures = await fetch(`${apiBaseUrl}/api/admin/roles/${r.id}/users`, {
        headers: { ...authHeaders() },
      })
      const users = ures.ok ? await ures.json() : []
      return [r.id, users] as [number, any[]]
    }))
    usersByRole.value = Object.fromEntries(entries)
  } catch (e) {
    console.error('AdminRolesView load error:', e)
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editing.value = null
  formName.value = ''
  formDisplayName.value = ''
  showForm.value = true
}

function startEdit(role: any) {
  editing.value = role
  formName.value = role.name
  formDisplayName.value = role.displayName ?? ''
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editing.value = null
}

async function save() {
  error.value = null
  try {
    const h = authHeaders()
    if (editing.value) {
      const res = await fetch(`${apiBaseUrl}/api/admin/roles/${editing.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...h },
        body: JSON.stringify({ displayName: formDisplayName.value }),
      })
      if (!res.ok) { const body = await res.json(); throw new Error(body.error ?? 'Chyba') }
    } else {
      const res = await fetch(`${apiBaseUrl}/api/admin/roles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...h },
        body: JSON.stringify({ name: formName.value, displayName: formDisplayName.value }),
      })
      if (!res.ok) { const body = await res.json(); throw new Error(body.error ?? 'Chyba') }
    }
    cancelForm()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  }
}

async function remove(id: number) {
  if (!confirm('Opravdu smazat roli?')) return
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/roles/${id}`, {
      method: 'DELETE',
      headers: { ...authHeaders() },
    })
    if (!res.ok) throw new Error()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}

onMounted(() => { load() })
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Role</h1>
        <p class="text-body-sm text-text-soft">{{ roles.length }} rolí</p>
      </div>
    </div>

    <div v-if="error" class="alert alert-error mb-6">{{ error }}</div>

    <div class="mb-6">
      <button @click="startCreate" class="btn-primary btn-sm">Nová role</button>
    </div>

    <div v-if="showForm" class="card mb-6">
      <h2 class="text-subsection text-text mb-4">{{ editing ? 'Upravit roli' : 'Nová role' }}</h2>
      <form @submit.prevent="save" class="space-y-4 max-w-form">
        <div>
          <label class="input-label">Název</label>
          <input v-model="formName" :disabled="!!editing" required class="input-field" />
        </div>
        <div>
          <label class="input-label">Zobrazovaný název</label>
          <input v-model="formDisplayName" required class="input-field" />
        </div>
        <div class="flex gap-3">
          <button type="submit" class="btn-primary btn-sm">{{ editing ? 'Uložit' : 'Přidat' }}</button>
          <button type="button" @click="cancelForm" class="btn-secondary btn-sm">Zrušit</button>
        </div>
      </form>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>
    <p v-else-if="roles.length === 0" class="py-12 text-center">
      <span class="text-section-title text-text-soft">Žádné role</span>
    </p>

    <div v-else class="space-y-4">
      <div v-for="role in roles" :key="role.id" class="card">
        <div class="flex items-center justify-between">
          <div>
            <span class="font-medium text-text">{{ role.displayName }}</span>
            <span class="ml-2 text-meta text-text-soft">{{ role.name }}</span>
          </div>
          <div class="flex gap-2">
            <button @click="startEdit(role)" class="btn-ghost btn-sm !h-8 !px-2">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button @click="remove(role.id)" class="btn-ghost btn-sm !h-8 !px-2 !text-error">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
        <div class="mt-4 border-t border-border pt-4">
          <p class="text-meta text-text-soft mb-3">Uživatelé s touto rolí ({{ (usersByRole[role.id] || []).length }})</p>
          <div v-if="(usersByRole[role.id] || []).length === 0" class="text-body-sm text-text-soft">Žádní uživatelé</div>
          <div v-else class="space-y-1.5">
            <div v-for="u in usersByRole[role.id]" :key="u.id" class="flex items-center gap-3 text-body-sm text-text-muted">
              <div class="flex h-8 w-8 items-center justify-center rounded-full bg-surface-strong text-text-soft font-medium text-meta">
                {{ (u.firstName?.[0] || '').toUpperCase() }}{{ (u.lastName?.[0] || '').toUpperCase() }}
              </div>
              <span class="text-text">{{ u.lastName ? u.firstName + ' ' + u.lastName : u.firstName }}</span>
              <span class="text-text-soft">{{ u.email }}</span>
              <span class="text-text-soft">@{{ u.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
