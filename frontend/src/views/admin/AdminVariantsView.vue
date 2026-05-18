<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchAdminVariants, createAdminVariant, updateAdminVariant, deleteAdminVariant, type VariantConfig } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()
const { show: showToast } = useToast()

if (!isAdmin.value) router.push('/admin/login')

const variants = ref<VariantConfig[]>([])
const loading = ref(true)
const editing = ref<Record<string, any>>({})
const saving = ref(false)

async function load() {
  try {
    variants.value = await fetchAdminVariants(authHeaders())
  } catch {
    showToast('Nepodařilo se načíst varianty', 'error')
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editing.value = { variantCode: '', label: '', registrationDeadline: '', raceDate: '', enabled: true, _isNew: true }
}

function startEdit(v: VariantConfig) {
  editing.value = { ...v, registrationDeadline: v.registrationDeadline ?? '', raceDate: v.raceDate ?? '', _isNew: false }
}

function cancelEdit() {
  editing.value = {}
}

async function save() {
  saving.value = true
  try {
    const data = {
      variantCode: editing.value.variantCode,
      label: editing.value.label,
      registrationDeadline: editing.value.registrationDeadline || null,
      raceDate: editing.value.raceDate || null,
      enabled: editing.value.enabled,
    }
    if (editing.value._isNew) {
      const created = await createAdminVariant(data, authHeaders())
      variants.value.push(created)
      showToast('Varianta vytvořena', 'success')
    } else {
      const updated = await updateAdminVariant(editing.value.id, data, authHeaders())
      const idx = variants.value.findIndex(v => v.id === editing.value.id)
      if (idx >= 0) variants.value[idx] = updated
      showToast('Varianta uložena', 'success')
    }
    editing.value = {}
  } catch {
    showToast('Ukládání selhalo', 'error')
  } finally {
    saving.value = false
  }
}

async function remove(v: VariantConfig) {
  if (!confirm(`Smazat variantu "${v.label}"?`)) return
  try {
    await deleteAdminVariant(v.id, authHeaders())
    variants.value = variants.value.filter(x => x.id !== v.id)
    showToast('Varianta smazána', 'success')
  } catch {
    showToast('Smazání selhalo', 'error')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <h1 class="text-page-title text-text">Varianty závodu</h1>
      <button @click="startCreate" class="btn-primary btn-sm">+ Přidat variantu</button>
    </div>

    <LoadingSpinner v-if="loading" />

    <div v-else-if="variants.length === 0" class="text-body-sm text-text-soft py-8 text-center">
      Žádné varianty nenastaveny.
    </div>

    <div v-else class="space-y-3">
      <div v-for="v in variants" :key="v.id"
        class="rounded-lg border border-border bg-surface p-4 flex items-center justify-between gap-4">
        <div class="flex-1 min-w-0">
          <div class="font-medium text-text">{{ v.label }}</div>
          <div class="text-meta text-text-soft space-x-4">
            <span>Kód: <span class="font-mono">{{ v.variantCode }}</span></span>
            <span v-if="v.registrationDeadline">Přihlášky do: <span class="font-mono">{{ v.registrationDeadline }}</span></span>
            <span v-if="v.raceDate">Datum závodu: <span class="font-mono">{{ v.raceDate }}</span></span>
            <span :class="v.enabled ? 'text-success' : 'text-red'">{{ v.enabled ? 'Aktivní' : 'Neaktivní' }}</span>
          </div>
        </div>
        <div class="flex gap-2 shrink-0">
          <button @click="startEdit(v)" class="btn-ghost btn-xs">Upravit</button>
          <button @click="remove(v)" class="btn-ghost btn-xs text-red hover:text-red">Smazat</button>
        </div>
      </div>
    </div>

    <!-- Edit / Create modal -->
    <div v-if="editing._isNew !== undefined" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="cancelEdit">
      <div class="mx-4 w-full max-w-md rounded-xl border border-border bg-surface p-6 shadow-lg">
        <h2 class="text-subsection text-text mb-4">{{ editing._isNew ? 'Nová varianta' : 'Upravit variantu' }}</h2>
        <div class="space-y-3">
          <div>
            <label class="text-label text-text-soft mb-1 block">Kód varianty</label>
            <input v-model="editing.variantCode" class="input-field w-full" placeholder="např. JEDNODENNI" :readonly="!editing._isNew" />
          </div>
          <div>
            <label class="text-label text-text-soft mb-1 block">Název</label>
            <input v-model="editing.label" class="input-field w-full" placeholder="např. Jednodenní závod" />
          </div>
          <div>
            <label class="text-label text-text-soft mb-1 block">Uzávěrka přihlášek</label>
            <input v-model="editing.registrationDeadline" type="date" class="input-field w-full" />
          </div>
          <div>
            <label class="text-label text-text-soft mb-1 block">Datum závodu</label>
            <input v-model="editing.raceDate" type="date" class="input-field w-full" />
          </div>
          <label class="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" v-model="editing.enabled" class="accent-primary" />
            <span class="text-body-sm text-text-muted">Aktivní</span>
          </label>
        </div>
        <div class="flex gap-2 mt-5">
          <button @click="save" :disabled="saving" class="btn-primary flex-1">
            {{ saving ? 'Ukládám…' : 'Uložit' }}
          </button>
          <button @click="cancelEdit" class="btn-ghost">Zrušit</button>
        </div>
      </div>
    </div>
  </div>
</template>
