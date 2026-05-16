<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

interface ChangelogEntry {
  id: number
  version: string
  description: string
  createdAt: string
}

const entries = ref<ChangelogEntry[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const form = ref({ version: '', description: '' })
const submitting = ref(false)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/changelog`, { headers: authHeaders() })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    entries.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function addEntry() {
  if (!form.value.version || !form.value.description) return
  submitting.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/changelog`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ version: form.value.version, description: form.value.description }),
    })
    if (!res.ok) throw new Error('Chyba uložení')
    form.value = { version: '', description: '' }
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">ChangeLog</h1>
        <p class="text-body-sm text-text-soft">{{ entries.length }} záznamů</p>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Nový záznam</h2>
          <form @submit.prevent="addEntry" class="space-y-4">
            <div>
              <label class="input-label">Verze</label>
              <input v-model="form.version" placeholder="např. v1.2.0" required class="input-field" />
            </div>
            <div>
              <label class="input-label">Popis změn</label>
              <textarea v-model="form.description" rows="4" required class="input-field min-h-[100px] resize-y"></textarea>
            </div>
            <button type="submit" :disabled="submitting" class="btn-primary btn-sm">
              {{ submitting ? 'Ukládám…' : 'Přidat' }}
            </button>
          </form>
          <p v-if="error" class="mt-4 text-body-sm text-error">{{ error }}</p>
        </div>
      </div>

      <div>
        <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>
        <div v-else-if="entries.length === 0" class="card text-center text-text-soft py-8">
          Žádné záznamy
        </div>
        <div v-else class="space-y-3">
          <div v-for="e in entries" :key="e.id" class="card !p-4">
            <div class="flex items-center justify-between">
              <span class="font-semibold text-primary">{{ e.version }}</span>
              <span class="text-meta text-text-soft">{{ new Date(e.createdAt).toLocaleDateString('cs') }}</span>
            </div>
            <p class="mt-1 text-body-sm text-text-muted">{{ e.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
