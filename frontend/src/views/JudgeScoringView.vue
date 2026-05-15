<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { lookupRacerByStartNumber, submitScore, type RacerLookup } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

const startNumber = ref<number | null>(null)
const runNumber = ref(1)
const points = ref<number | null>(null)
const racer = ref<RacerLookup | null>(null)
const loading = ref(false)
const lookupLoading = ref(false)
const error = ref<string | null>(null)
const success = ref(false)
const searched = ref(false)

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
  if (!racer.value || points.value === null || points.value === undefined) return
  loading.value = true
  error.value = null
  try {
    await submitScore({
      racerRegistrationId: racer.value.id,
      runNumber: runNumber.value,
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

if (!isLoggedIn.value) {
  router.push('/admin/login')
}
</script>

<template>
  <div class="max-w-form mx-auto">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-page-title text-text">Bodovací formulář</h1>
        <p class="text-body text-text-muted">Zadejte startovní číslo a body</p>
      </div>
    </div>

    <!-- Success -->
    <div v-if="success" class="space-y-6">
      <div class="card text-center">
        <div class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-success/10 mb-4">
          <svg class="h-8 w-8 text-success" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 class="text-subsection text-success">Body zapsány</h2>
        <p class="mt-2 text-body text-text-muted">
          {{ racer?.teamName }} — jízda {{ runNumber }}:
          <strong class="text-primary">{{ points }} b.</strong>
        </p>
      </div>
      <button @click="handleNext" class="btn-primary w-full">
        Další závodník
      </button>
    </div>

    <!-- Form -->
    <div v-else class="space-y-6">
      <div>
        <label class="input-label">Startovní číslo</label>
        <div class="flex gap-3">
          <input
            v-model.number="startNumber"
            type="number"
            min="1"
            placeholder="např. 7"
            class="input-field flex-1 text-center text-2xl font-bold"
            @keyup.enter="handleLookup"
          />
          <button @click="handleLookup" :disabled="lookupLoading || !startNumber" class="btn-primary">
            {{ lookupLoading ? '…' : 'OK' }}
          </button>
        </div>
      </div>

      <div v-if="racer" class="card !border-success/30 !bg-success/5">
        <div class="flex items-center gap-2 text-success mb-1">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <span class="text-label">Potvrzeno</span>
        </div>
        <p class="text-subsection text-text">{{ racer.teamName }}</p>
        <p class="text-body-sm text-text-muted">{{ racer.vehiclePlate }} · #{{ racer.startNumber }}</p>
      </div>

      <p v-else-if="searched && !racer" class="text-body text-text-soft text-center">
        {{ lookupLoading ? 'Vyhledávám…' : error || 'Zadejte číslo a klikněte OK' }}
      </p>

      <div v-if="racer" class="space-y-5">
        <div>
          <label class="input-label">Jízda</label>
          <select v-model.number="runNumber" class="input-field">
            <option :value="1">1. jízda</option>
            <option :value="2">2. jízda</option>
            <option :value="3">3. jízda</option>
          </select>
        </div>

        <div>
          <label class="input-label">Počet bodů</label>
          <input
            v-model.number="points"
            type="number"
            min="0"
            max="999"
            placeholder="0"
            class="input-field text-center text-2xl font-bold"
          />
        </div>

        <button @click="handleSubmit" :disabled="loading || points === null" class="btn-primary w-full">
          {{ loading ? 'Odesílám…' : 'Odeslat body' }}
        </button>
      </div>

      <p v-if="error" class="text-body-sm text-error">{{ error }}</p>
    </div>
  </div>
</template>
