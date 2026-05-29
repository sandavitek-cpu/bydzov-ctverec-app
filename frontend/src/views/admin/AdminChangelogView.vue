<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl } from '@/api'

interface ChangelogEntry {
  id: number
  version: string
  description: string
  createdAt: string
}

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const entries = ref<ChangelogEntry[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const showForm = ref(false)
const editing = ref<ChangelogEntry | null>(null)
const formVersion = ref('')
const formDescription = ref('')

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/changelog`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    if (!res.ok) { const body = await res.json().catch(() => ({ error: `HTTP ${res.status}` })); throw new Error(body.error ?? `HTTP ${res.status}`) }
    entries.value = await res.json()
  } catch (e) {
    console.error('AdminChangelogView load error:', e)
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editing.value = null
  formVersion.value = ''
  formDescription.value = ''
  showForm.value = true
}

function startEdit(entry: ChangelogEntry) {
  editing.value = entry
  formVersion.value = entry.version
  formDescription.value = entry.description
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editing.value = null
}

async function save() {
  error.value = null
  try {
    const h = { 'Content-Type': 'application/json', ...authHeaders() }
    if (editing.value) {
      const res = await fetch(`${apiBaseUrl}/api/admin/changelog/${editing.value.id}`, {
        method: 'PUT',
        headers: h,
        body: JSON.stringify({ version: formVersion.value, description: formDescription.value }),
      })
      if (!res.ok) { const body = await res.json(); throw new Error(body.error ?? 'Chyba') }
    } else {
      const res = await fetch(`${apiBaseUrl}/api/admin/changelog`, {
        method: 'POST',
        headers: h,
        body: JSON.stringify({ version: formVersion.value, description: formDescription.value }),
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
  if (!confirm('Opravdu smazat záznam?')) return
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/changelog/${id}`, {
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
        <h1 class="text-page-title text-text">ChangeLog</h1>
        <p class="text-body-sm text-text-soft">{{ entries.length }} záznamů</p>
      </div>
    </div>

    <div v-if="error" class="alert alert-error mb-6">{{ error }}</div>

    <div class="mb-6">
      <button @click="startCreate" class="btn-primary btn-sm">Nový záznam</button>
    </div>

    <div v-if="showForm" class="card mb-6">
      <h2 class="text-subsection text-text mb-4">{{ editing ? 'Upravit záznam' : 'Nový záznam' }}</h2>
      <form @submit.prevent="save" class="space-y-4 max-w-form">
        <div>
          <label class="input-label">Verze</label>
          <input v-model="formVersion" required class="input-field" placeholder="např. 0.5.0" />
        </div>
        <div>
          <label class="input-label">Popis</label>
          <textarea v-model="formDescription" required class="input-field" rows="3" placeholder="Co se změnilo…"></textarea>
        </div>
        <div class="flex gap-3">
          <button type="submit" class="btn-primary btn-sm">{{ editing ? 'Uložit' : 'Přidat' }}</button>
          <button type="button" @click="cancelForm" class="btn-secondary btn-sm">Zrušit</button>
        </div>
      </form>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="entries.length === 0" class="py-12 text-center">
      <span class="text-section-title text-text-soft">Žádné záznamy</span>
    </p>

    <div v-else class="space-y-4">
      <div v-for="entry in entries" :key="entry.id" class="card">
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <div class="flex items-center gap-3">
              <span class="text-label text-primary font-mono whitespace-nowrap">{{ entry.version }}</span>
              <span class="text-meta text-text-soft">{{ new Date(entry.createdAt).toLocaleDateString('cs') }}</span>
            </div>
            <p class="mt-1 text-body-sm text-text-muted">{{ entry.description }}</p>
          </div>
          <div class="flex gap-2 shrink-0">
            <button @click="startEdit(entry)" class="btn-ghost btn-sm !h-8 !px-2">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button @click="remove(entry.id)" class="btn-ghost btn-sm !h-8 !px-2 !text-error">
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
