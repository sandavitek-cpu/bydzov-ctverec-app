<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useScrollAnimation } from '@/composables/useScrollAnimation'
import { fetchCurrentEdition } from '@/api'
import RoleDashboard from '@/components/RoleDashboard.vue'

const { isLoggedIn, hasAdmin, hasJudge, hasRacer } = useAuth()

useScrollAnimation()

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
  <div v-else class="space-y-20">
    <!-- Hero -->
    <section class="relative overflow-hidden rounded-xl min-h-[320px] sm:min-h-[400px] flex items-center px-6 sm:px-10 py-10 sm:py-16 lg:py-24 border-l-4 border-l-red"
      style="background: linear-gradient(135deg, var(--bg-alt) 0%, var(--surface-strong) 50%, var(--bg-alt) 100%);"
    >
      <div class="absolute inset-0 opacity-[0.04] pointer-events-none select-none"
        style="background-image:
          linear-gradient(45deg, var(--text) 25%, transparent 25%),
          linear-gradient(-45deg, var(--text) 25%, transparent 25%),
          linear-gradient(45deg, transparent 75%, var(--text) 75%),
          linear-gradient(-45deg, transparent 75%, var(--text) 75%);
          background-size: 60px 60px;
          background-position: 0 0, 0 30px, 30px -30px, -30px 0px;
        "
      ></div>
      <div class="absolute top-0 right-0 w-1/3 h-full opacity-[0.06] pointer-events-none select-none"
        style="background: radial-gradient(ellipse at center, var(--primary) 0%, transparent 70%);"
      ></div>
      <div class="absolute bottom-0 left-0 w-1/2 h-1/2 opacity-[0.04] pointer-events-none select-none"
        style="background: radial-gradient(ellipse at center, var(--red) 0%, transparent 70%);"
      ></div>
      <div class="relative z-10 w-full">
        <p class="text-meta text-text-soft uppercase tracking-[0.12em]">30. ročník – 20.–21. června 2026</p>
        <h1 class="text-hero text-text mt-2">Novobydžovský Čtverec</h1>
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
    <section class="fade-in-up">
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
      <div v-for="(item, i) in [
        { icon: 'car', title: 'Spoustu krásných vozidel', text: 'Historické automobily a motocykly všeho druhu' },
        { icon: 'handshake', title: 'Přátelská atmosféra', text: 'Mezi příznivci panuje více než přátelská atmosféra' },
        { icon: 'music', title: 'Živá hudba', text: 'Doprovodný program po celý den' },
        { icon: 'food', title: 'Stánky s občerstvením', text: 'Občerstvení po celou dobu závodu' },
        { icon: 'kids', title: 'Dětský koutek', text: 'Zábava i pro nejmenší návštěvníky' },
        { icon: 'road', title: 'Příjemná vyjížďka', text: 'Vyrazte se svým vozem do okolí' },
        { icon: 'map', title: 'Spousta kontrol', text: 'Kontrolní stanoviště s různorodými úkoly' },
        { icon: 'trophy', title: 'Hodnotné ceny', text: 'Pamětní list pro každého účastníka' },
      ]" :key="item.title" class="card text-center fade-in-up" :class="'delay-' + ((i % 4) + 1)">
        <span class="inline-flex items-center justify-center w-12 h-12 rounded-full bg-primary/10 text-primary">
          <svg v-if="item.icon === 'car'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M14 10h-2m-4 0H6m12 0h-2M5 13l1.2-3.6A2 2 0 018.1 8h7.8a2 2 0 011.9 1.4L19 13m-1 4h-1a2 2 0 01-2-2v-1m-6 3h-1a2 2 0 01-2-2v-1m10-5V7a2 2 0 00-2-2H8a2 2 0 00-2 2v2"/></svg>
          <svg v-else-if="item.icon === 'handshake'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 9h4l2 3 3-3 3 3 2-3h4M4 15h4l2-3 3 3 3-3 2 3h4"/></svg>
          <svg v-else-if="item.icon === 'music'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19V6l12-3v13M9 19a3 3 0 11-6 0 3 3 0 016 0zm12-3a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
          <svg v-else-if="item.icon === 'food'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          <svg v-else-if="item.icon === 'kids'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"/></svg>
          <svg v-else-if="item.icon === 'road'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
          <svg v-else-if="item.icon === 'map'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/></svg>
          <svg v-else-if="item.icon === 'trophy'" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M6 3h12v4a6 6 0 11-12 0V3zm0 0H3v2a4 4 0 004 4h2m10-6h3v2a4 4 0 01-4 4h-2m-4 10v2m0-2a4 4 0 01-4-4h8a4 4 0 01-4 4z"/></svg>
        </span>
        <h3 class="text-subsection text-text mt-3">{{ item.title }}</h3>
        <p class="text-body-sm text-text-muted mt-1">{{ item.text }}</p>
      </div>
    </section>

    <!-- Conditions -->
    <section class="rounded-xl bg-surface-strong px-4 sm:px-8 py-8 border-l-4 border-l-primary fade-in-up">
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
    <section class="fade-in-up">
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
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[5rem] sm:min-w-[7rem]">{{ item.time }}</span>
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
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[5rem] sm:min-w-[7rem]">{{ item.time }}</span>
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
              <span class="shrink-0 rounded-md bg-primary/10 px-2 py-0.5 font-mono text-meta text-primary font-semibold min-w-[5rem] sm:min-w-[7rem]">{{ item.time }}</span>
              <span class="text-body-sm text-text-muted">{{ item.text }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Fee -->
    <section class="fade-in-up">
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
    <section class="rounded-xl bg-surface-strong px-8 py-8 fade-in-up">
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
    <section class="card text-center fade-in-up">
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
