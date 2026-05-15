<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import AdminNav from '@/components/admin/AdminNav.vue'
import { apiBaseUrl, type RegistrationResult } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const registrations = ref<RegistrationResult[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function fetchRegistrations() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) {
      logout()
      router.push('/admin/login')
      return
    }
    registrations.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(reg: RegistrationResult) {
  const newStatus = reg.status === 'PAID' ? 'PENDING' : 'PAID'
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ status: newStatus }),
    })
    if (!res.ok) throw new Error()
    reg.status = newStatus
  } catch {
    error.value = 'Nepodařilo se změnit stav'
  }
}

async function downloadCsv() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/export`, {
      headers: { ...authHeaders() },
    })
    if (!res.ok) throw new Error()
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'prihlasky.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    error.value = 'Nepodařilo se stáhnout CSV'
  }
}

function mailtoUnpaid() {
  const unpaid = registrations.value.filter(r => r.status !== 'PAID')
  if (unpaid.length === 0) return
  const bcc = unpaid.map(r => r.email).join(',')
  window.location.href = `mailto:${bcc}?subject=${encodeURIComponent('Novobydžovský čtverec 2026 – připomínka platby startovného')}&body=${encodeURIComponent('Dobrý den,\n\nrádi bychom Vás upozornili, že Vaše startovné za přihlášku do Novobydžovského čtverce 2026 dosud nebylo uhrazeno.\n\nProsíme o co nejdřívější úhradu na účet 2802609342/2010 s variabilním symbolem Vašeho startovního čísla.\n\nDěkujeme.\n\nTým Novobydžovského čtverce')}`
}

onMounted(fetchRegistrations)

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Motocykl',
  OSOBNI: 'Osobní',
  CLASSIC: 'Historické',
  NAKLADNI: 'Nákladní',
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold text-white">Přehled přihlášek</h1>
        <p class="text-sm text-slate-400">{{ registrations.length }} přihlášených</p>
      </div>
      <div class="flex gap-2">
        <button
          @click="downloadCsv"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          CSV
        </button>
        <button
          v-if="registrations.some(r => r.status !== 'PAID')"
          @click="mailtoUnpaid"
          class="rounded-lg border border-amber-700 px-3 py-2 text-sm text-amber-400 transition hover:bg-amber-900/30"
        >
          Upomínka
        </button>
      </div>
      <AdminNav />
    </div>

    <p v-if="loading" class="mt-8 text-slate-500">Načítám…</p>
    <p v-else-if="error" class="mt-8 text-red-400">{{ error }}</p>

    <div v-else-if="registrations.length === 0" class="mt-8 text-slate-500">
      Zatím žádné přihlášky.
    </div>

    <div v-else class="mt-6 overflow-x-auto">
      <table class="w-full text-left text-sm">
        <thead>
          <tr class="border-b border-slate-800 text-slate-500">
            <th class="py-2 pr-4 font-medium">#</th>
            <th class="py-2 pr-4 font-medium">Posádka</th>
            <th class="py-2 pr-4 font-medium">Kategorie</th>
            <th class="py-2 pr-4 font-medium">Vozidlo</th>
            <th class="py-2 pr-4 font-medium">Kontakt</th>
            <th class="py-2 pr-4 font-medium">Startovné</th>
            <th class="py-2 pr-2 font-medium">Stav</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in registrations"
            :key="r.id"
            class="border-b border-slate-800/50 transition hover:bg-slate-900/40"
          >
            <td class="py-3 pr-4 font-mono text-amber-400">{{ r.startNumber }}</td>
            <td class="py-3 pr-4">
              <div class="font-medium text-white">{{ r.teamName }}</div>
              <div class="text-xs text-slate-500">{{ r.vehiclePlate }} ({{ r.vehicleYear }})</div>
            </td>
            <td class="py-3 pr-4 text-slate-300">{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }}</td>
            <td class="py-3 pr-4 text-slate-300">{{ r.crewCount }} os.</td>
            <td class="py-3 pr-4">
              <div class="text-slate-300">{{ r.email }}</div>
              <div class="text-xs text-slate-500">{{ r.phone }}</div>
            </td>
            <td class="py-3 pr-4 font-mono text-slate-300">{{ r.startFee }} Kč</td>
            <td class="py-3 pr-2">
              <button
                @click="toggleStatus(r)"
                class="rounded-full px-2.5 py-0.5 text-xs font-medium transition"
                :class="r.status === 'PAID'
                  ? 'bg-emerald-900/50 text-emerald-400 hover:bg-emerald-800/50'
                  : 'bg-amber-900/30 text-amber-400 hover:bg-amber-800/30'"
              >
                {{ r.status === 'PAID' ? 'Zaplaceno' : 'Čeká' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
