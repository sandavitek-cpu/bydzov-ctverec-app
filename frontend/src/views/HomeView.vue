<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { fetchCurrentEdition } from '@/api'
import RoleDashboard from '@/components/RoleDashboard.vue'

const { isLoggedIn, hasAdmin, hasJudge, hasRacer } = useAuth()

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
  <RoleDashboard v-if="isLoggedIn && (hasAdmin || hasJudge || hasRacer)" />
  <div v-else class="space-y-16">
    <!-- Hero -->
    <section class="relative overflow-hidden rounded-xl bg-surface-strong px-8 py-12 lg:py-20 mb-4 border-l-4 border-l-red">
      <div class="absolute inset-0 opacity-[0.03] pointer-events-none select-none" style="background-image: repeating-linear-gradient(45deg, transparent, transparent 40px, currentColor 40px, currentColor 41px);"></div>
      <div class="relative z-10">
        <p class="text-meta text-text-soft uppercase tracking-[0.12em]">30. ročník – 20.–21. června 2026</p>
        <h1 class="text-hero text-text mt-2">Novobydžovský<br />Čtverec</h1>
        <p class="font-accent text-xl italic text-red mt-1">Memoriál Elišky Junkové</p>
        <div class="flex items-center gap-4 mt-4">
          <span class="font-accent text-2xl italic text-red">Od roku 1996</span>
          <span class="h-8 w-px bg-border"></span>
          <span class="font-display text-3xl tracking-[0.04em] text-primary">{{ edition?.year ?? '…' }}</span>
        </div>
        <div class="mt-8 flex flex-wrap gap-4">
          <RouterLink to="/registrace" class="btn-primary no-underline">
            Přihlásit se do soutěže
          </RouterLink>
          <RouterLink to="/vysledky/2026" class="btn-secondary no-underline">
            Výsledky {{ edition?.year ?? '…' }}
          </RouterLink>
        </div>
      </div>
    </section>

    <!-- About -->
    <section>
      <h2 class="text-section-title text-text">O závodě</h2>
      <div class="mt-4 max-w-3xl text-body-lg text-text-muted space-y-4 leading-relaxed">
        <p>
          Novobydžovský čtverec je tradiční <strong class="text-text">orientační závod</strong>, který v Novém Bydžově a okolí působí už 30 let. Jde o sraz nadšenců veteránských vozidel všeho druhu.
        </p>
        <p>
          Závodníci se mohou účastnit pouze se strojem <strong class="text-text">do roku 1989</strong> a mají možnost si vybrat z jednodenní nebo dvoudenní varianty závodu, kde <strong class="text-text">rychlost není rozhodující</strong>.
        </p>
        <p>
          Závod spočívá v řešení různých úkolů a otázek v kontrolních stanovištích umístěných různě na trase. Mezi oblíbené kontroly patří například odhad vzdálenosti, převoz na voru na čas, slalom, házení štětkou do záchodu a mnoho dalšího!
        </p>
        <p>
          Závod je orientační, takže závodník se předem nemůže připravit, kde a jaké kontroly jsou, ani kudy závodní trasa vede. Délka trasy se pohybuje kolem <strong class="text-text">100 km</strong>.
        </p>
        <p>
          Na výstavu historických vozidel se veřejnost může podívat na náměstí města a v průběhu závodu na cestě. Mezi příznivci panuje více než přátelská atmosféra.
        </p>
        <p class="text-text font-medium">Přijeďte se i Vy pobavit a zasoutěžit si s Vaším veteránem!</p>
      </div>
    </section>

    <!-- Why -->
    <section class="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <div v-for="item in [
        { icon: '🚗', title: 'Spoustu krásných vozidel', text: 'Historické automobily a motocykly všeho druhu' },
        { icon: '🤝', title: 'Přátelská atmosféra', text: 'Mezi příznivci panuje více než přátelská atmosféra' },
        { icon: '🎵', title: 'Živá hudba', text: 'Doprovodný program po celý den' },
        { icon: '🍴', title: 'Stánky s občerstvením', text: 'Občerstvení po celou dobu závodu' },
        { icon: '🧒', title: 'Dětský koutek', text: 'Zábava i pro nejmenší návštěvníky' },
        { icon: '🏎️', title: 'Příjemná vyjížďka', text: 'Vyrazte se svým vozem do okolí' },
        { icon: '📍', title: 'Spousta kontrol', text: 'Kontrolní stanoviště s různorodými úkoly' },
        { icon: '🏆', title: 'Hodnotné ceny', text: 'Pamětní list pro každého účastníka' },
      ]" :key="item.title" class="card text-center">
        <span class="text-3xl">{{ item.icon }}</span>
        <h3 class="text-subsection text-text mt-2">{{ item.title }}</h3>
        <p class="text-body-sm text-text-muted mt-1">{{ item.text }}</p>
      </div>
    </section>

    <!-- Conditions -->
    <section class="rounded-xl bg-surface-strong px-8 py-8 border-l-4 border-l-primary">
      <h2 class="text-section-title text-text">Podmínky závodníka</h2>
      <div class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div class="flex items-center gap-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-bold">1</span>
          <span class="text-body text-text-muted">Vozidlo <strong class="text-text">do roku 1989</strong></span>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-bold">2</span>
          <span class="text-body text-text-muted"><strong class="text-text">Platná přihláška</strong> s vyplněnými údaji</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-bold">3</span>
          <span class="text-body text-text-muted"><strong class="text-text">Plná nádrž</strong> a dobrá nálada</span>
        </div>
        <div class="flex items-center gap-3">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-bold">4</span>
          <span class="text-body text-text-muted"><strong class="text-text">Dobrá nálada</strong> – rychlost není rozhodující</span>
        </div>
      </div>
    </section>

    <!-- Program -->
    <section>
      <div class="flex items-center gap-3 mb-1">
        <h2 class="text-section-title text-text">Program</h2>
        <span class="hidden sm:inline h-px flex-1 bg-border"></span>
      </div>
      <div class="mt-6 grid gap-6 sm:grid-cols-2">
        <div class="card">
          <h3 class="text-subsection text-primary">Jednodenní závod</h3>
          <div class="mt-4 space-y-4">
            <div v-for="item in [
              { time: '8:30 – 9:00', text: 'Prezence účastníků závodu v průchodu radnice' },
              { time: '9:00', text: 'Začátek výstavy historických vozidel na náměstí' },
              { time: '11:00', text: 'Start závodu – kategorie motocyklů' },
              { time: 'hned poté', text: 'Start závodu – kategorie automobilů' },
              { time: '—', text: 'Oběd na trase' },
              { time: '16:30 – 18:30', text: 'Večeře' },
              { time: '~ 19:00', text: 'Vyhlášení vítězů' },
            ]" :key="item.time" class="flex items-start gap-3">
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[7rem]">{{ item.time }}</span>
              <span class="text-body-sm text-text-muted">{{ item.text }}</span>
            </div>
          </div>
        </div>
        <div class="card">
          <h3 class="text-subsection text-primary">Dvoudenní závod</h3>
          <div class="mt-4 space-y-4">
            <p class="text-meta text-text-soft uppercase tracking-wider text-xs">Sobota</p>
            <div v-for="item in [
              { time: '8:30 – 9:00', text: 'Prezence účastníků závodu v průchodu radnice' },
              { time: '9:00', text: 'Začátek výstavy historických vozidel na náměstí' },
              { time: '11:00', text: 'Start závodu – kategorie motocyklů' },
              { time: 'hned poté', text: 'Start závodu – kategorie automobilů' },
              { time: '—', text: 'Oběd na trase' },
              { time: 'od 14:00', text: 'Dojezd do kempu, ubytování a večeře' },
            ]" :key="item.time" class="flex items-start gap-3">
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[7rem]">{{ item.time }}</span>
              <span class="text-body-sm text-text-muted">{{ item.text }}</span>
            </div>
            <hr class="border-border" />
            <p class="text-meta text-text-soft uppercase tracking-wider text-xs">Neděle</p>
            <div v-for="item in [
              { time: '8:00', text: 'Snídaně v kempu' },
              { time: '9:00', text: 'Start druhý den' },
              { time: '11:30 – 12:30', text: 'Oběd' },
              { time: '~ 13:00', text: 'Vyhlášení vítězů' },
            ]" :key="item.time" class="flex items-start gap-3">
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[7rem]">{{ item.time }}</span>
              <span class="text-body-sm text-text-muted">{{ item.text }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Fee -->
    <section>
      <div class="flex items-center gap-3 mb-1">
        <h2 class="text-section-title text-text">Ceník startovného</h2>
        <span class="hidden sm:inline h-px flex-1 bg-border"></span>
      </div>
      <p class="text-body-sm text-text-soft mt-2">V ceně je plnohodnotné občerstvení na celý závod, pamětní list a ubytování (pouze dvoudenní varianta).</p>
      <div class="mt-6 grid gap-4 sm:grid-cols-3">
        <div class="card border-l-4 border-l-red">
          <h3 class="text-subsection text-text">Jednodenní závod</h3>
          <div class="mt-4 space-y-3">
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-red">Vozidlo DO 1945</p>
              <p class="text-kpi text-text mt-1">500 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 500 Kč každý další člen</p>
            </div>
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-text-muted">Vozidlo OD 1946</p>
              <p class="text-kpi text-text mt-1">800 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 500 Kč každý další člen</p>
            </div>
          </div>
        </div>
        <div class="card border-l-4 border-l-primary">
          <h3 class="text-subsection text-text">Dvoudenní – UZAVŘENO</h3>
          <div class="mt-4 space-y-3">
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-red">Vozidlo DO 1945</p>
              <p class="text-kpi text-text mt-1">1 000 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 1 000 Kč každý další člen</p>
            </div>
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-text-muted">Vozidlo OD 1946</p>
              <p class="text-kpi text-text mt-1">1 200 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 1 000 Kč každý další člen</p>
            </div>
          </div>
        </div>
        <div class="card border-l-4 border-l-text-soft">
          <h3 class="text-subsection text-text">Dvoudenní bez ubytování</h3>
          <div class="mt-4 space-y-3">
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-red">Vozidlo DO 1945</p>
              <p class="text-kpi text-text mt-1">600 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 600 Kč každý další člen</p>
            </div>
            <div class="rounded-md bg-bg-alt p-3">
              <p class="text-label text-text-muted">Vozidlo OD 1946</p>
              <p class="text-kpi text-text mt-1">900 Kč</p>
              <p class="text-meta text-text-soft">včetně řidiče + 600 Kč každý další člen</p>
            </div>
          </div>
        </div>
      </div>
      <p class="mt-4 text-body-sm text-text-soft">Splatnost startovného je 14 dnů od vyplnění přihlášky, nejpozději však do uzávěrky přihlášky. Účet: <span class="font-mono text-text">1086360369/0800</span></p>
    </section>

    <!-- Categories -->
    <section class="rounded-xl bg-surface-strong px-8 py-8">
      <h2 class="text-section-title text-text">Závodní kategorie</h2>
      <div class="mt-6 grid gap-6 sm:grid-cols-2">
        <div>
          <h3 class="text-subsection text-primary">Jednodenní závod</h3>
          <ul class="mt-3 space-y-2 text-body-sm text-text-muted">
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejmladší účastník</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší účastník</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší automobil</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší motocykl</li>
            <li class="flex items-center gap-2"><span class="text-primary">●</span> Hlavní kategorie <strong>Auto do roku 1945</strong></li>
            <li class="flex items-center gap-2"><span class="text-primary">●</span> Hlavní kategorie <strong>Auto od roku 1946</strong></li>
            <li class="flex items-center gap-2"><span class="text-primary">●</span> Hlavní kategorie <strong>Moto</strong></li>
            <li class="flex items-center gap-2"><span class="text-text-soft">●</span> Speciální cena pro posledního</li>
          </ul>
        </div>
        <div>
          <h3 class="text-subsection text-primary">Dvoudenní závod</h3>
          <ul class="mt-3 space-y-2 text-body-sm text-text-muted">
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejmladší účastník</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší účastník</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší automobil</li>
            <li class="flex items-center gap-2"><span class="text-red">●</span> Nejstarší motocykl</li>
            <li class="flex items-center gap-2"><span class="text-primary">●</span> Hlavní kategorie celého dvoudenního závodu</li>
            <li class="flex items-center gap-2"><span class="text-text-soft">●</span> Speciální cena pro posledního</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- Contact -->
    <section class="card text-center">
      <p class="text-body-lg text-text-muted">Pro více informací nás neváhejte kontaktovat.</p>
      <p class="text-section-title text-text mt-2">Těšíme se na Vás!</p>
      <div class="mt-6">
        <p class="text-subsection text-primary">PETR HAZDRA</p>
        <p class="text-meta text-text-soft">Předseda klubu</p>
        <div class="mt-3 flex flex-wrap justify-center gap-6 text-body text-text-muted">
          <a href="tel:+420605732332" class="text-primary hover:text-primary-hover no-underline">+420 605 732 332</a>
          <a href="mailto:nbctverec@gmail.com" class="text-primary hover:text-primary-hover no-underline">nbctverec@gmail.com</a>
        </div>
      </div>
    </section>
  </div>
</template>
