<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchAdminVariants, updateAdminVariant, reopenAdminVariant, closeAdminVariantReopen, type VariantConfig } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()
const { show: showToast } = useToast()

if (!isAdmin.value) router.push('/admin/login')

const variants = ref<VariantConfig[]>([])
const loading = ref(true)
const saving = ref(false)
const reopening = ref<Record<number, boolean>>({})

const variantLabels: Record<string, string> = {
  JEDNODENNI: 'Jednodenní závod',
  DVODENNI_UZAVRENO: 'Dvoudenní závod – UZAVŘENO',
  DVODENNI_BEZ_UBYTOVANI: 'Dvoudenní závod bez ubytování',
}

const reopenOptions: { label: string; value: number | 'midnight' }[] = [
  { label: '30 minut', value: 30 },
  { label: '1 hodina', value: 60 },
  { label: '4 hodiny', value: 240 },
  { label: '8 hodin', value: 480 },
  { label: '12 hodin', value: 720 },
  { label: '24 hodin', value: 1440 },
  { label: 'do půlnoci', value: 'midnight' },
]

function isReopened(v: VariantConfig): boolean {
  return v.registrationReopenedUntil != null && new Date(v.registrationReopenedUntil) > new Date()
}

function reopenEndsAt(v: VariantConfig): string {
  if (!v.registrationReopenedUntil) return ''
  const d = new Date(v.registrationReopenedUntil)
  return d.toLocaleString('cs-CZ')
}

async function load() {
  try {
    variants.value = await fetchAdminVariants(authHeaders())
  } catch {
    showToast('Nepodařilo se načíst', 'error')
  } finally {
    loading.value = false
  }
}

async function save(v: VariantConfig) {
  saving.value = true
  try {
    const updated = await updateAdminVariant(v.id, {
      registrationDeadline: v.registrationDeadline || null,
      raceDate: v.raceDate || null,
    }, authHeaders())
    Object.assign(v, updated)
    showToast('Termíny uloženy', 'success')
  } catch {
    showToast('Ukládání selhalo', 'error')
  } finally {
    saving.value = false
  }
}

async function doReopen(v: VariantConfig, value: number | 'midnight') {
  reopening.value[v.id] = true
  try {
    const data = value === 'midnight' ? { untilMidnight: true } : { durationMinutes: value }
    const updated = await reopenAdminVariant(v.id, data, authHeaders())
    Object.assign(v, updated)
    showToast('Přihlášky znovu otevřeny', 'success')
  } catch {
    showToast('Nepodařilo se otevřít přihlášky', 'error')
  } finally {
    reopening.value[v.id] = false
  }
}

async function doCloseReopen(v: VariantConfig) {
  reopening.value[v.id] = true
  try {
    const updated = await closeAdminVariantReopen(v.id, authHeaders())
    Object.assign(v, updated)
    showToast('Znovuotevření zrušeno', 'success')
  } catch {
    showToast('Nepodařilo se zrušit znovuotevření', 'error')
  } finally {
    reopening.value[v.id] = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h1 class="text-page-title text-text">Nastavení závodu</h1>
      <p class="text-body-sm text-text-soft mt-1">Doplň termíny uzávěrky přihlášek a datum konání pro jednotlivé varianty.</p>
    </div>

    <LoadingSpinner v-if="loading" />

    <div v-else class="space-y-4">
      <div v-for="v in variants" :key="v.id"
        class="rounded-lg border border-border bg-surface p-4 sm:p-6">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h2 class="text-subsection text-text">{{ variantLabels[v.variantCode] ?? v.label }}</h2>
            <p class="text-meta text-text-soft font-mono">{{ v.variantCode }}</p>
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-label text-text-soft mb-1 block">Uzávěrka přihlášek</label>
            <input v-model="v.registrationDeadline" type="date" class="input-field w-full" />
          </div>
          <div>
            <label class="text-label text-text-soft mb-1 block">Datum závodu</label>
            <input v-model="v.raceDate" type="date" class="input-field w-full" />
          </div>
        </div>
        <div class="mt-4 flex justify-end">
          <button @click="save(v)" :disabled="saving" class="btn-primary btn-sm">
            {{ saving ? 'Ukládám…' : 'Uložit' }}
          </button>
        </div>
        <div class="mt-4 pt-4 border-t border-border">
          <div class="flex items-center justify-between flex-wrap gap-2">
            <div>
              <span class="text-label text-text-soft">Znovuotevření přihlášek</span>
              <p v-if="isReopened(v)" class="text-body-xs text-green-600 mt-0.5">
                Otevřeno do {{ reopenEndsAt(v) }}
              </p>
              <p v-else-if="v.registrationDeadline && new Date(v.registrationDeadline) < new Date()" class="text-body-xs text-orange-600 mt-0.5">
                Po uzávěrce – lze znovu otevřít
              </p>
              <p v-else class="text-body-xs text-text-soft mt-0.5">
                Uzávěrka ještě neproběhla
              </p>
            </div>
            <div class="flex items-center gap-2 flex-wrap">
              <template v-if="isReopened(v)">
                <button @click="doCloseReopen(v)" :disabled="reopening[v.id]" class="btn-secondary btn-sm">
                  {{ reopening[v.id] ? 'Zpracovávám…' : 'Zavřít' }}
                </button>
              </template>
              <template v-else>
                <button v-for="opt in reopenOptions" :key="opt.label"
                  @click="doReopen(v, opt.value)" :disabled="reopening[v.id]"
                  class="btn-secondary btn-sm">
                  {{ opt.label }}
                </button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
