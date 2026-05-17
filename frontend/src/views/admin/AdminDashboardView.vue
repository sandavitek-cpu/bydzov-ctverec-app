<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { apiBaseUrl, approveRegistration, impersonateRegistration } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout, impersonateAs } = useAuth()
const { show: showToast } = useToast()

interface AdminReg {
  id: number; teamName: string; email: string; phone: string
  vehicleCategory: string; vehicleMake: string; vehiclePlate: string
  vehicleYear: number; crewCount: number; startNumber: number
  startFee: number; status: string; variant: string | null
  firstName: string | null; lastName: string | null; firstTime: boolean
  gender: string | null; driverAge: number | null; club: string | null
  address: string | null; youngestAge: number | null; youngestName: string | null
  engineDisplacement: number | null; power: number | null; maxSpeed: number | null
  vehicleNotes: string | null; notes: string | null; contacted: boolean
  properlyRegistered: boolean; arrived: boolean; consent: boolean
  approved: boolean; createdAt: string
}

interface AdminStats {
  totalCrews: number; totalMembers: number; paid: number
  contacted: number; arrived: number; firstTimers: number
  women: number; kidsUnder10: number; vehiclesBefore1945: number
  cars: number; motos: number
  oldestVehicle: number; newestVehicle: number
  oldestDriver: number; youngestDriver: number
  jednodenni: number; dvoudenni: number; withoutAccommodation: number
  jednodenniMembers: number; dvoudenniMembers: number; approved: number
}

const registrations = ref<AdminReg[]>([])
const stats = ref<AdminStats | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const selected = ref<AdminReg | null>(null)
const approvingId = ref<number | null>(null)

const filterVariant = ref('all')
const filterStatus = ref('all')
const filterSearch = ref('')
const selectedIds = ref<Set<number>>(new Set())
const batchProcessing = ref(false)

if (!isAdmin.value) {
  router.push('/admin/login')
}

const filtered = computed(() => {
  let list = registrations.value
  if (filterVariant.value !== 'all') {
    list = list.filter(r => r.variant === filterVariant.value)
  }
  if (filterStatus.value !== 'all') {
    list = list.filter(r => r.status === filterStatus.value)
  }
  if (filterSearch.value.trim()) {
    const q = filterSearch.value.toLowerCase()
    list = list.filter(r =>
      r.teamName.toLowerCase().includes(q) ||
      r.vehiclePlate.toLowerCase().includes(q) ||
      r.email.toLowerCase().includes(q) ||
      r.lastName?.toLowerCase().includes(q) ||
      r.firstName?.toLowerCase().includes(q)
    )
  }
  return list
})

function toggleSelect(id: number) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  selectedIds.value = s
}

function toggleSelectAll() {
  if (selectedIds.value.size === filtered.value.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(filtered.value.map(r => r.id))
  }
}

