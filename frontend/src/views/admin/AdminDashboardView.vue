<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { apiBaseUrl, impersonateRegistration, assignStartNumber } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout, impersonateAs } = useAuth()
const { show: showToast } = useToast()

interface CrewInfo {
  firstName: string; lastName: string; email: string
  driverAge: number | null; gender: string | null; address: string | null
  clubMember: boolean; clubName: string | null; firstTime: boolean
}

interface AdminReg {
  id: number; teamName: string; email: string; phone: string
  vehicleCategory: string; vehicleMake: string; vehiclePlate: string
  vehicleYear: number; crewCount: number; startNumber: number
  startFee: number; paymentReference: number | null; status: string; variant: string | null
  firstName: string | null; lastName: string | null; firstTime: boolean
  gender: string | null; driverAge: number | null; club: string | null
  address: string | null; youngestAge: number | null; youngestName: string | null
  engineDisplacement: number | null; power: number | null; maxSpeed: number | null
  vehicleNotes: string | null; notes: string | null; contacted: boolean
  properlyRegistered: boolean; arrived: boolean; consent: boolean
  approved: boolean; createdAt: string; paidAt: string | null; crewMembers: CrewInfo[]
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
const editing = ref(false)
const editForm = ref<Record<string, any>>({})
const saving = ref(false)
const resendingId = ref<number | null>(null)

const filterVariant = ref('all')
const filterStatus = ref('all')
const filterSearch = ref('')
const selectedIds = ref<Set<number>>(new Set())
const batchProcessing = ref(false)

const raceStatus = ref<{ started: boolean; finished: boolean } | null>(null)

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

async function batchAction(action: 'paid' | 'pending') {
  const ids = Array.from(selectedIds.value)
  if (ids.length === 0) return
  batchProcessing.value = true
  let success = 0; let fail = 0
  for (const id of ids) {
    try {
      const newStatus = action === 'paid' ? 'PAID' : 'PENDING'
      const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json', ...authHeaders() },
        body: JSON.stringify({ status: newStatus }),
      })
      if (!res.ok) throw new Error()
      success++
    } catch {
      fail++
    }
  }
  selectedIds.value = new Set()
  await fetchAll()
  batchProcessing.value = false
  const actionLabel = action === 'paid' ? 'Přihlášen a zaplaceno' : 'Vráceno na nezaplaceno'
  if (fail > 0) {
    showToast(`${actionLabel}: ${success} OK, ${fail} selhalo`, 'error')
  } else {
    showToast(`${actionLabel}: ${success} posádek`, 'success')
  }
}

async function fetchRaceStatus() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/race`, { headers: authHeaders() })
    if (res.ok) raceStatus.value = await res.json()
  } catch { /* ignore */ }
}

async function handleRaceStart() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/race/start`, {
      method: 'POST', headers: authHeaders(),
    })
    if (!res.ok) { showToast('Nepodařilo se spustit závod', 'error'); return }
    await fetchRaceStatus()
    showToast('Závod zahájen!', 'success')
  } catch { showToast('Chyba při spouštění závodu', 'error') }
}

async function handleRaceFinish() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/race/finish`, {
      method: 'POST', headers: authHeaders(),
    })
    if (!res.ok) { showToast('Nepodařilo se ukončit závod', 'error'); return }
    await fetchRaceStatus()
    showToast('Závod ukončen!', 'success')
  } catch { showToast('Chyba při ukončování závodu', 'error') }
}

async function handleRaceReset() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/race/reset`, {
      method: 'POST', headers: authHeaders(),
    })
    if (!res.ok) { showToast('Nepodařilo se resetovat závod', 'error'); return }
    await fetchRaceStatus()
    showToast('Závod resetován', 'info')
  } catch { showToast('Chyba při resetu závodu', 'error') }
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
    if (selected.value?.id === reg.id) {
      selected.value = reg
    }
    await fetchAll()
  } catch {
    error.value = 'Nepodařilo se změnit stav'
  }
}

async function handleResend(reg: AdminReg) {
  resendingId.value = reg.id
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/resend-credentials`, {
      method: 'POST', headers: authHeaders(),
    })
    const body = await res.json()
    if (!res.ok) throw new Error(body.error ?? 'Chyba')
    await fetchAll()
    showToast(`Přihlašovací údaje odeslány (${body.resent} uživatelům)`, 'success')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Odeslání selhalo'
  } finally {
    resendingId.value = null
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

async function handleAssignStartNumber(reg: AdminReg) {
  try {
    const result = await assignStartNumber(reg.id, authHeaders())
    reg.startNumber = result.startNumber
    showToast(`Startovní číslo #${result.startNumber} přiděleno`, 'success')
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Přidělení selhalo', 'error')
  }
}

