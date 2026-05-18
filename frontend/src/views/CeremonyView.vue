<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCeremonyCategories, type CeremonyCategory } from '@/api'

const route = useRoute()
const year = ref(Number(route.params.rok) || new Date().getFullYear())
const categories = ref<CeremonyCategory[]>([])
const loading = ref(true)
const revealed = ref<Set<number>>(new Set())
const animating = ref<number | null>(null)
const countdown = ref(0)
const error = ref<string | null>(null)

const currentCat = computed(() => {
  return categories.value.find(c => c.id === animating.value) ?? null
})

const allRevealed = computed(() => {
  return categories.value.length > 0 && categories.value.every(c => revealed.value.has(c.id))
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const data = await fetchCeremonyCategories(year.value)
    categories.value = data.categories
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function reveal(cat: CeremonyCategory) {
  if (revealed.value.has(cat.id)) return
  if (animating.value !== null) return
  animating.value = cat.id
  countdown.value = 3
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      revealed.value = new Set([...revealed.value, cat.id])
      animating.value = null
    }
  }, 800)
}

function resetAll() {
  revealed.value = new Set()
  animating.value = null
  countdown.value = 0
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen flex flex-col items-center justify-center px-4 py-8">
    <div class="w-full max-w-2xl">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-text tracking-tight">Vyhlášení výsledků {{ year }}</h1>
        <p class="text-body-sm text-text-soft mt-1">Kliknutím na kategorii odhalíte vítěze</p>
      </div>

      <div v-if="loading" class="text-center py-16">
        <div class="animate-spin w-10 h-10 border-4 border-primary border-t-transparent rounded-full mx-auto"></div>
      </div>

      <p v-else-if="error" class="alert alert-error">{{ error }}</p>

      <div v-else class="space-y-4">
        <button v-for="cat in categories" :key="cat.id"
          @click="reveal(cat)"
          :disabled="animating !== null && animating !== cat.id"
          class="w-full text-left transition-all duration-500"
          :class="revealed.has(cat.id)
            ? 'opacity-100 cursor-default'
            : 'hover:scale-[1.02] cursor-pointer'"
        >
          <!-- Animated fanfare card -->
          <div v-if="animating === cat.id"
            class="ceremony-card bg-gradient-to-br from-yellow-50 to-amber-100 dark:from-yellow-900/30 dark:to-amber-800/20 border-2 border-yellow-400 rounded-2xl p-8 text-center"
          >
            <div class="text-6xl mb-4 animate-bounce">{{ countdown > 0 ? countdown : '🎉' }}</div>
            <p v-if="countdown > 0" class="text-2xl font-bold text-yellow-700 dark:text-yellow-300 animate-pulse">
              {{ cat.name }}
            </p>
            <p v-if="countdown === 3" class="text-lg text-yellow-600 dark:text-yellow-400 mt-2">Připravte se…</p>
            <p v-if="countdown === 2" class="text-lg text-yellow-600 dark:text-yellow-400 mt-2">Bubnování… 🥁</p>
            <p v-if="countdown === 1" class="text-lg text-yellow-600 dark:text-yellow-400 mt-2">A vítězem se stává…</p>
          </div>

          <!-- Revealed card -->
          <div v-else-if="revealed.has(cat.id)"
            class="ceremony-card bg-gradient-to-br from-green-50 to-emerald-100 dark:from-green-900/30 dark:to-emerald-800/20 border-2 border-green-400 rounded-2xl p-8 text-center"
          >
            <div class="text-5xl mb-3">🏆</div>
            <h2 class="text-xl font-bold text-text mb-1">{{ cat.name }}</h2>
            <p v-if="cat.winnerName" class="text-3xl font-black text-green-700 dark:text-green-300 mt-2">
              {{ cat.winnerName }}
            </p>
            <p v-else class="text-lg text-text-soft">Bez vítěze</p>
            <div v-if="cat.winnerNumber" class="mt-2 text-lg text-text-soft">
              Startovní číslo #{{ cat.winnerNumber }}
            </div>
            <div v-if="cat.winnerPoints != null" class="text-sm text-text-soft mt-1">
              {{ cat.winnerPoints }} bodů
            </div>
          </div>

          <!-- Hidden card -->
          <div v-else
            class="ceremony-card bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 border-2 border-gray-300 dark:border-gray-600 rounded-2xl p-8 text-center hover:shadow-lg transition-shadow"
          >
            <div class="text-4xl mb-2 opacity-40">❓</div>
            <h2 class="text-lg font-bold text-text-muted">{{ cat.name }}</h2>
            <p class="text-sm text-text-soft mt-1">Klikni pro odhalení</p>
          </div>
        </button>

        <p v-if="categories.length === 0" class="text-center text-text-soft py-8">
          Žádné kategorie pro tento ročník.
        </p>

        <div v-if="allRevealed" class="text-center pt-6">
          <button @click="resetAll" class="btn-secondary">Resetovat</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ceremony-card {
  animation: fadeScale 0.4s ease-out;
}

@keyframes fadeScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
