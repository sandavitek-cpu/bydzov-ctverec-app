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
    <div class="flex items-center justify-between gap-4">
      <h1 class="text-2xl font-bold text-white">Role</h1>
    </div>

    <div v-if="error" class="mt-4 rounded-lg border border-red-800 bg-red-900/30 p-3 text-sm text-red-400">{{ error }}</div>

    <div class="mt-6 flex gap-2">
      <button @click="startCreate" class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500">Nová role</button>
    </div>

    <div v-if="showForm" class="mt-4 rounded-lg border border-slate-800 bg-slate-900/60 p-4">
      <h2 class="mb-4 text-lg font-semibold text-white">{{ editing ? 'Upravit roli' : 'Nová role' }}</h2>
      <form @submit.prevent="save" class="space-y-3">
        <div>
          <label class="block text-xs text-slate-500">Název</label>
          <input v-model="formName" :disabled="!!editing" required class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white disabled:opacity-50" />
        </div>
        <div>
          <label class="block text-xs text-slate-500">Zobrazovaný název</label>
          <input v-model="formDisplayName" required class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
        </div>
        <div class="flex gap-2">
          <button type="submit" class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500">{{ editing ? 'Uložit' : 'Přidat' }}</button>
          <button type="button" @click="cancelForm" class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 transition hover:bg-slate-800">Zrušit</button>
        </div>
      </form>
    </div>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="roles.length === 0" class="mt-8 text-slate-500">Žádné role.</p>

    <div v-else class="mt-6 space-y-4">
      <div v-for="role in roles" :key="role.id" class="rounded-lg border border-slate-800 bg-slate-900/40 p-4">
        <div class="flex items-center justify-between">
          <div>
            <span class="font-medium text-white">{{ role.displayName }}</span>
            <span class="ml-2 text-xs text-slate-500">{{ role.name }}</span>
          </div>
          <div class="flex gap-2">
            <button @click="startEdit(role)" class="rounded px-2 py-1 text-xs text-slate-500 transition hover:bg-slate-800 hover:text-slate-300">✏️</button>
            <button @click="remove(role.id)" class="rounded px-2 py-1 text-xs text-red-500 transition hover:bg-red-900/30">🗑</button>
          </div>
        </div>
        <div class="mt-3 border-t border-slate-800 pt-3">
          <p class="mb-2 text-xs font-medium text-slate-500">Uživatelé s touto rolí ({{ (usersByRole[role.id] || []).length }})</p>
          <div v-if="(usersByRole[role.id] || []).length === 0" class="text-xs text-slate-600">Žádní uživatelé</div>
          <div v-else class="space-y-1">
            <div v-for="u in usersByRole[role.id]" :key="u.id" class="flex items-center gap-2 text-xs text-slate-400">
              <span class="text-slate-300">{{ u.name }}</span>
              <span class="text-slate-600">{{ u.email }}</span>
              <span class="text-slate-600">(@{{ u.username }})</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
