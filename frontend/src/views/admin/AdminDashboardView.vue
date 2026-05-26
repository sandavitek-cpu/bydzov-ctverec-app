<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import RaceControls from '@/components/admin/RaceControls.vue'
import RegistrationFilters from '@/components/admin/RegistrationFilters.vue'
import RegistrationStats from '@/components/admin/RegistrationStats.vue'
import RegistrationTable from '@/components/admin/RegistrationTable.vue'
import { apiBaseUrl, authFetch, impersonateRegistration, assignStartNumber, fetchAdminUsers, fetchAdminVariants, type AdminUser, type VariantConfig } from '@/api'

const router = useRouter()
const { authHeaders, logout, impersonateAs } = useAuth()
const { show: showToast } = useToast()

export interface CrewInfo {
  firstName: string; lastName: string; email: string
  driverAge: number | null; gender: string | null; address: string | null
  clubMember: boolean; clubName: string | null; firstTime: boolean
}

export interface AdminReg {
  id: number; teamName: string; email: string; phone: string
  vehicleCategory: string; vehicleMake: string; vehiclePlate: string
  vehicleYear: number; crewCount: number; startNumber: number
  startFee: number; paidAmount: number | null; paymentReference: number | null; status: string; variant: string | null
  firstName: string | null; lastName: string | null; firstTime: boolean
  gender: string | null; driverAge: number | null; club: string | null
  address: string | null; youngestAge: number | null; youngestName: string | null
  engineDisplacement: number | null; power: number | null; maxSpeed: number | null
  vehicleNotes: string | null; vehicleStory: string | null; notes: string | null; contacted: boolean
  properlyRegistered: boolean; arrived: boolean; consent: boolean
  approved: boolean; createdAt: string; paidAt: string | null; cancelledAt: string | null; refundAmount: number | null; crewMembers: CrewInfo[]
}

export interface AdminStats {
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
const remindingId = ref<number | null>(null)

const filterVariant = ref('all')
const filterStatus = ref('all')
const filterSearch = ref('')
const selectedIds = ref<Set<number>>(new Set())
const batchProcessing = ref(false)

const raceStatus = ref<{ started: boolean; finished: boolean } | null>(null)

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
      const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
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
    const res = await authFetch(`${apiBaseUrl}/api/admin/race`)
    if (res.ok) raceStatus.value = await res.json()
  } catch { /* ignore */ }
}

async function handleRaceStart() {
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/race/start`, {
      method: 'POST',
    })
    if (!res.ok) { showToast('Nepodařilo se spustit závod', 'error'); return }
    await fetchRaceStatus()
    showToast('Závod zahájen!', 'success')
  } catch { showToast('Chyba při spouštění závodu', 'error') }
}

async function handleRaceFinish() {
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/race/finish`, {
      method: 'POST',
    })
    if (!res.ok) { showToast('Nepodařilo se ukončit závod', 'error'); return }
    await fetchRaceStatus()
    showToast('Závod ukončen!', 'success')
  } catch { showToast('Chyba při ukončování závodu', 'error') }
}

