<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCeremonyCategories, fetchResults, type CeremonyCategory, type ResultRow } from '@/api'

const route = useRoute()
const year = ref(Number(route.params.rok) || new Date().getFullYear())
const categories = ref<CeremonyCategory[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

interface CeremonyItem {
  id: string
  name: string
  winnerName: string | null
  winnerNumber: number | null
  winnerPoints: number | null
  rank?: number
}

const items = ref<CeremonyItem[]>([])
const done = ref<Set<string>>(new Set())

// Fullscreen ceremony state
const fullscreen = ref(false)
const fsCategory = ref<string>('')
const fsCountdown = ref(0)
const fsName = ref<string | null>(null)
const fsNumber = ref<number | null>(null)
const fsPoints = ref<number | null>(null)
const fsPhase = ref<'idle' | 'countdown' | 'reveal'>('idle')
let fsTimer: ReturnType<typeof setInterval> | null = null

async function load() {
  loading.value = true
  error.value = null
  try {
    const [catData, resData] = await Promise.all([
      fetchCeremonyCategories(year.value),
      fetchResults(year.value).catch(() => null),
    ])
    categories.value = catData.categories

    const all: CeremonyItem[] = []

    // Top 3 from overall results
    if (resData?.results) {
      const sorted = [...resData.results].sort((a, b) => a.rank - b.rank).slice(0, 3)
      for (const r of sorted) {
        all.push({
          id: `rank-${r.rank}`,
          name: r.rank === 1 ? '1. místo' : r.rank === 2 ? '2. místo' : '3. místo',
          winnerName: r.teamName,
          winnerNumber: r.startNumber,
          winnerPoints: r.totalPoints,
          rank: r.rank,
        })
      }
    }

    // Categories
    for (const c of catData.categories) {
      all.push({
        id: `cat-${c.id}`,
        name: c.name,
        winnerName: c.winnerName,
        winnerNumber: c.winnerNumber,
        winnerPoints: c.winnerPoints,
      })
    }

    items.value = all
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function announce(item: CeremonyItem) {
  if (fsTimer) return
  fullscreen.value = true
  fsCategory.value = item.name
  fsName.value = item.winnerName
  fsNumber.value = item.winnerNumber
  fsPoints.value = item.winnerPoints
  fsPhase.value = 'countdown'
  fsCountdown.value = 3

  fsTimer = setInterval(() => {
    fsCountdown.value--
    if (fsCountdown.value <= 0) {
      if (fsTimer) clearInterval(fsTimer)
      fsTimer = null
      fsPhase.value = 'reveal'
      done.value = new Set([...done.value, item.id])
    }
  }, 800)
}

function closeFullscreen() {
  if (fsTimer) {
    clearInterval(fsTimer)
    fsTimer = null
  }
  fullscreen.value = false
  fsPhase.value = 'idle'
}

function resetAll() {
  closeFullscreen()
  done.value = new Set()
}

const podiumLabel: Record<number, string> = { 1: '🏆', 2: '🥈', 3: '🥉' }

onMounted(load)
</script>

<template>
  <!-- Fullscreen overlay -->
  <Teleport to="body">
    <div v-if="fullscreen"
      class="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-black/90 transition-all duration-500"
      @click="fsPhase === 'reveal' && closeFullscreen()"
    >
      <!-- Countdown phase -->
      <template v-if="fsPhase === 'countdown'">
        <div class="text-[15vw] font-black text-white animate-pulse select-none">
          {{ fsCountdown }}
        </div>
        <p class="text-xl sm:text-2xl text-white/60 mt-4 tracking-widest uppercase">
          {{ fsCountdown === 3 ? 'Připravte se…' : fsCountdown === 2 ? 'Bubnování… 🥁' : 'A vítězem se stává…' }}
        </p>
        <p class="text-lg sm:text-xl text-white/40 mt-2">{{ fsCategory }}</p>
      </template>

      <!-- Reveal phase -->
      <template v-else>
        <div class="text-8xl sm:text-[10vw] mb-6 animate-bounce">🏆</div>
        <h2 class="text-xl sm:text-3xl text-white/60 mb-2">{{ fsCategory }}</h2>
        <p v-if="fsName" class="text-5xl sm:text-[8vw] font-black text-white text-center px-4 leading-tight">
          {{ fsName }}
        </p>
        <p v-else class="text-3xl text-white/40">Bez vítěze</p>
        <div class="flex gap-6 mt-6 text-xl sm:text-2xl text-white/50">
          <span v-if="fsNumber">#{{ fsNumber }}</span>
          <span v-if="fsPoints != null">{{ fsPoints }} bodů</span>
        </div>
        <p class="mt-12 text-sm text-white/30">Kliknutím zavřete</p>
      </template>
    </div>
  </Teleport>

  <!-- Page content -->
  <div class="min-h-screen flex flex-col items-center justify-center px-4 py-8">
    <div class="w-full max-w-2xl">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-text tracking-tight">Vyhlášení výsledků {{ year }}</h1>
        <p class="text-body-sm text-text-soft mt-1">Kliknutím na tlačítko Vyhlásit spustíte ceremoniál</p>
      </div>

      <div v-if="loading" class="text-center py-16">
        <div class="animate-spin w-10 h-10 border-4 border-primary border-t-transparent rounded-full mx-auto"></div>
      </div>

      <p v-else-if="error" class="alert alert-error">{{ error }}</p>

      <div v-else class="space-y-4">
        <div v-for="item in items" :key="item.id"
          class="ceremony-card rounded-2xl border-2 p-6 text-center transition-all duration-300"
          :class="done.has(item.id)
            ? 'bg-gradient-to-br from-green-50 to-emerald-100 dark:from-green-900/30 dark:to-emerald-800/20 border-green-400'
            : 'bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 border-gray-300 dark:border-gray-600'"
        >
          <div v-if="item.rank" class="text-5xl mb-2">{{ podiumLabel[item.rank] ?? '🏅' }}</div>
          <h2 class="text-xl font-bold text-text mb-1">{{ item.name }}</h2>

          <div v-if="done.has(item.id)" class="mt-2">
            <p class="text-2xl font-black text-green-700 dark:text-green-300">{{ item.winnerName || 'Bez vítěze' }}</p>
            <div class="flex justify-center gap-4 mt-1 text-sm text-text-soft">
              <span v-if="item.winnerNumber">#{{ item.winnerNumber }}</span>
              <span v-if="item.winnerPoints != null">{{ item.winnerPoints }} b.</span>
            </div>
          </div>

          <button v-if="!done.has(item.id)" @click="announce(item)"
            :disabled="fullscreen"
            class="mt-4 btn-primary"
          >
            {{ fullscreen ? 'Probíhá…' : 'Vyhlásit' }}
          </button>
        </div>

        <p v-if="items.length === 0" class="text-center text-text-soft py-8">
          Žádné výsledky ani kategorie.
        </p>

        <div v-if="done.size === items.length && items.length > 0" class="text-center pt-6">
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
