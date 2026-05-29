<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchUserIncidents, updateIncidentStatus, type UserIncidentData } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

const assignments = ref<UserIncidentData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const updating = ref<number | null>(null)

if (!isLoggedIn.value) {
  router.push('/admin/login')
}

const assigneeStatusLabels: Record<string, string> = {
  PENDING: 'Čeká na zpracování',
  IN_PROGRESS: 'Rozpracováno',
  DONE: 'Hotovo',
}

async function load() {
  loading.value = true
  error.value = null
  try {
    assignments.value = await fetchUserIncidents(authHeaders())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function setStatus(assigneeId: number, status: string) {
  updating.value = assigneeId
  try {
    await updateIncidentStatus(assigneeId, status, authHeaders())
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba změny stavu'
  } finally {
    updating.value = null
  }
}

function nextStatus(current: string): string | null {
  if (current === 'PENDING') return 'IN_PROGRESS'
  if (current === 'IN_PROGRESS') return 'DONE'
  return null
}

function statusActionLabel(current: string): string {
  if (current === 'PENDING') return 'Začít plnit'
  if (current === 'IN_PROGRESS') return 'Označit hotovo'
  return ''
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-page-title text-text">Moje úkoly</h1>
      <p class="text-body-sm text-text-soft">{{ assignments.length }} přiřazených úkolů</p>
    </div>

    <LoadingSpinner v-if="loading" />

    <div v-else-if="assignments.length === 0" class="card text-center text-text-soft py-8">
      Nemáte žádné přiřazené úkoly
    </div>

    <div v-else class="space-y-4">
      <div v-for="a in assignments" :key="a.assigneeId" class="card !p-4">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="font-semibold text-text">{{ a.title }}</span>
              <span class="inline-flex items-center rounded-full px-2 py-0.5 text-meta font-medium"
                :class="a.assigneeStatus === 'DONE' ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : a.assigneeStatus === 'IN_PROGRESS' ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400' : 'bg-surface-strong text-text-soft'"
              >{{ assigneeStatusLabels[a.assigneeStatus] || a.assigneeStatus }}</span>
            </div>
            <div v-if="a.description" class="mt-1 text-body-sm text-text-muted">{{ a.description }}</div>
            <div class="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-meta text-text-soft">
              <span>Zadal: {{ a.createdByName }}</span>
              <span v-if="a.dueDate">Termín: <span :class="new Date(a.dueDate) < new Date() && a.assigneeStatus !== 'DONE' ? 'text-error font-medium' : ''">{{ a.dueDate }}</span></span>
              <span>Dne: {{ new Date(a.createdAt).toLocaleDateString('cs') }}</span>
            </div>
          </div>

          <div class="flex gap-2 shrink-0">
            <button v-if="nextStatus(a.assigneeStatus)"
              @click="setStatus(a.assigneeId, nextStatus(a.assigneeStatus)!)"
              :disabled="updating === a.assigneeId"
              class="btn-primary btn-sm"
            >
              <LoadingSpinner v-if="updating === a.assigneeId" class="!h-4 !w-4" />
              {{ statusActionLabel(a.assigneeStatus) }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <p v-if="error" class="mt-4 text-body-sm text-error">{{ error }}</p>
  </div>
</template>
