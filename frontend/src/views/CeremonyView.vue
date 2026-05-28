<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCeremonyCategories, generateCeremonyPresentation, type CeremonyData } from '@/api'

interface Slide {
  id: string
  type: 'overall' | 'category'
  rank?: number
  title: string
  subtitle: string | null
  winnerName: string | null
  winnerNumber: number | null
  winnerPoints: number | null
  teamName: string | null
}

const route = useRoute()
const year = ref(Number(route.params.rok) || new Date().getFullYear())
const loading = ref(true)
const generating = ref(false)
const error = ref<string | null>(null)
const slides = ref<Slide[]>([])
const currentSlideIndex = ref(-1)
const showOverlay = ref(false)
const overlayPhase = ref<'countdown' | 'reveal'>('countdown')
const overlayCountdown = ref(3)
const overlaySlide = ref<Slide | null>(null)
const doneIds = ref<Set<string>>(new Set())
const isAdmin = ref(false)
let countdownInterval: ReturnType<typeof setInterval> | null = null
const pendingTimeouts: ReturnType<typeof setTimeout>[] = []

onUnmounted(() => {
  if (countdownInterval) clearInterval(countdownInterval)
  pendingTimeouts.forEach(clearTimeout)
})

const adminToken = typeof window !== 'undefined' ? localStorage.getItem('admin_token') : null
const adminRole = typeof window !== 'undefined' ? localStorage.getItem('admin_role') : null

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('admin_token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

function buildSlides(data: CeremonyData): Slide[] {
  const result: Slide[] = []
  for (const row of data.overall) {
    result.push({
      id: `overall-${row.rank}`,
      type: 'overall',
      rank: row.rank,
      title: row.rank === 1 ? '1. místo' : row.rank === 2 ? '2. místo' : '3. místo',
      subtitle: null,
      winnerName: row.teamName,
      winnerNumber: row.startNumber,
      winnerPoints: row.totalPoints,
      teamName: row.teamName,
    })
  }
  for (const cat of data.categories) {
    result.push({
      id: `cat-${cat.id}`,
      type: 'category',
      title: cat.name,
      subtitle: null,
      winnerName: cat.winnerName,
      winnerNumber: cat.winnerNumber,
      winnerPoints: cat.winnerPoints,
      teamName: cat.winnerTeam,
    })
  }
  return result
}

function startPresentation() {
  showOverlay.value = true
  currentSlideIndex.value = 0
  showSlide(0)
}

function showSlide(index: number) {
  if (index >= slides.value.length) {
    showOverlay.value = false
    return
  }
  if (countdownInterval) clearInterval(countdownInterval)
  overlaySlide.value = slides.value[index]
  overlayPhase.value = 'countdown'
  overlayCountdown.value = 3
  let count = 3
  countdownInterval = setInterval(() => {
    count--
    overlayCountdown.value = count
    if (count <= 0) {
      clearInterval(countdownInterval!)
      countdownInterval = null
      overlayPhase.value = 'reveal'
      doneIds.value = new Set([...doneIds.value, overlaySlide.value!.id])
    }
  }, 800)
}

function nextSlide() {
  const next = currentSlideIndex.value + 1
  if (next >= slides.value.length) {
    showOverlay.value = false
    currentSlideIndex.value = -1
    return
  }
  currentSlideIndex.value = next
  showSlide(next)
}

function resetAll() {
  doneIds.value = new Set()
  currentSlideIndex.value = -1
  showOverlay.value = false
  overlaySlide.value = null
}

async function regenerate() {
  generating.value = true
  try {
    const h = authHeaders()
    const result = await generateCeremonyPresentation(h)
    slides.value = buildSlides(result.data)
    doneIds.value = new Set()
    currentSlideIndex.value = -1
  } catch {
    error.value = 'Chyba při generování prezentace'
  } finally {
    generating.value = false
  }
}

async function load() {
  loading.value = true
  error.value = null
  isAdmin.value = !!(adminToken && adminRole?.includes('ADMIN'))
  try {
    const data = await fetchCeremonyCategories(year.value)
    slides.value = buildSlides(data)
  } catch {
    error.value = 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="relative min-h-screen">
    <!-- Fullscreen presentation overlay -->
    <div v-if="showOverlay"
      class="fixed inset-0 z-[99999] flex flex-col items-center justify-center cursor-pointer select-none ceremony-overlay"
      @click="overlayPhase === 'reveal' && nextSlide()"
    >
      <template v-if="overlayPhase === 'countdown'">
        <div class="text-[15vw] font-black ceremony-overlay-text ceremony-pulse">
          {{ overlayCountdown }}
        </div>
        <p class="text-xl ceremony-overlay-secondary mt-4 tracking-widest uppercase">
          {{ overlayCountdown === 3 ? 'Připravte se…' : overlayCountdown === 2 ? 'Bubnování…' : 'A vítězem se stává…' }}
        </p>
        <p class="text-lg ceremony-overlay-muted mt-2">{{ overlaySlide?.title }}</p>
      </template>
      <template v-else>
        <div class="text-7xl sm:text-9xl mb-6 ceremony-bounce">
          {{ overlaySlide?.rank === 1 ? '🏆' : overlaySlide?.rank === 2 ? '🥈' : overlaySlide?.rank === 3 ? '🥉' : '🏅' }}
        </div>
        <p class="text-2xl ceremony-overlay-secondary mb-2">{{ overlaySlide?.title }}</p>
        <p class="text-4xl sm:text-[8vw] font-black ceremony-overlay-text text-center px-4 leading-tight">
          {{ overlaySlide?.winnerName || 'Bez vítěze' }}
        </p>
        <div class="flex gap-6 mt-6 text-xl ceremony-overlay-tertiary">
          <span v-if="overlaySlide?.winnerNumber">#{{ overlaySlide.winnerNumber }}</span>
          <span v-if="overlaySlide?.winnerPoints != null">{{ overlaySlide.winnerPoints }} bodů</span>
          <span v-if="overlaySlide?.teamName && overlaySlide.teamName !== overlaySlide.winnerName" class="opacity-50">
            {{ overlaySlide.teamName }}
          </span>
        </div>
        <p class="mt-12 text-sm ceremony-overlay-muted">Kliknutím pokračovat</p>
      </template>
    </div>

    <!-- Page content -->
    <div class="flex flex-col items-center justify-center px-4 py-8">
      <div class="w-full max-w-3xl">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-text tracking-tight">Vyhlášení výsledků {{ year }}</h1>
          <p v-if="slides.length > 0" class="text-text-soft text-sm mt-1">{{ slides.length }} položek</p>
        </div>

        <div v-if="loading" class="text-center py-16">
          <div class="animate-spin w-10 h-10 border-4 border-primary border-t-transparent rounded-full mx-auto"></div>
        </div>

        <p v-else-if="error" class="alert alert-error">{{ error }}</p>

        <div v-else-if="slides.length === 0" class="text-center py-8">
          <p class="text-body text-text-soft">Žádné výsledky ani kategorie.</p>
        </div>

        <template v-else>
          <!-- Slides preview list -->
          <div class="space-y-3 mb-8">
            <div v-for="slide in slides" :key="slide.id"
              class="rounded-2xl border-2 p-5 text-center transition-all duration-300"
              :class="doneIds.has(slide.id)
                ? 'bg-gradient-to-br from-green-50 to-emerald-100 dark:from-green-900/30 dark:to-emerald-800/20 border-green-400'
                : 'bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 border-gray-300 dark:border-gray-600'"
            >
              <div class="text-4xl mb-2">
                {{ slide.type === 'overall'
                  ? (slide.rank === 1 ? '🏆' : slide.rank === 2 ? '🥈' : '🥉')
                  : '🏅' }}
              </div>
              <h2 class="text-xl font-bold text-text mb-1">{{ slide.title }}</h2>
              <div v-if="doneIds.has(slide.id)" class="mt-2">
                <p class="text-2xl font-black text-green-700 dark:text-green-300">{{ slide.winnerName || 'Bez vítěze' }}</p>
                <div class="flex justify-center gap-4 mt-1 text-sm text-text-soft">
                  <span v-if="slide.winnerNumber">#{{ slide.winnerNumber }}</span>
                  <span v-if="slide.winnerPoints != null">{{ slide.winnerPoints }} b.</span>
                </div>
              </div>
              <button v-else @click="currentSlideIndex = slides.indexOf(slide); showSlide(currentSlideIndex); showOverlay = true"
                :disabled="showOverlay"
                class="mt-4 btn-primary"
              >
                {{ showOverlay ? 'Probíhá…' : 'Vyhlásit' }}
              </button>
            </div>
          </div>

          <!-- Controls -->
          <div class="flex flex-wrap justify-center gap-3">
            <button v-if="doneIds.size > 0 && doneIds.size === slides.length" @click="resetAll" class="btn-secondary">
              Resetovat
            </button>
            <button @click="startPresentation" :disabled="showOverlay || slides.length === 0" class="btn-primary">
              Spustit celou prezentaci
            </button>
            <button v-if="isAdmin" @click="regenerate" :disabled="generating" class="btn-secondary">
              {{ generating ? 'Generuji…' : 'Vygenerovat prezentaci' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ceremony-overlay {
  background: rgba(0, 0, 0, 0.92);
}
.ceremony-overlay-text {
  color: #fff;
}
.ceremony-overlay-secondary {
  color: rgba(255, 255, 255, 0.6);
}
.ceremony-overlay-tertiary {
  color: rgba(255, 255, 255, 0.5);
}
.ceremony-overlay-muted {
  color: rgba(255, 255, 255, 0.3);
}
.ceremony-pulse {
  animation: ceremonyPulse 1s ease-in-out infinite;
}
.ceremony-bounce {
  animation: ceremonyBounce 1s ease-in-out infinite;
}
@keyframes ceremonyPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
@keyframes ceremonyBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}
</style>
