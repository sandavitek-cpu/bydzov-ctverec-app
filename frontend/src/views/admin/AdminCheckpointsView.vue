<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import CheckpointMap from '@/components/CheckpointMap.vue'
import UserPickerModal from '@/components/UserPickerModal.vue'
import { apiBaseUrl, createAdminCheckpoint, updateAdminCheckpoint, deleteAdminCheckpoint, fetchAdminUsers, fetchAdminRoutes, fetchAdminTasks, type AdminUser, type CheckpointData, type RouteData, type TaskData } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { reverseGeocode } from '@/utils/mapUtils'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const checkpoints = ref<CheckpointData[]>([])
const users = ref<AdminUser[]>([])
const routes = ref<RouteData[]>([])
const allTasks = ref<TaskData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref<CheckpointData | null>(null)
const showUserPicker = ref(false)
const addresses = ref<Record<number, string>>({})

const commissionerUsers = computed(() =>
  users.value.filter(u => {
    const roleNames = u.appRoles.map(r => r.name)
    return roleNames.includes('ADMIN') || roleNames.includes('JUDGE') || u.role === 'ADMIN' || u.role === 'JUDGE'
  })
)

const form = ref({
  name: '',
  lat: 50.2415,
  lng: 15.4900,
  radius: 300,
  sortOrder: 0,
  taskDescription: '',
  maxPoints: null as number | null,
  volunteers: [] as string[],
  taskIds: [] as number[],
})

const routeLines = computed(() =>
  routes.value.flatMap(r =>
    r.points.length > 0
      ? [{ points: r.points.map(p => ({ lat: p.lat, lng: p.lng })), color: r.variant === 'JEDNODENNI' ? '#dc2626' : '#2563eb' }]
      : []
  )
)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const h = authHeaders()
    const cpRes = await fetch(`${apiBaseUrl}/api/admin/checkpoints`, { headers: h })
    if (cpRes.status === 403) { logout(); router.push('/admin/login'); return }
    checkpoints.value = await cpRes.json()
    routes.value = await fetchAdminRoutes(h)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function loadAddresses() {
  for (let i = 0; i < checkpoints.value.length; i++) {
    const cp = checkpoints.value[i]
    if (cp.id != null) {
      if (i > 0) await new Promise(r => setTimeout(r, 1000))
      addresses.value[cp.id] = await reverseGeocode(cp.lat, cp.lng)
    }
  }
}

function startEdit(cp: CheckpointData) {
  editing.value = cp
  form.value = {
    name: cp.name,
    lat: cp.lat,
    lng: cp.lng,
    radius: cp.radius,
    sortOrder: cp.sortOrder,
    taskDescription: cp.taskDescription ?? '',
    maxPoints: cp.maxPoints,
    volunteers: cp.volunteers ?? [],
    taskIds: cp.taskIds ?? [],
  }
}

function resetForm() {
  editing.value = null
  form.value = { name: '', lat: 50.2415, lng: 15.4900, radius: 300, sortOrder: 0, taskDescription: '', maxPoints: null, volunteers: [], taskIds: [] }
}

async function save() {
  error.value = null
  try {
    const h = authHeaders()
    if (editing.value) {
      await updateAdminCheckpoint(editing.value.id!, form.value, h)
    } else {
      await createAdminCheckpoint(form.value, h)
    }
    resetForm()
    await load()
    await loadAddresses()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  }
}

