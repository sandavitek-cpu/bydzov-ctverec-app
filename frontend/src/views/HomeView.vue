<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchCurrentEdition } from '@/api'
import { RouterLink } from 'vue-router'

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
  <div>
    <!-- Hero -->
    <section class="relative overflow-hidden rounded-xl bg-surface-strong px-8 py-12 lg:py-20 mb-10 border-l-4 border-l-accent-gold">
      <div class="absolute inset-0 opacity-[0.03] pointer-events-none select-none" style="background-image: repeating-linear-gradient(45deg, transparent, transparent 40px, currentColor 40px, currentColor 41px);"></div>
      <div class="relative z-10">
        <p class="text-meta text-text-soft uppercase tracking-[0.12em]">Motoristická soutěž historických vozidel</p>
        <h1 class="text-hero text-text mt-2">Novobydžovský<br />Čtverec</h1>
        <div class="flex items-center gap-4 mt-4">
          <span class="font-accent text-2xl italic text-accent-gold">Od roku 1996</span>
          <span class="h-8 w-px bg-border"></span>
          <span class="font-display text-3xl tracking-[0.04em] text-primary">{{ edition?.year ?? '…' }}</span>
        </div>
        <div class="mt-4 flex items-center gap-3 text-body-sm">
          <span class="inline-flex items-center gap-1.5 rounded-full bg-accent-gold/10 px-3 py-1 text-accent-gold font-semibold">
            <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>
            30. ročník — 20. června 2026
          </span>
        </div>
        <p class="mt-4 max-w-xl text-body-lg text-text-muted">
          Tradiční setkání milovníků historických vozidel. Přijďte se podívat nebo se zúčastněte závodu.
        </p>
        <div class="mt-8 flex flex-wrap gap-4">
          <RouterLink to="/registrace" class="btn-primary no-underline">
            Přihlásit se do soutěže
          </RouterLink>
          <RouterLink to="/vysledky/2026" class="btn-secondary no-underline">
            Výsledky {{ edition?.year ?? '…' }}
          </RouterLink>
        </div>
      </div>
      <!-- Decorative background -->
      <div class="absolute right-0 top-0 h-full w-1/3 opacity-[0.04] pointer-events-none select-none">
        <div class="absolute right-8 top-8 text-[200px] font-display text-text leading-none">{{ edition?.year ?? '' }}</div>
      </div>
    </section>

    <!-- Current edition card -->
    <div class="card mb-10 !border-l-accent-gold !border-l-4">
      <div v-if="loading" class="text-body-sm text-text-soft">Načítám…</div>
      <div v-else-if="error" class="alert alert-error">{{ error }}</div>
      <template v-else-if="edition">
        <div class="flex items-center gap-3">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary/10">
            <span class="font-display text-xl text-primary font-bold">{{ edition.year }}</span>
          </div>
          <div>
            <p class="text-meta text-text-soft uppercase tracking-wider">Aktuální ročník</p>
            <h2 class="text-section-title text-text">{{ edition.label }}</h2>
          </div>
        </div>
        <p class="text-body text-text-muted mt-3 ml-0 sm:ml-[60px]">
          Všechny informace o průběhu, výsledcích a přihláškách najdete zde.
        </p>
      </template>
    </div>

    <!-- Quick links -->
    <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <RouterLink to="/registrace" class="card no-underline group cursor-pointer">
        <div class="flex items-center justify-between">
          <h3 class="text-subsection text-text">Přihláška</h3>
          <span class="text-2xl text-text-soft opacity-40 transition-opacity group-hover:opacity-100">→</span>
        </div>
        <p class="mt-2 text-body-sm text-text-muted">Přihlaste svou posádku do soutěže</p>
      </RouterLink>
      <RouterLink to="/vysledky/2026" class="card no-underline group cursor-pointer">
        <div class="flex items-center justify-between">
          <h3 class="text-subsection text-text">Výsledky</h3>
          <span class="text-2xl text-text-soft opacity-40 transition-opacity group-hover:opacity-100">→</span>
        </div>
        <p class="mt-2 text-body-sm text-text-muted">Průběžné i konečné výsledky závodu</p>
      </RouterLink>
      <RouterLink to="/archiv" class="card no-underline group cursor-pointer">
        <div class="flex items-center justify-between">
          <h3 class="text-subsection text-text">Archiv</h3>
          <span class="text-2xl text-text-soft opacity-40 transition-opacity group-hover:opacity-100">→</span>
        </div>
        <p class="mt-2 text-body-sm text-text-muted">Historie všech ročníků soutěže</p>
      </RouterLink>
      <div class="card">
        <h3 class="text-subsection text-text">{{ edition?.year ?? '…' }}</h3>
        <p class="mt-2 text-body-sm text-text-muted">Aktuální ročník – sledujte novinky</p>
      </div>
    </div>

    <!-- Heritage section -->
    <div class="mt-16 pt-10 relative">
      <div class="absolute left-0 top-0 w-16 h-0.5 bg-accent-gold"></div>
      <div class="flex items-center gap-3 mb-1">
        <h2 class="text-section-title text-text">Historie soutěže</h2>
        <span class="hidden sm:inline h-px flex-1 bg-border"></span>
      </div>
      <p class="mt-4 text-body-lg text-text-muted max-w-2xl">
        Novobydžovský čtverec je tradiční motoristická soutěž pořádaná od roku 1996. 
        Každoročně přiláká desítky posádek s historickými vozidly a stovky návštěvníků.
      </p>
      <div class="mt-8 grid gap-6 sm:grid-cols-3">
        <div class="card text-center hover:border-accent-gold/30 transition-colors">
          <p class="text-kpi heritage-year">1996</p>
          <p class="text-body-sm text-text-muted mt-1">První ročník</p>
        </div>
        <div class="card text-center hover:border-accent-gold/30 transition-colors">
          <p class="text-kpi text-primary">30</p>
          <p class="text-body-sm text-text-muted mt-1">Ročníků</p>
        </div>
        <div class="card text-center hover:border-accent-gold/30 transition-colors">
          <p class="text-kpi text-accent-olive">500+</p>
          <p class="text-body-sm text-text-muted mt-1">Posádek</p>
        </div>
      </div>
    </div>
  </div>
</template>
