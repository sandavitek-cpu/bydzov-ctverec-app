<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchVehicles, createVehicle, updateVehicle, deleteVehicle, type VehicleData } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()
const { show: showToast } = useToast()

const vehicles = ref<VehicleData[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref<VehicleData | null>(null)
const showForm = ref(false)
const saving = ref(false)

const form = ref({
  vehicleMake: '',
  vehiclePlate: '',
  vehicleYear: new Date().getFullYear(),
  vehicleCategory: 'AUTO',
  engineDisplacement: null as number | null,
  power: null as number | null,
  maxSpeed: null as number | null,
  vehicleNotes: '',
})

if (!isLoggedIn.value) {
  router.push('/admin/login')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    vehicles.value = await fetchVehicles(authHeaders())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    vehicleMake: '',
    vehiclePlate: '',
    vehicleYear: new Date().getFullYear(),
    vehicleCategory: 'AUTO',
    engineDisplacement: null,
    power: null,
    maxSpeed: null,
    vehicleNotes: '',
  }
  editing.value = null
  showForm.value = true
}

function startEdit(v: VehicleData) {
  form.value = {
    vehicleMake: v.vehicleMake,
    vehiclePlate: v.vehiclePlate,
    vehicleYear: v.vehicleYear,
    vehicleCategory: v.vehicleCategory,
    engineDisplacement: v.engineDisplacement,
    power: v.power,
    maxSpeed: v.maxSpeed,
    vehicleNotes: v.vehicleNotes ?? '',
  }
  editing.value = v
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editing.value = null
}

async function handleSave() {
  saving.value = true
  try {
    if (editing.value) {
      await updateVehicle(editing.value.id, form.value, authHeaders())
      showToast('Vozidlo upraveno', 'success')
    } else {
      await createVehicle(form.value, authHeaders())
      showToast('Vozidlo přidáno', 'success')
    }
    showForm.value = false
    editing.value = null
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Chyba při ukládání', 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete(v: VehicleData) {
  if (!confirm(`Opravdu smazat vozidlo ${v.vehicleMake} (${v.vehiclePlate})?`)) return
  try {
    await deleteVehicle(v.id, authHeaders())
    showToast('Vozidlo smazáno', 'info')
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Chyba při mazání', 'error')
  }
}
</script>

<template>
  <div class="max-w-form mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-page-title text-text">Můj vozový park</h1>
      <button v-if="!showForm" @click="resetForm" class="btn-primary btn-sm">Přidat vozidlo</button>
    </div>

    <LoadingSpinner v-if="loading" />
    <p v-else-if="error" class="alert alert-error">{{ error }}</p>

    <!-- Empty state -->
    <div v-else-if="vehicles.length === 0 && !showForm" class="card text-center py-12">
      <p class="text-body text-text-muted">Zatím nemáte žádná vozidla.</p>
      <p class="text-body-sm text-text-soft mt-2">Přidejte své vozidlo pro rychlejší přihlašování k závodům.</p>
    </div>

    <!-- Vehicle form -->
    <div v-if="showForm" class="card !p-6 mb-6">
      <h2 class="text-subsection text-text mb-4">{{ editing ? 'Upravit vozidlo' : 'Nové vozidlo' }}</h2>
      <form @submit.prevent="handleSave" class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="input-label">Typ vozidla</label>
            <div class="flex gap-3 mt-1">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="AUTO" v-model="form.vehicleCategory" class="accent-primary" />
                <span class="text-body-sm text-text-muted">Automobil</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="MOTO" v-model="form.vehicleCategory" class="accent-primary" />
                <span class="text-body-sm text-text-muted">Motocykl</span>
              </label>
            </div>
          </div>
          <div>
            <label class="input-label">Rok výroby</label>
            <input v-model.number="form.vehicleYear" type="number" min="1900" max="1989" required class="input-field w-full" />
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="input-label">Značka a typ</label>
            <input v-model="form.vehicleMake" required class="input-field w-full" placeholder="Škoda 1000 MB" />
          </div>
          <div>
            <label class="input-label">SPZ</label>
            <input v-model="form.vehiclePlate" required class="input-field w-full" placeholder="5H1 2345" />
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label class="input-label">Objem motoru (ccm)</label>
            <input v-model.number="form.engineDisplacement" type="number" class="input-field w-full" placeholder="např. 998" />
          </div>
          <div>
            <label class="input-label">Výkon (kW)</label>
            <input v-model.number="form.power" type="number" class="input-field w-full" placeholder="např. 33" />
          </div>
          <div>
            <label class="input-label">Max. rychlost (km/h)</label>
            <input v-model.number="form.maxSpeed" type="number" class="input-field w-full" placeholder="např. 120" />
          </div>
        </div>
        <div>
          <label class="input-label">Poznámka</label>
          <textarea v-model="form.vehicleNotes" class="input-field w-full" rows="2" placeholder="Nepovinné"></textarea>
        </div>
        <div class="flex gap-2 pt-2">
          <button type="submit" :disabled="saving" class="btn-primary flex-1">
            {{ saving ? 'Ukládám...' : editing ? 'Uložit změny' : 'Přidat vozidlo' }}
          </button>
          <button type="button" @click="cancelForm" class="btn-ghost">Zrušit</button>
        </div>
      </form>
    </div>

    <!-- Vehicle cards -->
    <div v-if="vehicles.length > 0" class="space-y-3">
      <div v-for="v in vehicles" :key="v.id"
        class="card !p-5 flex items-start justify-between gap-4"
      >
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="font-semibold text-text text-lg">{{ v.vehicleMake }}</span>
            <span class="badge badge-admin">{{ v.vehicleCategory === 'MOTO' ? 'Motocykl' : 'Automobil' }}</span>
          </div>
          <div class="flex flex-wrap gap-x-6 gap-y-1 text-body-sm text-text-muted">
            <span>SPZ: <span class="text-text font-mono">{{ v.vehiclePlate }}</span></span>
            <span>Ročník: <span class="text-text">{{ v.vehicleYear }}</span></span>
            <span v-if="v.engineDisplacement">Objem: <span class="text-text">{{ v.engineDisplacement }} ccm</span></span>
            <span v-if="v.power">Výkon: <span class="text-text">{{ v.power }} kW</span></span>
            <span v-if="v.maxSpeed">Max: <span class="text-text">{{ v.maxSpeed }} km/h</span></span>
          </div>
          <p v-if="v.vehicleNotes" class="text-body-sm text-text-soft mt-1 italic">{{ v.vehicleNotes }}</p>
        </div>
        <div class="flex items-center gap-1 shrink-0">
          <button @click="startEdit(v)" class="btn-ghost btn-xs" title="Upravit">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
            </svg>
          </button>
          <button @click="handleDelete(v)" class="btn-ghost btn-xs text-red hover:text-red" title="Smazat">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