async function handleRaceReset() {
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/race/reset`, {
      method: 'POST',
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
      authFetch(`${apiBaseUrl}/api/admin/registrations`),
      authFetch(`${apiBaseUrl}/api/admin/registrations/stats`),
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
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
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
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/resend-credentials`, {
      method: 'POST',
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

async function handleSendReminder(reg: AdminReg) {
  remindingId.value = reg.id
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/remind`, {
      method: 'POST',
    })
    const body = await res.json()
    if (!res.ok) throw new Error(body.error ?? 'Chyba')
    showToast(`Upomínka odeslána na ${reg.email}`, 'success')
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Odeslání selhalo', 'error')
  } finally {
    remindingId.value = null
  }
}

function downloadRegPdf(reg: AdminReg) {
  const a = document.createElement('a')
  a.href = `${apiBaseUrl}/api/admin/registrations/${reg.id}/export/pdf`
  a.download = `prihlaska_${reg.startNumber}_${reg.teamName.replace(/\s+/g, '_')}.pdf`
  authFetch(a.href)
    .then(r => r.blob())
    .then(blob => {
      const url = URL.createObjectURL(blob)
      a.href = url
      a.click()
      URL.revokeObjectURL(url)
    })
    .catch(() => showToast('Nepodařilo se stáhnout PDF', 'error'))
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
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/export/pdf`)
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

async function handleCancel(reg: AdminReg) {
  const msg = reg.paidAmount
    ? 'Tým již zaplatil ' + reg.paidAmount + ' Kč. Po stornu bude vypočtena částka k vrácení (100 % před uzávěrkou, 75 % po uzávěrce). Pokračovat?'
    : 'Opravdu chcete stornovat tuto přihlášku?'
  if (!confirm(msg)) return
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${reg.id}/cancel`, {
      method: 'POST',
    })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error ?? 'Storno selhalo')
    }
    showToast('Přihláška stornována', 'info')
    await fetchAll()
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Storno selhalo', 'error')
  }
}

async function downloadCsv() {
  try {
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/export`)
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
      'variant', 'teamName', 'phone', 'vehicleCategory', 'vehicleMake',
      'vehiclePlate', 'vehicleYear', 'crewCount', 'firstTime', 'gender',
      'driverAge', 'club', 'address', 'youngestAge', 'youngestName',
      'engineDisplacement', 'power', 'maxSpeed', 'vehicleNotes', 'vehicleStory', 'notes',
      'contacted', 'properlyRegistered', 'arrived', 'consent'
    ]
    const body: Record<string, any> = {}
    for (const key of allowed) {
      if (key in editForm.value) body[key] = editForm.value[key]
    }
    const res = await authFetch(`${apiBaseUrl}/api/admin/registrations/${selected.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error()
    const result = await res.json()
    if (result.registration) {
      Object.assign(selected.value, result.registration)
      if (result.message) {
        showToast(result.message, 'info')
      } else {
        showToast('Údaje uloženy', 'success')
      }
    } else {
      Object.assign(selected.value, result)
      showToast('Údaje uloženy', 'success')
    }
    editing.value = false
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
  loadVariantConfigs()
})

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Motocykl', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
  AUTO: 'Automobil',
}

const userQuery = ref('')
const userResults = ref<AdminUser[]>([])
const userSearchLoading = ref(false)
let userSearchTimer: ReturnType<typeof setTimeout> | null = null

async function searchUsers() {
  const q = userQuery.value.trim()
  if (q.length < 2) { userResults.value = []; return }
  userSearchLoading.value = true
  try {
    userResults.value = await fetchAdminUsers(authHeaders(), q)
  } catch {
    userResults.value = []
  } finally {
    userSearchLoading.value = false
  }
}

function onUserInput() {
  if (userSearchTimer) clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(searchUsers, 300)
}

function selectUser(user: AdminUser) {
  editForm.value.firstName = user.firstName
  editForm.value.lastName = user.lastName
  editForm.value.email = user.email
  editForm.value.phone = user.phone
  userQuery.value = `${user.firstName} ${user.lastName} (${user.email})`
  userResults.value = []
}

function clearUserSelection() {
  userQuery.value = ''
  userResults.value = []
  editForm.value.firstName = ''
  editForm.value.lastName = ''
  editForm.value.email = ''
  editForm.value.phone = ''
}

const variantLabel: Record<string, string> = {
  JEDNODENNI: 'Jednodenní', DVODENNI: 'Dvoudenní',
  DVODENNI_UZAVRENO: 'Dvoudenní (uzavřeno)', DVODENNI_BEZ_UBYTOVANI: 'Dvoudenní bez ubytování',
  AUTO: 'Automobil',
}

const variantOptions = computed(() => {
  const seen = new Set<string>()
  const opts: { value: string; label: string }[] = []
  for (const vc of variantConfigs.value) {
    if (seen.has(vc.variantCode)) continue
    seen.add(vc.variantCode)
    opts.push({ value: vc.variantCode, label: variantLabel[vc.variantCode] ?? vc.label })
  }
  return opts
})

const variantConfigs = ref<VariantConfig[]>([])

const variantDeadline = computed(() => {
  const map: Record<string, string> = {}
  for (const vc of variantConfigs.value) {
    if (vc.registrationDeadline) map[vc.variantCode] = vc.registrationDeadline
  }
  return map
})

async function loadVariantConfigs() {
  try {
    variantConfigs.value = await fetchAdminVariants(authHeaders())
  } catch { /* ok */ }
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

    <RaceControls
      :race-status="raceStatus"
      @start="handleRaceStart"
      @finish="handleRaceFinish"
      @reset="handleRaceReset"
    />

    <RegistrationFilters
      v-model:filter-variant="filterVariant"
      v-model:filter-status="filterStatus"
      v-model:filter-search="filterSearch"
      :variants="variantOptions"
      :selected-ids-size="selectedIds.size"
      :batch-processing="batchProcessing"
      @batch-action="batchAction"
      @clear-selection="selectedIds = new Set()"
    />

    <RegistrationStats :stats="stats" />

    <LoadingSpinner v-if="loading" />
    <p v-else-if="error" class="alert alert-error mb-4">{{ error }}</p>

    <div v-else-if="filtered.length === 0" class="py-12 text-center">
      <p class="text-section-title text-text-soft">Žádné přihlášky neodpovídají filtrům</p>
    </div>

    <RegistrationTable
      v-else
      :registrations="filtered"
      :selected-ids="selectedIds"
      :resending-id="resendingId"
      :reminding-id="remindingId"
      :variant-deadline="variantDeadline"
      :variant-label="variantLabel"
      :category-label="categoryLabel"
      @toggle-select="toggleSelect"
      @toggle-select-all="toggleSelectAll"
      @toggle-status="toggleStatus"
      @resend="handleResend"
      @send-reminder="handleSendReminder"
      @download-pdf="downloadRegPdf"
      @impersonate="handleImpersonate"
      @assign-start-number="handleAssignStartNumber"
      @select-reg="selectReg"
    />

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
          <div v-if="selected.status === 'CANCELLED'">
            <p class="text-label text-text-soft mb-1.5">Stav</p>
            <span class="badge !bg-red/10 !text-red">Stornováno</span>
            <p v-if="selected.refundAmount" class="text-body-sm text-text-soft mt-1">Vráceno: {{ selected.refundAmount }} Kč</p>
          </div>
          <div v-else>
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
            <p v-if="selected.vehicleStory" class="mt-2 text-body-sm text-primary italic">{{ selected.vehicleStory }}</p>
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

          <!-- Cancel -->
          <div v-if="selected.status !== 'CANCELLED'" class="pt-2">
            <button @click="handleCancel(selected)" class="btn-ghost btn-xs text-red hover:text-red">
              Stornovat přihlášku
            </button>
          </div>

          <!-- Actions -->
          <div class="flex flex-wrap gap-2 pt-1">
            <button @click="handleResend(selected)" :disabled="resendingId === selected.id"
              class="btn-secondary btn-xs">
              {{ resendingId === selected.id ? '…' : 'Poslat přihlašovací údaje' }}
            </button>
            <button v-if="selected.status !== 'PAID'" @click="handleSendReminder(selected)" :disabled="remindingId === selected.id"
              class="btn-secondary btn-xs">
              {{ remindingId === selected.id ? '…' : 'Poslat upomínku platby' }}
            </button>
            <button @click="downloadRegPdf(selected)" class="btn-secondary btn-xs">
              PDF k prezenci
            </button>
          </div>

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
          <!-- User lookup -->
          <div class="relative">
            <label class="text-label text-text-soft mb-1 block">Vybrat existujícího uživatele</label>
            <input
              v-model="userQuery"
              @input="onUserInput"
              placeholder="Hledat podle jména nebo emailu…"
              class="input-field w-full"
            />
            <button v-if="editForm.firstName && !userQuery" @click="clearUserSelection" class="absolute right-2 top-8 text-text-soft hover:text-text text-body-sm">
              Zrušit výběr
            </button>
            <div v-if="userSearchLoading" class="text-body-sm text-text-soft mt-1">Vyhledávám…</div>
            <ul v-else-if="userResults.length > 0" class="absolute z-20 left-0 right-0 mt-1 bg-bg border border-border rounded-lg shadow-lg max-h-48 overflow-y-auto">
              <li
                v-for="u in userResults"
                :key="u.id"
                @click="selectUser(u)"
                class="px-3 py-2 text-body-sm hover:bg-bg-alt cursor-pointer border-b border-border last:border-b-0"
              >
                <span class="text-text">{{ u.firstName }} {{ u.lastName }}</span>
                <span class="text-text-soft ml-2">{{ u.email }}</span>
                <span v-if="u.phone" class="text-text-soft ml-2">{{ u.phone }}</span>
              </li>
            </ul>
          </div>
          <hr class="border-border" />

          <div class="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-1">
            <div>
              <label class="text-label text-text-soft mb-1 block">Varianta</label>
              <select v-model="editForm.variant" class="input-field w-full">
                <option :value="null">—</option>
                <option v-for="opt in variantOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Název týmu</label>
              <input v-model="editForm.teamName" class="input-field w-full" />
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Telefon</label>
              <input v-model="editForm.phone" class="input-field w-full" />
            </div>
            <div>
              <label class="text-label text-text-soft mb-1 block">Kategorie</label>
              <select v-model="editForm.vehicleCategory" class="input-field w-full">
                <option :value="null">—</option>
                <option value="AUTO">Automobil</option>
                <option value="MOTO">Motocykl</option>
              </select>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-label text-text-soft mb-1 block">Značka</label>
                <input v-model="editForm.vehicleMake" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">SPZ</label>
                <input v-model="editForm.vehiclePlate" class="input-field w-full" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-label text-text-soft mb-1 block">Ročník</label>
                <input type="number" v-model.number="editForm.vehicleYear" class="input-field w-full" />
              </div>
              <div>
                <label class="text-label text-text-soft mb-1 block">Počet osob</label>
                <input type="number" v-model.number="editForm.crewCount" class="input-field w-full" min="1" max="10" />
              </div>
            </div>
            <p v-if="selected && editForm.crewCount > (selected.crewMembers?.length || 0)"
              class="text-body-sm text-warning">
              Počet osob byl zvýšen na {{ editForm.crewCount }}, ale je evidováno pouze {{ selected.crewMembers?.length || 0 }} členů posádky. Doplňte zbývající členy.
            </p>
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
              <RuiAnAutocomplete v-model="editForm.address" placeholder="Město, ulice" />
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
              <label class="text-label text-text-soft mb-1 block">Zajímavosti pro moderátora</label>
              <textarea v-model="editForm.vehicleStory" class="input-field w-full" rows="2" placeholder="Např. auto přijelo z Německa…"></textarea>
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
