<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl, lookupRacerByStartNumber, submitScore } from '@/api'

const { hasAdmin, hasJudge, hasRacer, name, authHeaders } = useAuth()

const role = computed(() => {
  if (hasAdmin.value) return 'admin'
  if (hasJudge.value) return 'judge'
  if (hasRacer.value) return 'racer'
  return null
})

const loading = ref(true)
const stats = ref<any>(null)
const registrations = ref<any[]>([])
const checkpoints = ref<any[]>([])
const racerRegistration = ref<any>(null)
const racerStanding = ref<any>(null)
const schedule = ref<any[]>([])
const checkpointProgress = ref<any>(null)
const error = ref<string | null>(null)

const categoryLabel: Record<string, string> = {
  MOTOCYKL: 'Motocykl', OSOBNI: 'Osobní', CLASSIC: 'Historické', NAKLADNI: 'Nákladní',
}

onMounted(async () => {
  loading.value = true
  try {
    const h = authHeaders()
    if (role.value === 'admin') {
      const [statsRes, regRes, cpRes] = await Promise.all([
        fetch(`${apiBaseUrl}/api/admin/registrations/stats`, { headers: h }),
        fetch(`${apiBaseUrl}/api/admin/registrations`, { headers: h }),
        fetch(`${apiBaseUrl}/api/admin/checkpoints/progress`, { headers: h }),
      ])
      if (statsRes.ok) stats.value = await statsRes.json()
      if (regRes.ok) {
        const all = await regRes.json()
        registrations.value = all.slice(-5).reverse()
      }
      if (cpRes.ok) checkpointProgress.value = await cpRes.json()
    }
    if (role.value === 'judge') {
      const res = await fetch(`${apiBaseUrl}/api/racer/checkpoints`, { headers: h })
      if (res.ok) {
        const all = await res.json()
        checkpoints.value = all.filter((cp: any) =>
          cp.volunteers?.some((v: string) => v === name.value)
        )
        if (checkpoints.value.length > 0) {
          quickCheckpointId.value = checkpoints.value[0].id
        }
      }
    }
    if (role.value === 'racer') {
      const [regRes, scheduleRes] = await Promise.all([
        fetch(`${apiBaseUrl}/api/racer/registration`, { headers: h }),
        fetch(`${apiBaseUrl}/api/racer/schedule`, { headers: h }),
      ])
      if (regRes.ok) {
        const body = await regRes.json()
        if (!Array.isArray(body) && !body.error) {
          racerRegistration.value = body
          racerStanding.value = body
        }
      }
      if (scheduleRes.ok) {
        const all = await scheduleRes.json()
        schedule.value = all.slice(0, 3)
      }
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
})

const quickStartNumber = ref<number | null>(null)
const quickCheckpointId = ref<number | null>(null)
const quickPoints = ref<number | null>(null)
const quickRacer = ref<any>(null)
const quickSaving = ref(false)
const quickError = ref<string | null>(null)
const quickSuccess = ref(false)

async function quickLookup() {
  if (!quickStartNumber.value) return
  quickError.value = null
  quickRacer.value = null
  quickSuccess.value = false
  try {
    const r = await lookupRacerByStartNumber(quickStartNumber.value)
    if (r) {
      quickRacer.value = r
    } else {
      quickError.value = 'Závodník nenalezen'
    }
  } catch (e) {
    quickError.value = e instanceof Error ? e.message : 'Chyba'
  }
}

async function quickSubmit() {
  if (!quickRacer.value || quickPoints.value == null || !quickCheckpointId.value) return
  quickSaving.value = true
  quickError.value = null
  quickSuccess.value = false
  try {
    await submitScore({
      racerRegistrationId: quickRacer.value.id,
      checkpointId: quickCheckpointId.value,
      points: quickPoints.value,
    }, authHeaders())
    quickSuccess.value = true
    quickStartNumber.value = null
    quickRacer.value = null
    quickPoints.value = null
  } catch (e) {
    quickError.value = e instanceof Error ? e.message : 'Chyba'
  } finally {
    quickSaving.value = false
  }
}
</script>

<template>
  <LoadingSpinner v-if="loading" />

  <div v-else-if="error" class="alert alert-error">{{ error }}</div>

  <!-- Admin dashboard -->
  <template v-else-if="role === 'admin'">
    <div class="mb-6">
      <h1 class="text-page-title text-text">Vítej zpět, {{ name }}</h1>
      <p class="text-body-sm text-text-soft">Správa Novobydžovského čtverce</p>
    </div>

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
        <p class="text-kpi" :class="stats.paid === stats.totalCrews ? 'text-success' : 'text-red'">{{ stats.paid }}</p>
        <p class="text-meta text-text-soft mt-0.5">Zaplaceno</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.arrived }}</p>
        <p class="text-meta text-text-soft mt-0.5">Přijelo</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.approved }}</p>
        <p class="text-meta text-text-soft mt-0.5">Schváleno</p>
      </div>
      <div class="card !p-4 text-center">
        <p class="text-kpi text-text-muted">{{ stats.firstTimers }}</p>
        <p class="text-meta text-text-soft mt-0.5">Nováčků</p>
      </div>
    </div>

    <div v-if="checkpointProgress" class="mb-8">
      <h2 class="text-subsection text-text mb-4">Stav bodování stanovišť</h2>
      <div class="flex gap-3 mb-4">
        <div class="card !p-3 text-center flex-1">
          <p class="text-kpi text-primary">{{ checkpointProgress.overall.total }}</p>
          <p class="text-meta text-text-soft mt-0.5">Celkem</p>
        </div>
        <div class="card !p-3 text-center flex-1">
          <p class="text-kpi text-success">{{ checkpointProgress.overall.complete }}</p>
          <p class="text-meta text-text-soft mt-0.5">Hotovo</p>
        </div>
        <div class="card !p-3 text-center flex-1">
          <p class="text-kpi text-red">{{ checkpointProgress.overall.incomplete }}</p>
          <p class="text-meta text-text-soft mt-0.5">Zbývá</p>
        </div>
      </div>
      <div class="overflow-x-auto rounded-xl border border-border">
        <table class="w-full">
          <thead class="table-header">
            <tr>
              <th class="w-8 text-center">#</th>
              <th>Stanoviště</th>
              <th class="text-center">Oskórováno</th>
              <th class="text-center w-24">Stav</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="cp in checkpointProgress.checkpoints" :key="cp.id" class="table-row">
              <td class="text-center font-mono font-bold text-primary">{{ cp.sortOrder }}</td>
              <td>
                <div class="font-medium text-text">{{ cp.name }}</div>
              </td>
              <td class="text-center">
                <span class="text-body-sm text-text-muted">{{ cp.scoredCount }} / {{ cp.totalRacers }}</span>
              </td>
              <td class="text-center">
                <span v-if="cp.complete" class="badge !bg-success/10 !text-success">Hotovo</span>
                <span v-else class="badge !bg-warning/10 !text-warning">Boduje se</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="registrations.length > 0" class="mb-8">
      <h2 class="text-subsection text-text mb-4">Poslední přihlášky</h2>
      <div class="overflow-x-auto rounded-xl border border-border">
        <table class="w-full">
          <thead class="table-header">
            <tr>
              <th class="w-8 text-center">#</th>
              <th>Posádka</th>
              <th>Kategorie</th>
              <th>Stav</th>
              <th class="hidden sm:table-cell">Schválení</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in registrations" :key="r.id" class="table-row">
              <td class="text-center font-mono font-bold text-primary">{{ r.startNumber }}</td>
              <td>
                <div class="font-medium text-text">{{ r.teamName }}</div>
                <div class="text-meta text-text-soft">{{ r.email }}</div>
              </td>
              <td>
                <span class="text-body-sm text-text-muted">{{ categoryLabel[r.vehicleCategory] ?? r.vehicleCategory }}</span>
              </td>
              <td>
                <span class="badge" :class="r.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'">
                  {{ r.status === 'PAID' ? 'Zaplaceno' : 'Čeká' }}
                </span>
              </td>
              <td class="hidden sm:table-cell">
                <span v-if="r.approved" class="badge !bg-success/10 !text-success">Schváleno</span>
                <span v-else class="badge !bg-warning/10 !text-warning">Čeká</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="flex flex-wrap gap-3">
      <RouterLink to="/admin/prihlaseni" class="btn-primary no-underline">Všechny přihlášky</RouterLink>
      <RouterLink to="/admin/stanoviste" class="btn-secondary no-underline">Stanoviště</RouterLink>
      <RouterLink to="/admin/komunikace" class="btn-secondary no-underline">Komunikace</RouterLink>
      <RouterLink to="/admin/uzivatele" class="btn-secondary no-underline">Uživatelé</RouterLink>
      <RouterLink to="/admin/varianty" class="btn-secondary no-underline">Varianty</RouterLink>
    </div>
  </template>

  <!-- Judge dashboard -->
  <template v-else-if="role === 'judge'">
    <div class="mb-6">
      <h1 class="text-page-title text-text">Vítej, {{ name }}</h1>
      <p class="text-body-sm text-text-soft">Bodovací rozhraní komisaře</p>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Rychlé bodování</h2>
          <div class="space-y-4">
            <div>
              <label class="input-label">Startovní číslo</label>
              <input v-model.number="quickStartNumber" type="number" min="1" placeholder="např. 7"
                class="input-field text-center text-xl font-bold" @keyup.enter="quickLookup" />
            </div>
            <div v-if="quickRacer" class="rounded-lg bg-success/5 border border-success/30 p-3">
              <p class="font-medium text-text">{{ quickRacer.teamName }}</p>
              <p class="text-body-sm text-text-muted">{{ quickRacer.vehiclePlate }} · #{{ quickRacer.startNumber }}</p>
            </div>
            <div v-if="quickRacer">
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="input-label">Stanoviště</label>
                  <select v-model.number="quickCheckpointId" class="input-field">
                    <option v-for="cp in checkpoints" :key="cp.id" :value="cp.id">
                      {{ cp.sortOrder }}. {{ cp.name }}
                    </option>
                  </select>
                </div>
                <div>
                  <label class="input-label">Body</label>
                  <input v-model.number="quickPoints" type="number" min="0" class="input-field text-center font-bold" />
                </div>
              </div>
              <button @click="quickSubmit" :disabled="quickSaving || quickPoints == null"
                class="btn-primary w-full mt-3"
              >{{ quickSaving ? 'Odesílám…' : 'Odeslat' }}</button>
              <p v-if="quickError" class="text-body-sm text-error mt-2">{{ quickError }}</p>
              <p v-if="quickSuccess" class="text-body-sm text-success mt-2">Body zapsány ✓</p>
            </div>
            <button v-if="!quickRacer" @click="quickLookup" class="btn-primary w-full">Vyhledat</button>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Tvoje stanoviště</h2>
          <div v-if="checkpoints.length === 0" class="text-body-sm text-text-soft text-center py-4">
            Nejsi přiřazen k žádnému stanovišti
          </div>
          <div v-else class="space-y-2">
            <div v-for="cp in checkpoints" :key="cp.id"
              class="flex items-center gap-3 rounded-lg border border-border bg-bg-alt p-3"
            >
              <span class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-surface-strong text-meta font-semibold text-text-muted">{{ cp.sortOrder }}</span>
              <div>
                <p class="text-body-sm font-medium text-text">{{ cp.name }}</p>
                <p v-if="cp.maxPoints != null" class="text-meta text-text-soft">max {{ cp.maxPoints }} bodů</p>
              </div>
            </div>
          </div>
        </div>
        <RouterLink to="/komisari" class="btn-primary no-underline w-full text-center">
          Bodovací formulář
        </RouterLink>
      </div>
    </div>
  </template>

  <!-- Racer dashboard -->
  <template v-else-if="role === 'racer'">
    <div class="mb-6">
      <h1 class="text-page-title text-text">Vítej, {{ name }}</h1>
      <p class="text-body-sm text-text-soft">Tvůj závodní profil</p>
    </div>

    <div v-if="racerRegistration" class="grid gap-6 lg:grid-cols-2">
      <div class="space-y-4">
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Tvá přihláška</h2>
          <div class="space-y-3">
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Startovní číslo</span>
              <span class="text-kpi text-primary">#{{ racerRegistration.startNumber }}</span>
            </div>
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Posádka</span>
              <span class="text-body font-medium text-text">{{ racerRegistration.teamName }}</span>
            </div>
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Kategorie</span>
              <span class="text-body-sm text-text">{{ categoryLabel[racerRegistration.vehicleCategory] ?? racerRegistration.vehicleCategory }}</span>
            </div>
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Stav</span>
              <span class="badge" :class="racerRegistration.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'">
                {{ racerRegistration.status === 'PAID' ? 'Zaplaceno' : 'Čeká na platbu' }}
              </span>
            </div>
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Schválení</span>
              <span v-if="racerRegistration.approved" class="badge !bg-success/10 !text-success">Schváleno</span>
              <span v-else class="badge !bg-warning/10 !text-warning">Čeká na schválení</span>
            </div>
            <div class="flex items-center justify-between py-1">
              <span class="text-text-soft text-body-sm">Startovné</span>
              <span class="text-body font-semibold text-primary">{{ racerRegistration.startFee }} Kč</span>
            </div>
          </div>
        </div>

        <div v-if="racerStanding" class="card">
          <h2 class="text-subsection text-text mb-4">Průběžné pořadí</h2>
          <div class="grid grid-cols-3 gap-3 text-center">
            <div>
              <p class="text-kpi text-primary">{{ racerStanding.rank ?? '…' }}<span class="text-body text-text-soft">/{{ racerStanding.totalRacers ?? '…' }}</span></p>
              <p class="text-meta text-text-soft">Pořadí</p>
            </div>
            <div>
              <p class="text-kpi text-text">{{ racerStanding.totalPoints ?? 0 }}</p>
              <p class="text-meta text-text-soft">Body</p>
            </div>
            <div>
              <p class="text-kpi text-text-muted">{{ racerStanding.scores?.length ?? 0 }}</p>
              <p class="text-meta text-text-soft">Jízdy</p>
            </div>
          </div>
        </div>
      </div>

      <div class="space-y-4">
        <div v-if="schedule.length > 0" class="card">
          <h2 class="text-subsection text-text mb-4">Nejbližší program</h2>
          <div class="space-y-3">
            <div v-for="(item, i) in schedule" :key="i"
              class="flex items-center gap-3"
            >
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold">{{ item.time }}</span>
              <span class="text-body-sm text-text-muted">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <h2 class="text-subsection text-text mb-4">Rychlé odkazy</h2>
          <div class="space-y-2">
            <RouterLink to="/zavodnik/itinerar" class="flex items-center gap-3 rounded-lg border border-border bg-bg-alt p-3 no-underline transition hover:border-primary">
              <span class="text-body-sm font-medium text-text">Itinerář</span>
              <span class="text-meta text-text-soft ml-auto">→</span>
            </RouterLink>
            <RouterLink to="/zavodnik/mapa" class="flex items-center gap-3 rounded-lg border border-border bg-bg-alt p-3 no-underline transition hover:border-primary">
              <span class="text-body-sm font-medium text-text">Mapa trasy</span>
              <span class="text-meta text-text-soft ml-auto">→</span>
            </RouterLink>
            <RouterLink to="/zavodnik/stav" class="flex items-center gap-3 rounded-lg border border-border bg-bg-alt p-3 no-underline transition hover:border-primary">
              <span class="text-body-sm font-medium text-text">Můj stav</span>
              <span class="text-meta text-text-soft ml-auto">→</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card text-center">
      <p class="text-subsection text-warning">Nejste přihlášen k závodu</p>
      <p class="text-body text-text-muted mt-2">Zaregistrujte se přes přihlašovací formulář.</p>
      <RouterLink to="/registrace" class="btn-primary mt-4 no-underline">Přihlásit se</RouterLink>
    </div>
  </template>
</template>
