<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCeremonyCategories, fetchResults } from '@/api'

interface CeremonyItem {
  id: string
  name: string
  winnerName: string | null
  winnerNumber: number | null
  winnerPoints: number | null
  rank?: number
}

const route = useRoute()
const year = ref(Number(route.params.rok) || new Date().getFullYear())
const loading = ref(true)
const error = ref<string | null>(null)
const items = ref<CeremonyItem[]>([])
const done = ref<Set<string>>(new Set())
const fullscreen = ref(false)
const fsItem = ref<CeremonyItem | null>(null)
const fsCountdown = ref(0)
const fsPhase = ref<'countdown' | 'reveal'>('countdown')

function announce(item: CeremonyItem) {
  if (fullscreen.value) return
  fsItem.value = item
  fsPhase.value = 'countdown'
  fsCountdown.value = 3
  fullscreen.value = true
  let count = 3
  const t = setInterval(() => {
    count--
    fsCountdown.value = count
    if (count <= 0) {
      clearInterval(t)
      fsPhase.value = 'reveal'
      done.value = new Set([...done.value, item.id])
    }
  }, 800)
}

function resetAll() {
  fullscreen.value = false
  fsItem.value = null
  done.value = new Set()
}
function closeOverlay() {
  fullscreen.value = false
  fsItem.value = null
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [catData, resData] = await Promise.all([
      fetchCeremonyCategories(year.value).catch(() => ({ year: year.value, categories: [] })),
      fetchResults(year.value).catch(() => null),
    ])
    const result: CeremonyItem[] = []
    if (resData?.results) {
      const top = [...resData.results].sort((a, b) => a.rank - b.rank).slice(0, 3)
      for (const r of top) {
        result.push({
          id: `r${r.rank}`,
          name: r.rank === 1 ? '1. místo' : r.rank === 2 ? '2. místo' : '3. místo',
          winnerName: r.teamName,
          winnerNumber: r.startNumber,
          winnerPoints: r.totalPoints,
          rank: r.rank,
        })
      }
    }
    for (const c of catData.categories) {
      result.push({
        id: `c${c.id}`,
        name: c.name,
        winnerName: c.winnerName,
        winnerNumber: c.winnerNumber,
        winnerPoints: c.winnerPoints,
      })
    }
    items.value = result
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
    <!-- Fullscreen overlay -->
    <div v-if="fullscreen"
      class="fixed inset-0 z-[99999] flex flex-col items-center justify-center"
      style="background: rgba(0,0,0,0.92)"
      @click="fsPhase === 'reveal' && closeOverlay()"
    >
      <template v-if="fsPhase === 'countdown'">
        <div class="text-[15vw] font-black text-white select-none" style="animation: pulse 1s ease-in-out infinite">
          {{ fsCountdown }}
        </div>
        <p class="text-xl text-white/60 mt-4 tracking-widest uppercase">
          {{ fsCountdown === 3 ? 'Připravte se…' : fsCountdown === 2 ? 'Bubnování… 🥁' : 'A vítězem se stává…' }}
        </p>
        <p class="text-lg text-white/40 mt-2">{{ fsItem?.name }}</p>
      </template>
      <template v-else>
        <div class="text-8xl mb-6" style="animation: bounce 1s ease-in-out infinite">🏆</div>
        <p class="text-2xl text-white/60 mb-2">{{ fsItem?.name }}</p>
        <p class="text-5xl sm:text-[8vw] font-black text-white text-center px-4 leading-tight">
          {{ fsItem?.winnerName || 'Bez vítěze' }}
        </p>
        <div class="flex gap-6 mt-6 text-xl text-white/50">
          <span v-if="fsItem?.winnerNumber">#{{ fsItem.winnerNumber }}</span>
          <span v-if="fsItem?.winnerPoints != null">{{ fsItem.winnerPoints }} bodů</span>
        </div>
        <p class="mt-12 text-sm text-white/30">Kliknutím zavřete</p>
      </template>
    </div>

    <!-- Page content -->
    <div class="flex flex-col items-center justify-center px-4 py-8">
      <div class="w-full max-w-2xl">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-text tracking-tight">Vyhlášení výsledků {{ year }}</h1>
        </div>

        <div v-if="loading" class="text-center py-16">
          <div class="animate-spin w-10 h-10 border-4 border-primary border-t-transparent rounded-full mx-auto"></div>
        </div>

        <p v-else-if="error" class="alert alert-error">{{ error }}</p>

        <div v-else-if="items.length === 0" class="text-center py-8">
          <p class="text-body text-text-soft">Žádné výsledky ani kategorie.</p>
        </div>

        <div v-else class="space-y-4">
          <div v-for="item in items" :key="item.id"
            class="rounded-2xl border-2 p-6 text-center transition-all duration-300"
            :class="done.has(item.id)
              ? 'bg-gradient-to-br from-green-50 to-emerald-100 dark:from-green-900/30 dark:to-emerald-800/20 border-green-400'
              : 'bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 border-gray-300 dark:border-gray-600'"
          >
            <div v-if="item.rank" class="text-5xl mb-2">{{ item.rank === 1 ? '🏆' : item.rank === 2 ? '🥈' : item.rank === 3 ? '🥉' : '🏅' }}</div>
            <h2 class="text-xl font-bold text-text mb-1">{{ item.name }}</h2>

            <div v-if="done.has(item.id)" class="mt-2">
              <p class="text-2xl font-black text-green-700 dark:text-green-300">{{ item.winnerName || 'Bez vítěze' }}</p>
              <div class="flex justify-center gap-4 mt-1 text-sm text-text-soft">
                <span v-if="item.winnerNumber">#{{ item.winnerNumber }}</span>
                <span v-if="item.winnerPoints != null">{{ item.winnerPoints }} b.</span>
              </div>
            </div>

            <button v-else @click="announce(item)"
              :disabled="fullscreen"
              class="mt-4 btn-primary"
            >
              {{ fullscreen ? 'Probíhá…' : 'Vyhlásit' }}
            </button>
          </div>

          <div v-if="done.size > 0 && done.size === items.length" class="text-center pt-6">
            <button @click="resetAll" class="btn-secondary">Resetovat</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}
</style>