async function batchAction(action: 'approve' | 'paid' | 'pending') {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) return
  batchProcessing.value = true
  let success = 0; let fail = 0
  for (const id of ids) {
    try {
      if (action === 'approve') {
        await approveRegistration(id, authHeaders())
      } else {
        const newStatus = action === 'paid' ? 'PAID' : 'PENDING'
        const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${id}/status`, {
          method: 'PATCH',
          headers: { 'Content-Type': 'application/json', ...authHeaders() },
          body: JSON.stringify({ status: newStatus }),
        })
        if (!res.ok) throw new Error()
      }
      success++
    } catch {
      fail++
    }
  }
  selectedIds.value = new Set()
  await fetchAll()
  batchProcessing.value = false
  const actionLabel = action === 'approve' ? 'Schváleno' : action === 'paid' ? 'Zaplaceno' : 'Vráceno'
  if (fail > 0) {
    showToast(`${actionLabel}: ${success} OK, ${fail} selhalo`, 'error')
  } else {
    showToast(`${actionLabel}: ${success} posádek`, 'success')
  }
}

async function fetchAll() {
  loading.value = true
  error.value = null
  try {
    const [regRes, statsRes] = await Promise.all([
      fetch(`${apiBaseUrl}/api/admin/registrations`, { headers: authHeaders() }),
      fetch(`${apiBaseUrl}/api/admin/registrations/stats`, { headers: authHeaders() }),
    ])
    if (regRes.status === 403) { logout(); router.push('/admin/login'); return }
    registrations.value = await regRes.json()
    stats.value = await statsRes.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(reg: AdminReg) {
  const newStatus = reg.status === 'PAID' ? 'PENDING' : 'PAID'
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ status: newStatus }),
    })
    if (!res.ok) throw new Error()
    reg.status = newStatus
    await fetchAll()
  } catch {
    error.value = 'Nepodařilo se změnit stav'
  }
}

async function handleApprove(reg: AdminReg) {
  approvingId.value = reg.id
  try {
    await approveRegistration(reg.id, authHeaders())
    await fetchAll()
    showToast(`${reg.teamName} schválen`, 'success')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Schválení selhalo'
  } finally {
    approvingId.value = null
  }
}

async function handleImpersonate(reg: AdminReg) {
  try {
    const result = await impersonateRegistration(reg.id, authHeaders())
    impersonateAs(result.accessToken, result.role, result.name, result.username)
    router.push('/')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Přepnutí selhalo'
  }
}

async function downloadCsv() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/export`, {
      headers: authHeaders(),
    })
    if (!res.ok) throw new Error()
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'prihlasky.csv'; a.click()
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

function selectReg(r: AdminReg) {
  selected.value = r
}

function closeDetail() {
  selected.value = null
}

onMounted(fetchAll)

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Motocykl', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
}

const variantLabel: Record<string, string> = {
  JEDNODENNI: 'Jednodenní', DVODENNI: 'Dvoudenní',
}
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Přihlášky</h1>
        <p class="text-body-sm text-text-soft">{{ filtered.length }} z {{ registrations.length }} posádek</p>
      </div>
      <div class="flex gap-3">
        <button @click="downloadCsv" class="btn-secondary btn-sm">CSV</button>
        <button v-if="registrations.some(r => r.status !== 'PAID')" @click="mailtoUnpaid" class="btn-ghost btn-sm">Upomínka</button>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex flex-wrap gap-3 mb-6">
      <select v-model="filterVariant" class="input-field !w-auto !h-[36px] text-body-sm">
        <option value="all">Všechny varianty</option>
        <option value="JEDNODENNI">Jednodenní</option>
        <option value="DVODENNI">Dvoudenní</option>
      </select>
      <select v-model="filterStatus" class="input-field !w-auto !h-[36px] text-body-sm">
        <option value="all">Všechny stavy</option>
        <option value="PAID">Zaplaceno</option>
        <option value="PENDING">Čeká na platbu</option>
      </select>
      <input v-model="filterSearch" placeholder="Hledat tým, SPZ, email…" class="input-field !w-auto !min-w-[200px] !h-[36px] text-body-sm flex-1" />
    </div>

    <!-- Batch actions -->
    <div v-if="selectedIds.size > 0" class="mb-4 flex items-center gap-3 rounded-xl border border-primary/30 bg-primary/5 px-4 py-2">
      <span class="text-body-sm font-semibold text-text">{{ selectedIds.size }} vybráno</span>
      <button @click="batchAction('approve')" :disabled="batchProcessing" class="btn-primary btn-xs">Schválit</button>
      <button @click="batchAction('paid')" :disabled="batchProcessing" class="btn-secondary btn-xs">Označit zaplaceno</button>
      <button @click="batchAction('pending')" :disabled="batchProcessing" class="btn-ghost btn-xs">Vrátit na čeká</button>
      <button @click="selectedIds = new Set()" class="btn-ghost btn-xs text-text-soft">Zrušit výběr</button>
    </div>

    <!-- Stats -->
    <div v-if="stats" class="mb-8 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
      <div class="card !p-4 text-center">
        <p class="text-kpi text-primary">{{ stats.totalCrews }}</p>
        <p class="text-meta text-text-soft mt-0.5">Posádek</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-primary">{{ stats.totalMembers }}</p>
        <p class="text-meta text-text-soft mt-0.5">Členů</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi" :class="stats.paid === stats.totalCrews ? 'text-success' : 'text-red'">{{ stats.paid }}<span class="text-body-sm text-text-soft">/{{ stats.totalCrews }}</span></p>
        <p class="text-meta text-text-soft mt-0.5">Zaplaceno</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi" :class="stats.arrived === stats.totalCrews ? 'text-success' : 'text-text-muted'">{{ stats.arrived }}</p>
        <p class="text-meta text-text-soft mt-0.5">Přijelo</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.cars }}</p>
        <p class="text-meta text-text-soft mt-0.5">Automobilů</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.motos }}</p>
        <p class="text-meta text-text-soft mt-0.5">Motocyklů</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.vehiclesBefore1945 }}</p>
        <p class="text-meta text-text-soft mt-0.5">Do 1945</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.firstTimers }}</p>
        <p class="text-meta text-text-soft mt-0.5">Nováčků</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-red">{{ stats.oldestVehicle }}</p>
        <p class="text-meta text-text-soft mt-0.5">Nejst. vozidlo</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-red">{{ stats.oldestDriver }}<span class="text-body-sm text-text-soft"> let</span></p>
        <p class="text-meta text-text-soft mt-0.5">Nejst. řidič</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.jednodenni }} / {{ stats.dvoudenni }} / {{ stats.withoutAccommodation }}</p>
        <p class="text-meta text-text-soft mt-0.5">1den / 2den / bez</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.kidsUnder10 }}</p>
        <p class="text-meta text-text-soft mt-0.5">Dětí do 10 let</p>
      </div>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-12 text-center">Načítám…</p>
    <p v-else-if="error" class="alert alert-error mb-4">{{ error }}</p>

    <div v-else-if="filtered.length === 0" class="py-12 text-center">
      <p class="text-section-title text-text-soft">Žádné přihlášky neodpovídají filtrům</p>
    </div>

    <!-- Table -->
    <div v-else class="overflow-x-auto rounded-xl border border-border">
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th class="w-8">
              <input type="checkbox" :checked="selectedIds.size === filtered.length && filtered.length > 0" @change="toggleSelectAll" class="cursor-pointer" />
            </th>
            <th class="w-8 text-center">#</th>
            <th>Posádka</th>
            <th class="hidden sm:table-cell">Varianta</th>
            <th>Stav</th>
            <th class="hidden md:table-cell">Schválení</th>
            <th class="text-right">Akce</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in filtered" :key="r.id"
            @click="selectReg(r)"
            class="table-row cursor-pointer"
          >
            <td class="text-center" @click.stop>
              <input type="checkbox" :checked="selectedIds.has(r.id)" @change="toggleSelect(r.id)" class="cursor-pointer" />
            </td>
            <td class="text-center font-mono font-bold text-primary">{{ r.startNumber }}</td>
            <td>
              <div class="font-medium text-text">{{ r.teamName }}</div>
              <div class="text-meta text-text-soft">{{ r.vehicleCategory ? categoryLabel[r.vehicleCategory] ?? r.vehicleCategory : '' }}{{ r.vehicleMake ? ' · ' + r.vehicleMake : '' }}</div>
            </td>
            <td class="hidden sm:table-cell">
              <span v-if="r.variant" class="text-body-sm text-text-muted">{{ variantLabel[r.variant] ?? r.variant }}</span>
              <span v-else class="text-body-sm text-text-soft">—</span>
            </td>
            <td>
              <div class="flex flex-wrap gap-1.5">
                <button @click.stop="toggleStatus(r)"
                  class="badge cursor-pointer transition-colors"
                  :class="r.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'"
                >{{ r.status === 'PAID' ? 'Zaplaceno' : 'Čeká' }}</button>
                <span v-if="r.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
                <span v-if="r.firstTime" class="badge !bg-red/10 !text-red">Nový</span>
              </div>
            </td>
            <td class="hidden md:table-cell">
              <div class="flex flex-wrap gap-1.5">
                <span v-if="r.approved" class="badge !bg-success/10 !text-success">Schváleno</span>
                <span v-else class="badge !bg-warning/10 !text-warning">Čeká</span>
              </div>
            </td>
            <td class="text-right">
              <div class="flex gap-1.5 justify-end" @click.stop>
                <button v-if="!r.approved" @click="handleApprove(r)" :disabled="approvingId === r.id"
                  class="btn-primary btn-xs"
                >{{ approvingId === r.id ? '…' : 'Schválit' }}</button>
                <button v-if="r.approved" @click="handleImpersonate(r)"
                  class="btn-ghost btn-xs"
                  title="Přihlásit jako tento tým"
                >👁</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Detail modal -->
    <div v-if="selected" class="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 py-8" @click.self="closeDetail">
      <div class="mx-4 w-full max-w-lg rounded-xl border border-border bg-surface p-6 shadow-lg">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-subsection text-text">#{{ selected.startNumber }} – {{ selected.teamName }}</h2>
          <button @click="closeDetail" class="text-text-soft hover:text-text text-xl leading-none">&times;</button>
        </div>

        <div class="space-y-4">
          <!-- Status badges -->
          <div class="flex flex-wrap gap-2">
            <span class="badge" :class="selected.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'">
              {{ selected.status === 'PAID' ? 'Zaplaceno' : 'Čeká na platbu' }}
            </span>
            <span v-if="selected.approved" class="badge !bg-success/10 !text-success">Schváleno</span>
            <span v-else class="badge !bg-warning/10 !text-warning">Neschváleno</span>
            <span v-if="selected.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
            <span v-if="selected.contacted" class="badge !bg-info/10 !text-info">Kontaktován</span>
            <span v-if="selected.firstTime" class="badge !bg-red/10 !text-red">Nováček</span>
            <span v-if="selected.properlyRegistered" class="badge !bg-success/10 !text-success">Přihlášeno</span>
          </div>

          <!-- Vehicle info -->
          <div class="rounded-lg border border-border bg-bg-alt p-4">
            <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-body-sm">
              <div><span class="text-text-soft">Kategorie:</span> <span class="text-text">{{ categoryLabel[selected.vehicleCategory] ?? selected.vehicleCategory }}</span></div>
              <div><span class="text-text-soft">Varianta:</span> <span class="text-text">{{ variantLabel[selected.variant ?? ''] ?? selected.variant ?? '—' }}</span></div>
              <div><span class="text-text-soft">Značka:</span> <span class="text-text">{{ selected.vehicleMake || '—' }}</span></div>
              <div><span class="text-text-soft">SPZ:</span> <span class="text-text font-mono">{{ selected.vehiclePlate }}</span></div>
              <div><span class="text-text-soft">Ročník:</span> <span class="text-text">{{ selected.vehicleYear }}</span></div>
              <div><span class="text-text-soft">Posádka:</span> <span class="text-text">{{ selected.crewCount }} osob</span></div>
              <div v-if="selected.engineDisplacement"><span class="text-text-soft">Obsah:</span> <span class="text-text">{{ selected.engineDisplacement }} ccm</span></div>
              <div v-if="selected.power"><span class="text-text-soft">Výkon:</span> <span class="text-text">{{ selected.power }} kW</span></div>
              <div v-if="selected.maxSpeed"><span class="text-text-soft">Max. rychlost:</span> <span class="text-text">{{ selected.maxSpeed }} km/h</span></div>
            </div>
            <p v-if="selected.vehicleNotes" class="mt-2 text-body-sm text-text-muted italic">{{ selected.vehicleNotes }}</p>
          </div>

          <!-- Driver info -->
          <div class="rounded-lg border border-border bg-bg-alt p-4">
            <h3 class="text-label text-text mb-2">Řidič</h3>
            <div class="grid grid-cols-2 gap-x-6 gap-y-2 text-body-sm">
              <div><span class="text-text-soft">Jméno:</span> <span class="text-text">{{ selected.firstName || selected.teamName }}{{ selected.lastName ? ' ' + selected.lastName : '' }}</span></div>
              <div v-if="selected.driverAge"><span class="text-text-soft">Věk:</span> <span class="text-text">{{ selected.driverAge }} let</span></div>
              <div v-if="selected.gender"><span class="text-text-soft">Pohlaví:</span> <span class="text-text">{{ selected.gender === 'M' ? 'Muž' : selected.gender === 'Z' ? 'Žena' : selected.gender }}</span></div>
              <div v-if="selected.club"><span class="text-text-soft">Klub:</span> <span class="text-text">{{ selected.club }}</span></div>
              <div v-if="selected.address" class="col-span-2"><span class="text-text-soft">Adresa:</span> <span class="text-text">{{ selected.address }}</span></div>
            </div>
          </div>

          <!-- Youngest member -->
          <div v-if="selected.youngestName || selected.youngestAge != null" class="rounded-lg border border-border bg-bg-alt p-4">
            <h3 class="text-label text-text mb-2">Nejmladší člen</h3>
            <p class="text-body-sm text-text-muted">
              {{ selected.youngestName || '—' }}{{ selected.youngestAge != null ? ' (' + selected.youngestAge + ' let)' : '' }}
            </p>
          </div>

          <!-- Notes -->
          <p v-if="selected.notes" class="rounded-lg border border-border bg-bg-alt p-4 text-body-sm text-text-muted italic">
            {{ selected.notes }}
          </p>

          <!-- Contact -->
          <div class="grid grid-cols-2 gap-4 text-body-sm">
            <div><span class="text-text-soft">Email:</span><br /><span class="text-text">{{ selected.email }}</span></div>
            <div><span class="text-text-soft">Telefon:</span><br /><span class="text-text">{{ selected.phone }}</span></div>
          </div>

          <!-- Fee -->
          <div class="rounded-lg border border-border bg-bg-alt p-4 flex items-center justify-between">
            <span class="text-text-soft text-body-sm">Startovné</span>
            <span class="text-kpi text-primary">{{ selected.startFee }} Kč</span>
          </div>

          <p class="text-meta text-text-soft text-center">Přihlášeno: {{ new Date(selected.createdAt).toLocaleString('cs') }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
