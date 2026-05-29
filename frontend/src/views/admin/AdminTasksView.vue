<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchAdminTasks, createAdminTask, updateAdminTask, deleteAdminTask, type TaskData } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()

const tasks = ref<TaskData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref<TaskData | null>(null)
const form = ref({
  name: '',
  description: '',
  recommendedPoints: null as number | null,
  tools: '',
})

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    tasks.value = await fetchAdminTasks(authHeaders())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function startEdit(task: TaskData) {
  editing.value = task
  form.value = {
    name: task.name,
    description: task.description ?? '',
    recommendedPoints: task.recommendedPoints,
    tools: task.tools ?? '',
  }
}

function resetForm() {
  editing.value = null
  form.value = { name: '', description: '', recommendedPoints: null, tools: '' }
}

async function save() {
  error.value = null
  try {
    const h = authHeaders()
    if (editing.value) {
      await updateAdminTask(editing.value.id!, form.value, h)
    } else {
      await createAdminTask(form.value, h)
    }
    resetForm()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  }
}

async function remove(id: number) {
  if (!confirm('Opravdu smazat tento úkol?')) return
  try {
    await deleteAdminTask(id, authHeaders())
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Databáze úkolů</h1>
        <p class="text-body-sm text-text-soft">{{ tasks.length }} úkolů</p>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <!-- Form -->
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">
            {{ editing ? 'Upravit úkol' : 'Nový úkol' }}
          </h2>
          <form @submit.prevent="save" novalidate class="space-y-4">
            <div>
              <label class="input-label">Název</label>
              <input v-model="form.name" required class="input-field" />
            </div>
            <div>
              <label class="input-label">Popis</label>
              <textarea v-model="form.description" rows="3" class="input-field min-h-[80px] resize-y"></textarea>
            </div>
            <div>
              <label class="input-label">Doporučené bodování</label>
              <input v-model.number="form.recommendedPoints" type="number" min="0" class="input-field" />
            </div>
            <div>
              <label class="input-label">Pomůcky</label>
              <textarea v-model="form.tools" rows="2" class="input-field min-h-[60px] resize-y" placeholder="např. kužely, kladivo, provaz"></textarea>
              <p class="text-meta text-text-soft mt-1">Jednotlivé pomůcky oddělte čárkami</p>
            </div>
            <div class="flex flex-wrap gap-3 pt-1">
              <button type="submit" class="btn-primary btn-sm">
                {{ editing ? 'Uložit' : 'Přidat' }}
              </button>
              <button v-if="editing" type="button" @click="resetForm" class="btn-secondary btn-sm">
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

        <div v-else-if="tasks.length === 0" class="card text-center text-text-soft py-8">
          Žádné úkoly
        </div>

        <div v-else class="space-y-2 max-h-[600px] overflow-y-auto pr-1">
          <div v-for="task in tasks" :key="task.id"
            class="card !p-3 transition-all"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-medium text-text">{{ task.name }}</span>
                </div>
                <div v-if="task.description" class="mt-1 text-body-sm text-text-muted">{{ task.description }}</div>
                <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-meta">
                  <span v-if="task.recommendedPoints != null" class="text-primary">
                    {{ task.recommendedPoints }} bodů
                  </span>
                  <span v-if="task.tools" class="text-text-soft">
                    Pomůcky: {{ task.tools }}
                  </span>
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button @click="startEdit(task)" class="btn-ghost btn-sm !h-7 !px-2">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button @click="remove(task.id!)" class="btn-ghost btn-sm !h-7 !px-2 !text-error">
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