async function downloadPdf() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/export/pdf`, {
      headers: authHeaders(),
    })
    if (!res.ok) throw new Error()
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'startovni_listina.pdf'; a.click()
    URL.revokeObjectURL(url)
  } catch {
    showToast('Nepodařilo se stáhnout PDF', 'error')
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
  window.location.href = `mailto:${bcc}?subject=${encodeURIComponent('Novobydžovský čtverec 2026 – připomínka platby startovného')}&body=${encodeURIComponent('Dobrý den,\n\nrádi bychom Vás upozornili, že Vaše startovné za přihlášku do Novobydžovského čtverce 2026 dosud nebylo uhrazeno.\n\nProsíme o co nejdřívější úhradu na účet 1086360369/0800 (Česká spořitelna) s variabilním symbolem Vašeho startovního čísla.\n\nDěkujeme.\n\nTým Novobydžovského čtverce')}`
}

function selectReg(r: AdminReg) {
  selected.value = r
  editing.value = false
}

function closeDetail() {
  selected.value = null
  editing.value = false
}

function startEdit() {
  if (!selected.value) return
  editForm.value = { ...selected.value }
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveEdit() {
  if (!selected.value) return
  saving.value = true
  try {
    const allowed = [
      'variant', 'vehicleMake', 'firstTime', 'gender', 'driverAge', 'club',
      'address', 'youngestAge', 'youngestName', 'engineDisplacement', 'power',
      'maxSpeed', 'vehicleNotes', 'notes', 'contacted', 'properlyRegistered',
      'arrived', 'consent'
    ]
    const body: Record<string, any> = {}
    for (const key of allowed) {
      if (key in editForm.value) body[key] = editForm.value[key]
    }
    const res = await fetch(`${apiBaseUrl}/api/admin/registrations/${selected.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error()
    const updated = await res.json()
    Object.assign(selected.value, updated)
    editing.value = false
    showToast('Údaje uloženy', 'success')
    await fetchAll()
  } catch {
    showToast('Nepodařilo se uložit změny', 'error')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchAll()
  fetchRaceStatus()
})

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
        <button @click="downloadPdf" class="btn-secondary btn-sm">PDF</button>
        <button @click="downloadCsv" class="btn-secondary btn-sm">CSV</button>
        <button v-if="registrations.some(r => r.status !== 'PAID')" @click="mailtoUnpaid" class="btn-ghost btn-sm">Upomínka</button>
      </div>
    </div>

    <!-- Race Mode -->
    <div class="mb-6 rounded-xl border p-4" :class="{
      'border-success/30 bg-success/5': raceStatus?.started && !raceStatus?.finished,
      'border-border bg-bg-alt': !raceStatus?.started,
      'border-info/30 bg-info/5': raceStatus?.finished,
    }">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div class="flex items-center gap-3">
          <span class="text-label">Režim závodu:</span>
          <span v-if="!raceStatus?.started" class="badge !bg-text-soft/10 !text-text-soft">Nezahájen</span>
          <span v-else-if="!raceStatus?.finished" class="badge !bg-success/10 !text-success">Probíhá</span>
          <span v-else class="badge !bg-info/10 !text-info">Ukončen</span>
        </div>
        <div class="flex gap-2 flex-wrap">
          <button v-if="!raceStatus?.started" @click="handleRaceStart" class="btn-primary btn-xs">Zahájit závod</button>
          <button v-if="raceStatus?.started && !raceStatus?.finished" @click="handleRaceFinish" class="btn-secondary btn-xs">Ukončit závod</button>
          <button v-if="raceStatus?.started" @click="handleRaceReset" class="btn-ghost btn-xs">Reset</button>
        </div>
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
        <option value="PAID">Přihlášen a zaplaceno</option>
        <option value="PENDING">Přihlášen, nezaplaceno</option>
      </select>
      <input v-model="filterSearch" placeholder="Hledat tým, SPZ, email…" class="input-field !w-full sm:!w-auto sm:!min-w-[200px] !h-[36px] text-body-sm flex-1" />
    </div>

    <!-- Batch actions -->
    <div v-if="selectedIds.size > 0" class="mb-4 flex items-center gap-3 rounded-xl border border-primary/30 bg-primary/5 px-4 py-2">
      <span class="text-body-sm font-semibold text-text">{{ selectedIds.size }} vybráno</span>
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
        <p class="text-meta text-text-soft mt-0.5">Přihlášeno a zaplaceno</p>
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
            <th>Členové</th>
            <th>Stav</th>
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
              <div class="flex flex-wrap gap-1">
                <span v-for="(cm, i) in r.crewMembers" :key="i"
                  class="inline-flex items-center gap-1 rounded-full bg-primary/5 px-2 py-0.5 text-meta text-text-muted"
                  :title="cm.email"
                >{{ cm.firstName }} {{ cm.lastName }}</span>
                <span v-if="!r.crewMembers?.length" class="text-meta text-text-soft">—</span>
              </div>
            </td>
            <td>
              <div class="flex flex-wrap gap-1.5">
                <button @click.stop="toggleStatus(r)"
                  class="badge cursor-pointer transition-colors"
                  :class="r.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'"
                >{{ r.status === 'PAID' ? 'Přihlášen a zaplaceno' : 'Přihlášen, nezaplaceno' }}</button>
                <span v-if="r.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
                <span v-if="r.firstTime" class="badge !bg-red/10 !text-red">Nový</span>
              </div>
            </td>
            <td class="text-right whitespace-nowrap">
              <div class="flex gap-1.5 justify-end" @click.stop>
                <button @click="handleResend(r)" :disabled="resendingId === r.id"
                  class="btn-ghost btn-xs whitespace-nowrap" title="Znovu odeslat přihlašovací údaje"
                >{{ resendingId === r.id ? '…' : 'Poslat údaje' }}</button>
                <button @click="handleImpersonate(r)"
                  class="btn-ghost btn-xs"
                  title="Přihlásit jako tento tým"
                >👁</button>
                <button v-if="r.status === 'PAID' && (!r.startNumber || r.startNumber === 0)" @click.stop="handleAssignStartNumber(r)"
                  class="btn-secondary btn-xs whitespace-nowrap"
                  title="Přidělit startovní číslo"
                >#</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Detail modal -->
    <div v-if="selected" class="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 py-4 sm:py-8" @click.self="closeDetail">
      <div class="mx-4 w-full max-w-lg rounded-xl border border-border bg-surface p-4 sm:p-6 shadow-lg">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-subsection text-text">#{{ selected.startNumber }} – {{ selected.teamName }}</h2>
          <div class="flex items-center gap-2">
            <button v-if="!editing" @click="startEdit" class="btn-secondary btn-xs">Upravit</button>
            <button @click="closeDetail" class="text-text-soft hover:text-text text-xl leading-none">&times;</button>
          </div>
        </div>

        <div v-if="!editing" class="space-y-4">
          <!-- Payment status toggle -->
          <div>
            <p class="text-label text-text-soft mb-1.5">Stav platby</p>
            <div class="flex rounded-lg border border-border overflow-hidden">
              <button @click.stop="toggleStatus(selected)"
                class="flex-1 px-3 py-1.5 text-body-sm font-medium transition-colors"
                :class="selected.status === 'PENDING'
                  ? 'bg-warning/10 text-warning'
                  : 'bg-surface text-text-soft hover:text-text'"
              >Přihlášen, nezaplaceno</button>
              <button @click.stop="toggleStatus(selected)"
                class="flex-1 px-3 py-1.5 text-body-sm font-medium transition-colors"
                :class="selected.status === 'PAID'
                  ? 'bg-success/10 text-success'
                  : 'bg-surface text-text-soft hover:text-text'"
              >Přihlášen a zaplaceno</button>
            </div>
          </div>

          <!-- Other badges -->
          <div class="flex flex-wrap gap-2">
            <span v-if="selected.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
            <span v-if="selected.contacted" class="badge !bg-info/10 !text-info">Kontaktován</span>
            <span v-if="selected.firstTime" class="badge !bg-red/10 !text-red">Nováček</span>
            <span v-if="selected.properlyRegistered" class="badge !bg-success/10 !text-success">Přihlášeno</span>
          </div>

          <!-- Crew Members -->
          <div v-if="selected.crewMembers?.length" class="rounded-lg border border-border bg-bg-alt p-4">
            <h3 class="text-label text-text mb-2">Členové posádky</h3>
            <div class="space-y-2">
              <div v-for="(cm, i) in selected.crewMembers" :key="i"
                class="rounded-md border border-border/50 bg-surface p-3 text-body-sm space-y-1">
                <div class="flex items-center justify-between">
                  <span class="font-medium text-text">{{ cm.firstName }} {{ cm.lastName }}</span>
                  <span class="text-text-soft text-meta">{{ cm.email }}</span>
                </div>
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-meta text-text-muted">
                  <span v-if="cm.driverAge">Věk: {{ cm.driverAge }}</span>
                  <span v-if="cm.gender">{{ cm.gender === 'M' ? 'Muž' : cm.gender === 'Z' ? 'Žena' : cm.gender }}</span>
                  <span v-if="cm.address">{{ cm.address }}</span>
                  <span v-if="cm.clubMember" class="text-primary">{{ cm.clubName ? 'Klub: ' + cm.clubName : 'Člen klubu' }}</span>
                  <span v-if="cm.firstTime" class="text-red">Nováček</span>
                </div>
              </div>
            </div>
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
            <div v-if="selected.paymentReference"><span class="text-text-soft">VS:</span><br /><span class="text-text font-mono">{{ selected.paymentReference }}</span></div>
          </div>

          <!-- Fee -->
          <div class="rounded-lg border border-border bg-bg-alt p-4 flex items-center justify-between">
            <span class="text-text-soft text-body-sm">Startovné</span>
            <span class="text-kpi text-primary">{{ selected.startFee }} Kč</span>
          </div>

          <p class="text-meta text-text-soft text-center">Přihlášeno: {{ new Date(selected.createdAt).toLocaleString('cs') }}</p>
          <p v-if="selected.paidAt" class="text-meta text-success text-center">Zaplaceno: {{ new Date(selected.paidAt).toLocaleString('cs') }}</p>
        </div>

        <!-- Edit form -->
        <div v-else class="space-y-4">
          <div class="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-1">
            <div>
              <label class="text-label text-text-soft mb-1 block">Varianta</label>
              <select v-model="editForm.variant" class="input-field w-full">
                <option :value="null">—</option>
                <option value="JEDNODENNI">Jednodenní</option>
                <option value="DVODENNI">Dvoudenní</option>
              </select>
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Značka</label>
              <input v-model="editForm.vehicleMake" class="input-field w-full" />
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">První účast</label>
              <input type="checkbox" v-model="editForm.firstTime" class="cursor-pointer" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-label text-text-soft mb-1 block">Věk řidiče</label>
                <input type="number" v-model.number="editForm.driverAge" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">Pohlaví</label>
                <select v-model="editForm.gender" class="input-field w-full">
                  <option :value="null">—</option>
                  <option value="M">Muž</option>
                  <option value="Z">Žena</option>
                </select>
              </div>
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Klub</label>
              <input v-model="editForm.club" class="input-field w-full" />
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Bydliště</label>
              <input v-model="editForm.address" class="input-field w-full" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-label text-text-soft mb-1 block">Nejmladší člen – věk</label>
                <input type="number" v-model.number="editForm.youngestAge" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">Nejmladší člen – jméno</label>
                <input v-model="editForm.youngestName" class="input-field w-full" />
              </div>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <div>
                <label class="text-label text-text-soft mb-1 block">Obsah (ccm)</label>
                <input type="number" v-model.number="editForm.engineDisplacement" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">Výkon (kW)</label>
                <input type="number" v-model.number="editForm.power" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">Max. rychlost</label>
                <input type="number" v-model.number="editForm.maxSpeed" class="input-field w-full" />
              </div>
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Poznámky k vozidlu</label>
              <textarea v-model="editForm.vehicleNotes" class="input-field w-full" rows="2"></textarea>
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Interní poznámka</label>
              <textarea v-model="editForm.notes" class="input-field w-full" rows="2"></textarea>
            </div>
            <div class="flex flex-wrap gap-4">
              <label class="flex items-center gap-2 text-body-sm cursor-pointer">
                <input type="checkbox" v-model="editForm.contacted" /> Kontaktován
              </label>
              <label class="flex items-center gap-2 text-body-sm cursor-pointer">
                <input type="checkbox" v-model="editForm.properlyRegistered" /> Řádně přihlášen
              </label>
              <label class="flex items-center gap-2 text-body-sm cursor-pointer">
                <input type="checkbox" v-model="editForm.arrived" /> Přijel
              </label>
              <label class="flex items-center gap-2 text-body-sm cursor-pointer">
                <input type="checkbox" v-model="editForm.consent" /> Souhlas
              </label>
            </div>
          </div>
          <div class="flex gap-2 pt-2 border-t border-border">
            <button @click="saveEdit" :disabled="saving" class="btn-primary flex-1">
              {{ saving ? 'Ukládám…' : 'Uložit' }}
            </button>
            <button @click="cancelEdit" class="btn-ghost">Zrušit</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
