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
  <div class="max-w-lg">
    <h1 class="text-2xl font-bold text-white">Přihláška do soutěže</h1>
    <p class="mt-1 text-sm text-slate-400">Vyplňte údaje o posádce a vozidle</p>

    <div v-if="submitted && result" class="mt-8 space-y-6">
      <div class="rounded-xl border border-emerald-800 bg-emerald-900/30 p-6">
        <p class="text-sm font-medium text-emerald-400">Přihláška přijata ✓</p>
        <p class="mt-1 text-slate-300">Děkujeme za přihlášení. Vaše startovní číslo:</p>
        <p class="mt-2 text-4xl font-bold text-amber-400">#{{ result.startNumber }}</p>
      </div>

      <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-6 space-y-2">
        <h2 class="text-sm font-medium text-slate-400">Platební údaje</h2>
        <p class="text-white">Částka: <strong class="text-amber-300">{{ result.startFee }} Kč</strong></p>
        <p class="text-slate-300">Bankovní účet: <span class="text-white font-mono">2802609342/2010</span></p>
        <p class="text-slate-300">Variabilní symbol: <span class="text-white font-mono">{{ result.startNumber }}</span></p>
        <p class="text-xs text-slate-500 mt-2">Do zprávy pro příjemce uveďte název posádky.</p>
      </div>

      <div class="rounded-xl border border-slate-800 bg-slate-900/60 p-6 text-center">
        <h2 class="text-sm font-medium text-slate-400 mb-3">QR kód pro prezenci</h2>
        <img :src="qrUrl" alt="QR kód" class="mx-auto rounded-lg" width="200" height="200" />
        <p class="mt-2 text-xs text-slate-500">Předložte při prezenci v den závodu</p>
      </div>
    </div>

    <form v-else @submit.prevent="handleSubmit" class="mt-6 space-y-5">
      <div>
        <label class="block text-sm font-medium text-slate-300">Název posádky</label>
        <input
          v-model="form.teamName"
          required
          class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white placeholder-slate-500 focus:border-amber-500 focus:outline-none"
          placeholder="např. Tým Pardubice"
        />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-slate-300">E-mail</label>
          <input
            v-model="form.email"
            type="email"
            required
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white placeholder-slate-500 focus:border-amber-500 focus:outline-none"
            placeholder="posadka@example.cz"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300">Telefon</label>
          <input
            v-model="form.phone"
            type="tel"
            required
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white placeholder-slate-500 focus:border-amber-500 focus:outline-none"
            placeholder="+420 777 123 456"
          />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300">Kategorie vozidla</label>
        <select
          v-model="form.vehicleCategory"
          required
          class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
        >
          <option value="" disabled>Vyberte kategorii</option>
          <option v-for="cat in CATEGORIES" :key="cat.value" :value="cat.value">
            {{ cat.label }} ({{ cat.fee }} Kč/osoba)
          </option>
        </select>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-slate-300">SPZ</label>
          <input
            v-model="form.vehiclePlate"
            required
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white placeholder-slate-500 focus:border-amber-500 focus:outline-none"
            placeholder="5H1 2345"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-slate-300">Ročník</label>
          <input
            v-model="form.vehicleYear"
            type="number"
            required
            min="1900"
            :max="new Date().getFullYear() + 1"
            class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
          />
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-slate-300">Počet členů posádky</label>
        <input
          v-model.number="form.crewCount"
          type="number"
          required
          min="1"
          max="10"
          class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
        />
      </div>

      <div
        v-if="form.vehicleCategory"
        class="rounded-lg border border-slate-700 bg-slate-800/50 px-4 py-3 text-sm"
      >
        <p class="text-slate-400">
          Startovné: <span class="text-white">{{ perPersonFee }} Kč</span> ×
          {{ form.crewCount }} osoba/y =
          <strong class="text-amber-300">{{ totalFee }} Kč</strong>
        </p>
      </div>

      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>

      <button
        type="submit"
        :disabled="loading"
        class="w-full rounded-lg bg-amber-500 px-4 py-3 font-semibold text-black transition hover:bg-amber-400 disabled:opacity-50"
      >
        {{ loading ? 'Odesílám…' : 'Odeslat přihlášku' }}
      </button>
    </form>
  </div>
</template>