async function remove(id: number) {
  if (!confirm('Opravdu smazat?')) return
  try {
    await deleteAdminCheckpoint(id, authHeaders())
    await load()
    await loadAddresses()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}

function onMapSelectCheckpoint(id: number) {
  const cp = checkpoints.value.find(c => c.id === id)
  if (cp) startEdit(cp)
}

onMounted(async () => {
  await load()
  await loadAddresses()
  try {
    users.value = await fetchAdminUsers(authHeaders())
  } catch {}
  try {
    allTasks.value = await fetchAdminTasks(authHeaders())
  } catch {}
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Kontrolní stanoviště</h1>
        <p class="text-body-sm text-text-soft">{{ checkpoints.length }} stanovišť</p>
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <!-- Form -->
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">
            {{ editing ? 'Upravit stanoviště' : 'Nové stanoviště' }}
          </h2>
          <form @submit.prevent="save" novalidate class="space-y-4">
            <div>
              <label class="input-label">Název</label>
              <input v-model="form.name" required class="input-field" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="input-label">GPS lat</label>
                <input v-model.number="form.lat" step="any" class="input-field font-mono" />
              </div>
              <div>
                <label class="input-label">GPS lng</label>
                <input v-model.number="form.lng" step="any" class="input-field font-mono" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="input-label">Rádius (m)</label>
                <input v-model.number="form.radius" type="number" min="0" class="input-field" />
              </div>
              <div>
                <label class="input-label">Pořadí</label>
                <input v-model.number="form.sortOrder" type="number" min="0" class="input-field" />
              </div>
            </div>
            <div>
              <label class="input-label">Popis úkolu</label>
              <textarea v-model="form.taskDescription" rows="2" class="input-field min-h-[80px] resize-y"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="input-label">Max. body</label>
                <input v-model.number="form.maxPoints" type="number" min="0" class="input-field" />
              </div>
              <div>
                <label class="input-label">Kontroloři</label>
                <button type="button" @click="showUserPicker = true" class="btn-secondary btn-sm mt-1">
                  {{ form.volunteers.length > 0 ? `Vybráno ${form.volunteers.length} kontrolorů` : 'Vybrat kontrolory' }}
                </button>
                <div v-if="form.volunteers.length > 0" class="mt-2 flex flex-wrap gap-1">
                  <span v-for="v in form.volunteers" :key="v"
                    class="inline-flex items-center rounded-full bg-surface-strong px-2.5 py-0.5 text-meta text-text"
                  >{{ v }}</span>
                </div>
              </div>
            </div>
            <div>
              <label class="input-label">Přiřazené úkoly</label>
              <div v-if="allTasks.length === 0" class="text-meta text-text-soft">
                Nejprve vytvořte úkoly v sekci <RouterLink to="/admin/ukoly" class="text-primary underline">Úkoly</RouterLink>
              </div>
              <div v-else class="mt-1 space-y-1 max-h-48 overflow-y-auto border border-border rounded-lg p-2">
                <label v-for="task in allTasks" :key="task.id"
                  class="flex items-center gap-2 py-1 px-2 rounded hover:bg-surface-2 cursor-pointer"
                >
                  <input type="checkbox" :value="task.id" v-model="form.taskIds"
                    class="rounded border-border text-primary focus:ring-primary" />
                  <span class="text-body-sm text-text">{{ task.name }}</span>
                  <span v-if="task.recommendedPoints != null" class="text-meta text-text-soft ml-auto">
                    {{ task.recommendedPoints }} b.
                  </span>
                </label>
              </div>
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

      <!-- List + Map -->
      <div class="space-y-4">
        <LoadingSpinner v-if="loading" />

        <div v-else-if="checkpoints.length === 0" class="card text-center text-text-soft py-8">
          Žádná stanoviště
        </div>

        <div v-else class="space-y-2 max-h-[400px] overflow-y-auto pr-1">
          <div v-for="cp in checkpoints" :key="cp.id"
            class="card !p-3 transition-all"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="flex h-6 w-6 items-center justify-center rounded-full bg-surface-strong text-meta font-semibold text-text-muted">{{ cp.sortOrder }}</span>
                  <span class="font-medium text-text">{{ cp.name }}</span>
                </div>
                <div class="mt-1 text-meta text-text-soft">
                  {{ addresses[cp.id!] || `${cp.lat.toFixed(4)}, ${cp.lng.toFixed(4)}` }} · rádius {{ cp.radius }} m
                </div>
                <div v-if="cp.taskDescription" class="mt-1 text-body-sm text-text-muted">{{ cp.taskDescription }}</div>
                <div class="mt-1 flex gap-3 text-meta">
                  <span v-if="cp.maxPoints != null" class="text-primary">max {{ cp.maxPoints }} bodů</span>
                  <span v-if="cp.volunteers?.length" class="text-text-soft">{{ cp.volunteers.join(', ') }}</span>
                </div>
                <div v-if="cp.taskIds?.length" class="mt-1 flex flex-wrap gap-1">
                  <span v-for="tid in cp.taskIds" :key="tid"
                    class="inline-flex items-center rounded-full bg-surface-strong px-2 py-0.5 text-meta text-text"
                  >{{ allTasks.find(t => t.id === tid)?.name ?? '?' }}</span>
                </div>
              </div>
              <div class="flex gap-1 shrink-0">
                <button @click="startEdit(cp)" class="btn-ghost btn-sm !h-7 !px-2">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button @click="remove(cp.id!)" class="btn-ghost btn-sm !h-7 !px-2 !text-error">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div>
          <p class="text-meta text-text-soft mb-2">Klikni do mapy pro nastavení souřadnic</p>
          <CheckpointMap
            :lat="form.lat"
            :lng="form.lng"
            :checkpoints="checkpoints"
            :route-lines="routeLines"
            @click="(lat, lng) => { form.lat = lat; form.lng = lng }"
            @select-checkpoint="onMapSelectCheckpoint"
          />
        </div>
      </div>
    </div>
    <UserPickerModal
      v-if="showUserPicker"
      :users="commissionerUsers"
      :model-value="form.volunteers"
      @update:model-value="(v: string[]) => form.volunteers = v"
      @close="showUserPicker = false"
    />
  </div>
</template>
