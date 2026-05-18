<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchAdminVariants, updateAdminVariant, type VariantConfig } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()
const { show: showToast } = useToast()

if (!isAdmin.value) router.push('/admin/login')

const variants = ref<VariantConfig[]>([])
const loading = ref(true)
const saving = ref(false)

const variantLabels: Record<string, string> = {
  JEDNODENNI: 'Jednodenní závod',
  DVODENNI_UZAVRENO: 'Dvoudenní závod – UZAVŘENO',
  DVODENNI_BEZ_UBYTOVANI: 'Dvoudenní závod bez ubytování',
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
      </div>
    </div>
  </div>
</template>
