<script setup lang="ts">
import { ref, computed } from 'vue'
import { submitRegistration, type RegistrationResult } from '@/api'

const CATEGORIES = [
  { value: 'MOTOCYKL', label: 'Motocykl', fee: 300 },
  { value: 'OSOBNI', label: 'Osobní automobil', fee: 500 },
  { value: 'CLASSIC', label: 'Historické vozidlo', fee: 400 },
  { value: 'NAKLADNI', label: 'Nákladní automobil', fee: 800 },
]

const form = ref({
  teamName: '',
  email: '',
  phone: '',
  vehicleCategory: '',
  vehiclePlate: '',
  vehicleYear: new Date().getFullYear(),
  crewCount: 1,
})

const submitted = ref(false)
const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<RegistrationResult | null>(null)

const perPersonFee = computed(() => {
  const cat = CATEGORIES.find(c => c.value === form.value.vehicleCategory)
  return cat?.fee ?? 0
})

const totalFee = computed(() => perPersonFee.value * form.value.crewCount)

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    result.value = await submitRegistration({
      teamName: form.value.teamName,
      email: form.value.email,
      phone: form.value.phone,
      vehicleCategory: form.value.vehicleCategory,
      vehiclePlate: form.value.vehiclePlate,
      vehicleYear: form.value.vehicleYear,
      crewCount: form.value.crewCount,
    })
    submitted.value = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba při odesílání'
  } finally {
    loading.value = false
  }
}

const qrUrl = computed(() => {
  if (!result.value) return ''
  const data = `bydzov-ctverec:${result.value.startNumber}:${result.value.id}`
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(data)}`
})
</script>

<template>
  <div>
    <h1 class="text-page-title text-text">Přihláška do soutěže</h1>
    <p class="mt-2 text-body-lg text-text-muted">Vyplňte údaje o posádce a vozidle</p>

    <!-- Success -->
    <div v-if="submitted && result" class="mt-8 space-y-6 max-w-form">
      <div class="alert alert-success">
        <p class="font-semibold">Přihláška přijata ✓</p>
        <p class="mt-1">Děkujeme za přihlášení. Vaše startovní číslo:</p>
        <p class="mt-2 text-kpi text-primary">#{{ result.startNumber }}</p>
      </div>

      <div class="card">
        <h2 class="text-subsection text-text">Platební údaje</h2>
        <div class="mt-4 space-y-2">
          <p class="text-body text-text-muted">Částka: <strong class="text-text">{{ result.startFee }} Kč</strong></p>
          <p class="text-body text-text-muted">Bankovní účet: <span class="font-mono font-semibold text-text">2802609342/2010</span></p>
          <p class="text-body text-text-muted">Variabilní symbol: <span class="font-mono font-semibold text-text">{{ result.startNumber }}</span></p>
          <p class="text-meta text-text-soft mt-2">Do zprávy pro příjemce uveďte název posádky.</p>
        </div>
      </div>

      <div class="card text-center">
        <h2 class="text-subsection text-text mb-4">QR kód pro prezenci</h2>
        <img :src="qrUrl" alt="QR kód" class="mx-auto rounded-md" width="200" height="200" />
        <p class="mt-3 text-meta text-text-soft">Předložte při prezenci v den závodu</p>
      </div>
    </div>

    <!-- Form -->
    <form v-else @submit.prevent="handleSubmit" class="mt-6 space-y-5 max-w-form">
      <div>
        <label class="input-label">Název posádky</label>
        <input v-model="form.teamName" required class="input-field" placeholder="např. Tým Pardubice" />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="input-label">E-mail</label>
          <input v-model="form.email" type="email" required class="input-field" placeholder="posadka@example.cz" />
        </div>
        <div>
          <label class="input-label">Telefon</label>
          <input v-model="form.phone" type="tel" required class="input-field" placeholder="+420 777 123 456" />
        </div>
      </div>

      <div>
        <label class="input-label">Kategorie vozidla</label>
        <select v-model="form.vehicleCategory" required class="input-field">
          <option value="" disabled>Vyberte kategorii</option>
          <option v-for="cat in CATEGORIES" :key="cat.value" :value="cat.value">
            {{ cat.label }} ({{ cat.fee }} Kč/osoba)
          </option>
        </select>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="input-label">SPZ</label>
          <input v-model="form.vehiclePlate" required class="input-field" placeholder="5H1 2345" />
        </div>
        <div>
          <label class="input-label">Ročník</label>
          <input v-model="form.vehicleYear" type="number" required min="1900" :max="new Date().getFullYear() + 1" class="input-field" />
        </div>
      </div>

      <div>
        <label class="input-label">Počet členů posádky</label>
        <input v-model.number="form.crewCount" type="number" required min="1" max="10" class="input-field" />
      </div>

      <div v-if="form.vehicleCategory" class="card !p-4 text-body-sm">
        <p class="text-text-muted">
          Startovné: <span class="font-semibold text-text">{{ perPersonFee }} Kč</span> ×
          {{ form.crewCount }} osoba/y =
          <strong class="text-primary">{{ totalFee }} Kč</strong>
        </p>
      </div>

      <p v-if="error" class="text-body-sm text-error">{{ error }}</p>

      <button type="submit" :disabled="loading" class="btn-primary w-full">
        {{ loading ? 'Odesílám…' : 'Odeslat přihlášku' }}
      </button>
    </form>
  </div>
</template>
