<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchCurrentEdition } from '@/api'

const loading = ref(true)
const error = ref<string | null>(null)
const edition = ref<{ year: number; label: string } | null>(null)

onMounted(async () => {
  try {
    edition.value = await fetchCurrentEdition()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Nepodařilo se načíst ročník'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-6">
    <div>
      <p class="text-sm uppercase tracking-widest text-amber-500/90">Motoristická soutěž</p>
      <h1 class="mt-2 text-3xl font-bold text-white md:text-4xl">Historická vozidla v Novém Bydžově</h1>
      <p class="mt-3 text-slate-400">
        Webová aplikace pro přihlášky, pořadatele a komisaře — vývojová verze napojená na API.
      </p>
    </div>

    <div
      class="rounded-xl border border-slate-800 bg-slate-900/60 p-6 shadow-lg shadow-black/20"
    >
      <h2 class="text-sm font-medium text-slate-400">Aktuální ročník z API</h2>
      <p v-if="loading" class="mt-2 text-slate-500">Načítám…</p>
      <p v-else-if="error" class="mt-2 text-red-400">{{ error }}</p>
      <template v-else-if="edition">
        <p class="mt-2 text-2xl font-semibold text-white">{{ edition.label }}</p>
        <p class="text-sm text-slate-500">Rok {{ edition.year }}</p>
      </template>
    </div>

    <RouterLink
      to="/registrace"
      class="block rounded-xl bg-amber-500 px-6 py-4 text-center font-semibold text-black shadow-lg transition hover:bg-amber-400"
    >
      Přihlásit se do soutěže →
    </RouterLink>
  </div>
</template>
