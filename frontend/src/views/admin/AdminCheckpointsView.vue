<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, createAdminCheckpoint, updateAdminCheckpoint, deleteAdminCheckpoint, type CheckpointData } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const checkpoints = ref<CheckpointData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref<CheckpointData | null>(null)
const mapLoaded = ref(false)

const form = ref({
  name: '',
  lat: 50.2415,
  lng: 15.4900,
  radius: 300,
  sortOrder: 0,
  taskDescription: '',
  maxPoints: null as number | null,
  volunteerName: '',
})

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    checkpoints.value = await res.json()
    setTimeout(() => { mapLoaded.value = true }, 100)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
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
    volunteerName: cp.volunteerName ?? '',
  }
}

function resetForm() {
  editing.value = null
  form.value = { name: '', lat: 50.2415, lng: 15.4900, radius: 300, sortOrder: 0, taskDescription: '', maxPoints: null, volunteerName: '' }
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
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  }
}

async function remove(id: number) {
  if (!confirm('Opravdu smazat?')) return
  try {
    await deleteAdminCheckpoint(id, authHeaders())
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}

function centerMap(cp: CheckpointData) {
  form.value.lat = cp.lat
  form.value.lng = cp.lng
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-white">Kontrolní stanoviště</h1>
        <p class="text-sm text-slate-400">{{ checkpoints.length }} stanovišť</p>
      </div>
    </div>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <div>
        <div class="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
          <h2 class="mb-4 text-lg font-semibold text-white">
            {{ editing ? 'Upravit stanoviště' : 'Nové stanoviště' }}
          </h2>
          <form @submit.prevent="save" class="space-y-3">
            <div>
              <label class="block text-xs text-slate-500">Název</label>
              <input v-model="form.name" required class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-slate-500">GPS lat</label>
                <input v-model.number="form.lat" step="any" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white font-mono" />
              </div>
              <div>
                <label class="block text-xs text-slate-500">GPS lng</label>
                <input v-model.number="form.lng" step="any" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white font-mono" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-slate-500">Rádius (m)</label>
                <input v-model.number="form.radius" type="number" min="0" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
              </div>
              <div>
                <label class="block text-xs text-slate-500">Pořadí</label>
                <input v-model.number="form.sortOrder" type="number" min="0" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
              </div>
            </div>
            <div>
              <label class="block text-xs text-slate-500">Popis úkolu</label>
              <textarea v-model="form.taskDescription" rows="2" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-slate-500">Max. body</label>
                <input v-model.number="form.maxPoints" type="number" min="0" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
              </div>
              <div>
                <label class="block text-xs text-slate-500">Dobrovolník</label>
                <input v-model="form.volunteerName" class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500" />
              </div>
            </div>
            <div class="flex gap-2 pt-1">
              <button
                type="submit"
                class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500"
              >
                {{ editing ? 'Uložit' : 'Přidat' }}
              </button>
              <button
                v-if="editing"
                type="button"
                @click="resetForm"
                class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 transition hover:bg-slate-800"
              >
                Zrušit
              </button>
            </div>
          </form>
          <p v-if="error" class="mt-3 text-sm text-red-400">{{ error }}</p>
        </div>
      </div>

      <div>
        <p v-if="loading" class="text-slate-500">Načítám…</p>

        <div v-else-if="checkpoints.length === 0" class="text-slate-500">
          Žádná stanoviště.
        </div>

        <div v-else class="space-y-2">
          <div
            v-for="cp in checkpoints"
            :key="cp.id"
            class="rounded-lg border border-slate-800 bg-slate-900/40 p-3 transition hover:bg-slate-900/60"
          >
            <div class="flex items-start justify-between">
              <div>
                <div class="flex items-center gap-2">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-slate-800 text-xs text-slate-400">{{ cp.sortOrder }}</span>
                  <span class="font-medium text-white">{{ cp.name }}</span>
                </div>
                <div class="mt-1 text-xs text-slate-500">
                  {{ cp.lat.toFixed(4) }}, {{ cp.lng.toFixed(4) }} · rádius {{ cp.radius }} m
                </div>
                <div v-if="cp.taskDescription" class="mt-1 text-xs text-slate-400">
                  {{ cp.taskDescription }}
                </div>
                <div class="mt-1 flex gap-3 text-xs">
                  <span v-if="cp.maxPoints != null" class="text-amber-500">max {{ cp.maxPoints }} bodů</span>
                  <span v-if="cp.volunteerName" class="text-emerald-400">👤 {{ cp.volunteerName }}</span>
                </div>
              </div>
              <div class="flex gap-1">
                <button
                  @click="centerMap(cp)"
                  class="rounded px-2 py-1 text-xs text-slate-500 transition hover:bg-slate-800 hover:text-slate-300"
                  title="Přenastavit mapu"
                >
                  🗺
                </button>
                <button
                  @click="startEdit(cp)"
                  class="rounded px-2 py-1 text-xs text-slate-500 transition hover:bg-slate-800 hover:text-slate-300"
                >
                  ✏️
                </button>
                <button
                  @click="remove(cp.id!)"
                  class="rounded px-2 py-1 text-xs text-red-500 transition hover:bg-red-900/30"
                >
                  🗑
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6 overflow-hidden rounded-lg border border-slate-800">
      <div class="bg-slate-900/60 px-4 py-2 text-sm text-slate-500">Náhled na mapě</div>
      <div id="checkpoint-map" class="h-80 w-full bg-slate-950">
        <div v-if="!mapLoaded" class="flex h-full items-center justify-center text-sm text-slate-600">
          Načítám mapu…
        </div>
        <iframe
          v-if="mapLoaded"
          :src="`https://www.openstreetmap.org/export/embed.html?bbox=${form.lng - 0.01}%2C${form.lat - 0.01}%2C${form.lng + 0.01}%2C${form.lat + 0.01}&layer=mapnik&marker=${form.lat}%2C${form.lng}`"
          class="h-full w-full"
          title="Mapa stanovišť"
        ></iframe>
      </div>
    </div>
  </div>
</template>
