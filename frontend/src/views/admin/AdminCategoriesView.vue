<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { fetchAdminCategories, createAdminCategory, updateAdminCategory, deleteAdminCategory, computeCategoryWinners, type RaceCategory } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders } = useAuth()
const { show: showToast } = useToast()

if (!isAdmin.value) router.push('/admin/login')

const categories = ref<RaceCategory[]>([])
const loading = ref(true)
const saving = ref(false)
const computing = ref(false)
const showForm = ref(false)
const editing = ref<RaceCategory | null>(null)
const form = ref({
  name: '',
  code: '',
  variant: '',
  determination: 'RANKING_TOP',
  sortOrder: 0,
})

const determinationLabels: Record<string, string> = {
  RANKING_TOP: 'Nejvyšší počet bodů',
  RANKING_LAST: 'Poslední místo',
  OLDEST_VEHICLE: 'Nejstarší vozidlo',
  YOUNGEST_DRIVER: 'Nejmladší účastník',
  OLDEST_DRIVER: 'Nejstarší účastník',
}

const variantLabels: Record<string, string> = {
  JEDNODENNI: 'Jednodenní',
  DVODENNI_UZAVRENO: 'Dvoudenní (UZAVŘENO)',
  DVODENNI_BEZ_UBYTOVANI: 'Dvoudenní bez ubytování',
}

async function load() {
  try {
    categories.value = await fetchAdminCategories(authHeaders())
  } catch {
    showToast('Nepodařilo se načíst kategorie', 'error')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = {
    name: '',
    code: '',
    variant: '',
    determination: 'RANKING_TOP',
    sortOrder: categories.value.length,
  }
  editing.value = null
  showForm.value = true
}

function startEdit(cat: RaceCategory) {
  form.value = {
    name: cat.name,
    code: cat.code ?? '',
    variant: cat.variant ?? '',
    determination: cat.determination,
    sortOrder: cat.sortOrder,
  }
  editing.value = cat
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editing.value = null
}

async function handleSave() {
  saving.value = true
  try {
    const data = {
      name: form.value.name,
      code: form.value.code || null,
      variant: form.value.variant || null,
      determination: form.value.determination,
      sortOrder: form.value.sortOrder,
    }
    if (editing.value) {
      const updated = await updateAdminCategory(editing.value.id, data, authHeaders())
      const idx = categories.value.findIndex(c => c.id === editing.value!.id)
      if (idx >= 0) categories.value[idx] = updated
      showToast('Kategorie uložena', 'success')
    } else {
      const created = await createAdminCategory(data, authHeaders())
      categories.value.push(created)
      categories.value.sort((a, b) => a.sortOrder - b.sortOrder)
      showToast('Kategorie vytvořena', 'success')
    }
    showForm.value = false
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Chyba při ukládání', 'error')
  } finally {
    saving.value = false
  }
}

async function handleDelete(cat: RaceCategory) {
  if (!confirm(`Opravdu smazat kategorii "${cat.name}"?`)) return
  try {
    await deleteAdminCategory(cat.id, authHeaders())
    categories.value = categories.value.filter(c => c.id !== cat.id)
    showToast('Kategorie smazána', 'info')
  } catch {
    showToast('Chyba při mazání', 'error')
  }
}

async function handleCompute() {
  computing.value = true
  try {
    const result = await computeCategoryWinners(authHeaders())
    showToast(`Spočítáno ${result.count} vítězů`, 'success')
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : 'Chyba při výpočtu', 'error')
  } finally {
    computing.value = false
  }
}

