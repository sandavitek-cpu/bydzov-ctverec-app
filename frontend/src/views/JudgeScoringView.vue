<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { lookupRacerByStartNumber, submitScore, type RacerLookup } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders, logout } = useAuth()

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
  <div class="mx-auto max-w-md">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-white">Bodovací formulář</h1>
      <button
        @click="logout(); router.push('/admin/login')"
        class="text-sm text-slate-500 hover:text-slate-300"
      >
        Odhlásit
      </button>
    </div>
    <p class="mt-1 text-sm text-slate-400">Zadejte startovní číslo a body</p>

    <div v-if="success" class="mt-8 space-y-6">
      <div class="rounded-xl border border-emerald-800 bg-emerald-900/30 p-6 text-center">
        <p class="text-3xl">✓</p>
        <p class="mt-2 text-lg font-medium text-emerald-400">Body zapsány</p>
        <p class="text-slate-300">
          {{ racer?.teamName }} — jízda {{ runNumber }}:
          <strong class="text-white">{{ points }} b.</strong>
        </p>
      </div>
      <button
        @click="handleNext"
        class="w-full rounded-lg bg-amber-500 px-4 py-3 font-semibold text-black transition hover:bg-amber-400"
      >
        Další závodník
      </button>
    </div>

    <div v-else class="mt-6 space-y-5">
      <div>
        <label class="block text-sm font-medium text-slate-300">Startovní číslo</label>
        <div class="mt-1 flex gap-2">
          <input
            v-model.number="startNumber"
            type="number"
            min="1"
            placeholder="např. 7"
            class="flex-1 rounded-lg border border-slate-700 bg-slate-800 px-3 py-3 text-center text-2xl text-white focus:border-amber-500 focus:outline-none"
            @keyup.enter="handleLookup"
          />
          <button
            @click="handleLookup"
            :disabled="lookupLoading || !startNumber"
            class="rounded-lg bg-amber-500 px-4 font-semibold text-black transition hover:bg-amber-400 disabled:opacity-50"
          >
            {{ lookupLoading ? '…' : 'OK' }}
          </button>
        </div>
      </div>

      <div v-if="racer" class="rounded-xl border border-emerald-800 bg-emerald-900/20 p-4">
        <p class="text-sm text-emerald-400">Potvrzeno ✓</p>
        <p class="mt-1 text-xl font-bold text-white">{{ racer.teamName }}</p>
        <p class="text-sm text-slate-400">{{ racer.vehiclePlate }} · #{{ racer.startNumber }}</p>
      </div>

      <p v-else-if="searched && !racer" class="text-center text-sm text-slate-500">
        {{ lookupLoading ? 'Vyhledávám…' : error || 'Zadejte číslo a klikněte OK' }}
      </p>

      <div v-if="racer" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-slate-300">Jízda</label>
          <select
            v-model.number="runNumber"
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
          >
            <option :value="1">1. jízda</option>
            <option :value="2">2. jízda</option>
            <option :value="3">3. jízda</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-slate-300">Počet bodů</label>
          <input
            v-model.number="points"
            type="number"
            min="0"
            max="999"
            placeholder="0"
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-3 text-center text-2xl text-white focus:border-amber-500 focus:outline-none"
          />
        </div>

        <button
          @click="handleSubmit"
          :disabled="loading || points === null"
          class="w-full rounded-lg bg-amber-500 px-4 py-3 font-semibold text-black transition hover:bg-amber-400 disabled:opacity-50"
        >
          {{ loading ? 'Odesílám…' : 'Odeslat body' }}
        </button>
      </div>

      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>
    </div>
  </div>
</template>
