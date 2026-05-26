<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, authFetch, lookupRacerByStartNumber, submitScore, type RacerLookup } from '@/api'

const { authHeaders } = useAuth()

const startNumber = ref<number | null>(null)
const selectedCheckpoint = ref<number | null>(null)
const points = ref<number | null>(null)
const racer = ref<RacerLookup | null>(null)
const loading = ref(false)
const lookupLoading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)
const searched = ref(false)

interface FullCheckpoint {
  id: number; name: string; sortOrder: number
  lat?: number; lng?: number; radius?: number
  taskDescription?: string; maxPoints?: number
}

const checkpoints = ref<FullCheckpoint[]>([])
const selectedCpFull = ref<FullCheckpoint | null>(null)

onMounted(async () => {
  try {
    const res = await authFetch(`${apiBaseUrl}/api/racer/checkpoints`)
    if (res.ok) {
      checkpoints.value = await res.json()
      if (checkpoints.value.length > 0) {
        selectedCheckpoint.value = checkpoints.value[0].id
        selectedCpFull.value = checkpoints.value[0]
      }
    }
  } catch {
    // silently fail, judge will see empty dropdown
  }
})

function onCpChange() {
  selectedCpFull.value = checkpoints.value.find(c => c.id === selectedCheckpoint.value) ?? null
}

async function handleLookup() {
  if (!startNumber.value) return
  lookupLoading.value = true
  error.value = null
  racer.value = null
  searched.value = true
  try {
    racer.value = await lookupRacerByStartNumber(startNumber.value)
    if (!racer.value) {
      error.value = 'Závodník s tímto startovním číslem nenalezen'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba vyhledávání'
  } finally {
    lookupLoading.value = false
  }
}

async function handleSubmit() {
  if (!racer.value || points.value === null || points.value === undefined || !selectedCheckpoint.value) return
  loading.value = true
  error.value = null
  try {
    await submitScore({
      racerRegistrationId: racer.value.id,
      checkpointId: selectedCheckpoint.value,
      points: points.value,
    }, authHeaders())
    success.value = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba odeslání'
  } finally {
    loading.value = false
  }
}

function handleNext() {
  success.value = false
  racer.value = null
  startNumber.value = null
  points.value = null
  error.value = null
  searched.value = false
}

</script>

<template>
  <div class="max-w-form mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-page-title text-text">Bodovací formulář</h1>
        <p class="text-body text-text-muted">Komisařské stanoviště</p>
      </div>
    </div>

    <!-- Success -->
    <div v-if="success" class="space-y-6">
      <div class="card text-center py-8">
        <div class="inline-flex h-20 w-20 items-center justify-center rounded-full bg-success/10 mb-4">
          <svg class="h-10 w-10 text-success" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 class="text-subsection text-success text-xl">Body zapsány</h2>
        <p class="mt-2 text-body text-text-muted">
          {{ racer?.teamName }} —
          <strong class="text-primary text-xl">{{ points }} b.</strong>
        </p>
      </div>
      <button @click="handleNext" class="btn-primary w-full py-4 text-lg font-bold">
        Další závodník
      </button>
    </div>

    <!-- Form -->
    <div v-else class="space-y-6">
      <!-- Start number — full-screen input -->
      <div>
        <label class="input-label text-lg mb-2">Startovní číslo</label>
        <div class="flex gap-3">
          <input
            v-model.number="startNumber"
            type="number"
            min="1"
            inputmode="numeric"
            autofocus
            placeholder="např. 7"
            class="input-field flex-1 text-center text-4xl font-bold py-6"
            @keyup.enter="handleLookup"
          />
          <button @click="handleLookup" :disabled="lookupLoading || !startNumber"
            class="btn-primary px-8 text-lg font-bold min-w-[80px]"
          >
            {{ lookupLoading ? '…' : 'OK' }}
          </button>
        </div>
      </div>

      <!-- Racer confirmed -->
      <div v-if="racer" class="card !border-success/30 !bg-success/5 py-6">
        <div class="flex items-center gap-2 text-success mb-1">
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <span class="text-label text-lg">Potvrzeno</span>
        </div>
        <p class="text-subsection text-text text-xl">{{ racer.teamName }}</p>
        <p class="text-body-sm text-text-muted">{{ racer.vehiclePlate }} · #{{ racer.startNumber }}</p>
      </div>

      <p v-else-if="searched && !racer" class="text-body text-text-soft text-center py-4">
        {{ lookupLoading ? 'Vyhledávám…' : error || 'Zadejte startovní číslo' }}
      </p>

      <!-- Scoring form -->
      <div v-if="racer" class="space-y-6">
        <div>
          <label class="input-label text-lg mb-2">Stanoviště</label>
          <select v-model.number="selectedCheckpoint" @change="onCpChange" class="input-field text-lg py-4">
            <option v-for="cp in checkpoints" :key="cp.id" :value="cp.id">
              {{ cp.sortOrder }}. {{ cp.name }}
            </option>
          </select>
          <p v-if="selectedCpFull?.taskDescription" class="mt-2 text-body-sm text-text-muted italic">
            {{ selectedCpFull.taskDescription }}
            <span v-if="selectedCpFull?.maxPoints">(max. {{ selectedCpFull.maxPoints }} b.)</span>
          </p>
        </div>

        <div>
          <label class="input-label text-lg mb-2">Počet bodů
            <span v-if="selectedCpFull?.maxPoints" class="text-text-soft font-normal"> (max. {{ selectedCpFull.maxPoints }})</span>
          </label>
          <input
            v-model.number="points"
            type="number"
            min="0"
            :max="selectedCpFull?.maxPoints ?? 999"
            inputmode="numeric"
            placeholder="0"
            class="input-field text-center text-4xl font-bold py-6"
          />
        </div>

        <button @click="handleSubmit" :disabled="loading || points === null"
          class="btn-primary w-full py-5 text-lg font-bold"
        >
          {{ loading ? 'Odesílám…' : 'Odeslat body' }}
        </button>
      </div>

      <p v-if="error" class="text-body-sm text-error text-center py-4 bg-error/5 rounded-lg">{{ error }}</p>
    </div>
  </div>
</template>