function editWinner(cat: RaceCategory) {
  const n = prompt('Jméno vítěze (nechte prázdné pro smazání):', cat.winnerName ?? '')
  if (n === null) return
  updateAdminCategory(cat.id, {
    winnerName: n || null,
    winnerTeam: cat.winnerTeam,
    winnerNumber: cat.winnerNumber,
    winnerPoints: cat.winnerPoints,
  }, authHeaders()).then(updated => {
    const idx = categories.value.findIndex(c => c.id === cat.id)
    if (idx >= 0) categories.value[idx] = updated
    showToast(n ? 'Vítěz nastaven' : 'Vítěz smazán', 'success')
  }).catch(() => showToast('Chyba při ukládání', 'error'))
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-page-title text-text">Kategorie</h1>
        <p class="text-body-sm text-text-soft mt-1">Závodní kategorie pro vyhlášení výsledků.</p>
      </div>
      <div class="flex gap-2">
        <button @click="handleCompute" :disabled="computing" class="btn-secondary btn-sm">
          {{ computing ? 'Počítám…' : 'Spočítat vítěze' }}
        </button>
        <button @click="resetForm" class="btn-primary btn-sm">Přidat kategorii</button>
      </div>
    </div>

    <LoadingSpinner v-if="loading" />

    <!-- Add/Edit form -->
    <div v-if="showForm" class="card !p-6 mb-6">
      <h2 class="text-subsection text-text mb-4">{{ editing ? 'Upravit kategorii' : 'Nová kategorie' }}</h2>
      <form @submit.prevent="handleSave" class="space-y-4">
        <div>
          <label class="input-label">Název</label>
          <input v-model="form.name" required class="input-field w-full" placeholder="Např. Hlavní kategorie Auto do roku 1945" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="input-label">Kód (volitelný filtr)</label>
            <input v-model="form.code" class="input-field w-full" placeholder="AUTO, MOTO…" />
          </div>
          <div>
            <label class="input-label">Varianta (volitelný filtr)</label>
            <select v-model="form.variant" class="input-field w-full">
              <option value="">– Všechny varianty –</option>
              <option v-for="(l, k) in variantLabels" :key="k" :value="k">{{ l }}</option>
            </select>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="input-label">Způsob určení vítěze</label>
            <select v-model="form.determination" class="input-field w-full">
              <option v-for="(l, k) in determinationLabels" :key="k" :value="k">{{ l }}</option>
            </select>
          </div>
          <div>
            <label class="input-label">Pořadí</label>
            <input v-model.number="form.sortOrder" type="number" class="input-field w-full" />
          </div>
        </div>
        <div class="flex gap-2 pt-2">
          <button type="submit" :disabled="saving" class="btn-primary flex-1">
            {{ saving ? 'Ukládám…' : editing ? 'Uložit' : 'Vytvořit' }}
          </button>
          <button type="button" @click="cancelForm" class="btn-ghost">Zrušit</button>
        </div>
      </form>
    </div>

    <!-- Category list -->
    <div v-else class="space-y-3">
      <div v-for="cat in categories" :key="cat.id"
        class="card !p-4 flex items-center justify-between gap-4">
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2">
            <span class="text-meta text-text-soft font-mono">{{ cat.sortOrder }}.</span>
            <h3 class="text-body font-semibold text-text truncate">{{ cat.name }}</h3>
          </div>
          <div class="flex flex-wrap gap-x-4 gap-y-1 mt-1 text-meta text-text-soft">
            <span v-if="cat.code">Filtr: {{ cat.code }}</span>
            <span v-if="cat.variant">Varianta: {{ variantLabels[cat.variant] ?? cat.variant }}</span>
            <span>Určení: {{ determinationLabels[cat.determination] ?? cat.determination }}</span>
          </div>
          <div v-if="cat.winnerName" class="mt-2 flex items-center gap-3 text-body-sm">
            <span class="inline-flex items-center gap-1 text-success font-medium">
              <span>🏆</span> {{ cat.winnerName }}
            </span>
            <span v-if="cat.winnerNumber" class="text-text-soft">#{{ cat.winnerNumber }}</span>
            <span v-if="cat.winnerPoints != null" class="text-text-soft">{{ cat.winnerPoints }} b.</span>
          </div>
          <p v-else class="text-meta text-text-soft mt-1">Zatím bez vítěze</p>
        </div>
        <div class="flex gap-2 shrink-0">
          <button @click="editWinner(cat)" class="btn-ghost btn-xs" title="Upravit vítěze">🏆</button>
          <button @click="startEdit(cat)" class="btn-ghost btn-xs">Upravit</button>
          <button @click="handleDelete(cat)" class="btn-ghost btn-xs text-red">Smazat</button>
        </div>
      </div>
      <p v-if="categories.length === 0" class="text-body text-text-soft text-center py-8">
        Zatím žádné kategorie. Přidejte první.
      </p>
    </div>
  </div>
</template>
