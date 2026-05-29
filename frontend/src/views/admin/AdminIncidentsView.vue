<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchAdminIncidents, createAdminIncident, deleteAdminIncident, fetchAdminUsers, type IncidentData, type AdminUser } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()

const incidents = ref<IncidentData[]>([])
const users = ref<AdminUser[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const form = ref({
  title: '',
  description: '',
  dueDate: '',
  assigneeIds: [] as number[],
})

const searchQuery = ref('')

if (!isAdmin.value) {
  router.push('/admin/login')
}

const filteredUsers = computed(() => {
  if (!searchQuery.value) return users.value
  const q = searchQuery.value.toLowerCase()
  return users.value.filter(u =>
    u.name.toLowerCase().includes(q) ||
    u.email.toLowerCase().includes(q)
  )
})

function toggleAssignee(userId: number) {
  const idx = form.value.assigneeIds.indexOf(userId)
  if (idx >= 0) {
    form.value.assigneeIds = form.value.assigneeIds.filter(id => id !== userId)
  } else {
    form.value.assigneeIds = [...form.value.assigneeIds, userId]
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const h = authHeaders()
    incidents.value = await fetchAdminIncidents(h)
    users.value = await fetchAdminUsers(h)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = { title: '', description: '', dueDate: '', assigneeIds: [] }
  searchQuery.value = ''
}

async function save() {
  error.value = null
  if (!form.value.title.trim()) {
    error.value = 'Název je povinný'
    return
  }
  if (form.value.assigneeIds.length === 0) {
    error.value = 'Vyberte alespoň jednoho řešitele'
    return
  }
  try {
    await createAdminIncident({
      title: form.value.title,
      description: form.value.description || undefined,
      dueDate: form.value.dueDate || undefined,
      assigneeIds: form.value.assigneeIds,
    }, authHeaders())
    resetForm()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  }
}

async function remove(id: number) {
  if (!confirm('Opravdu smazat tento úkol?')) return
  try {
    await deleteAdminIncident(id, authHeaders())
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}

const statusLabels: Record<string, string> = {
  OPEN: 'Otevřeno',
  IN_PROGRESS: 'Rozpracováno',
  DONE: 'Hotovo',
}

const assigneeStatusLabels: Record<string, string> = {
  PENDING: 'Čeká',
  IN_PROGRESS: 'Rozpracováno',
  DONE: 'Hotovo',
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Úkoly pro pořadatele</h1>
        <p class="text-body-sm text-text-soft">{{ incidents.length }} úkolů</p>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <!-- Form -->
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Nový úkol</h2>
          <form @submit.prevent="save" novalidate class="space-y-4">
            <div>
              <label class="input-label">Název</label>
              <input v-model="form.title" required class="input-field" placeholder="např. Vyzvednout sponzorské dary" />
            </div>
            <div>
              <label class="input-label">Popis</label>
              <textarea v-model="form.description" rows="3" class="input-field min-h-[80px] resize-y" placeholder="Podrobnosti k úkolu…"></textarea>
            </div>
            <div>
              <label class="input-label">Termín splnění</label>
              <input v-model="form.dueDate" type="date" class="input-field" />
            </div>
            <div>
              <label class="input-label">Řešitelé</label>
              <div class="relative mb-2">
                <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input v-model="searchQuery" placeholder="Hledat uživatele…" class="input-field pl-10" />
              </div>
              <div class="max-h-48 overflow-y-auto space-y-1 rounded-lg border border-border p-2">
                <label v-for="u in filteredUsers" :key="u.id"
                  class="flex items-center gap-3 cursor-pointer rounded-md px-3 py-2 transition hover:bg-bg-alt"
                >
                  <input type="checkbox" :checked="form.assigneeIds.includes(u.id)" @change="toggleAssignee(u.id)" class="accent-primary" />
                  <div class="min-w-0">
                    <span class="text-body-sm text-text">{{ u.name }}</span>
                    <span class="text-meta text-text-soft ml-2">{{ u.email }}</span>
                  </div>
                </label>
                <p v-if="filteredUsers.length === 0" class="text-body-sm text-text-soft text-center py-4">
                  Žádní uživatelé
                </p>
              </div>
              <p class="text-meta text-text-soft mt-1">{{ form.assigneeIds.length }} vybráno</p>
            </div>
            <div class="flex flex-wrap gap-3 pt-1">
              <button type="submit" class="btn-primary btn-sm">Vytvořit úkol</button>
              <button v-if="form.title || form.description || form.assigneeIds.length" type="button" @click="resetForm" class="btn-secondary btn-sm">
                Zrušit
              </button>
            </div>
          </form>
          <p v-if="error" class="mt-4 text-body-sm text-error">{{ error }}</p>
        </div>
      </div>

      <!-- List -->
      <div class="space-y-4">
        <LoadingSpinner v-if="loading" />

        <div v-else-if="incidents.length === 0" class="card text-center text-text-soft py-8">
          Žádné úkoly
        </div>

        <div v-else class="space-y-3 max-h-[700px] overflow-y-auto pr-1">
          <div v-for="incident in incidents" :key="incident.id"
            class="card !p-4 transition-all"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-medium text-text">{{ incident.title }}</span>
                  <span class="inline-flex items-center rounded-full px-2 py-0.5 text-meta font-medium"
                    :class="incident.status === 'DONE' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : incident.status === 'IN_PROGRESS' ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400' : 'bg-surface-strong text-text-soft'"
                  >{{ statusLabels[incident.status] || incident.status }}</span>
                </div>
                <div v-if="incident.description" class="mt-1 text-body-sm text-text-muted">{{ incident.description }}</div>
                <div class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-meta text-text-soft">
                  <span>Vytvořil: {{ incident.createdByName }}</span>
                  <span v-if="incident.dueDate">Termín: {{ incident.dueDate }}</span>
                  <span>{{ new Date(incident.createdAt).toLocaleDateString('cs') }}</span>
                </div>
                <div class="mt-2 pt-2 border-t border-border/50">
                  <p class="text-meta font-medium text-text-soft mb-1">Řešitelé:</p>
                  <div v-for="a in incident.assignees" :key="a.id" class="flex items-center justify-between text-body-sm py-0.5">
                    <span class="text-text">{{ a.userName }}</span>
                    <span class="inline-flex items-center rounded-full px-2 py-0.5 text-meta"
                      :class="a.status === 'DONE' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : a.status === 'IN_PROGRESS' ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400' : 'bg-surface-strong text-text-soft'"
                    >{{ assigneeStatusLabels[a.status] || a.status }}</span>
                  </div>
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button @click="remove(incident.id)" class="btn-ghost btn-sm !h-7 !px-2 !text-error" title="Smazat">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
