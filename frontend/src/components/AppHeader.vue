<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, fetchNotifications, markNotificationRead, markAllNotificationsRead, fetchCurrentEdition } from '@/api'
import type { NotificationItem } from '@/api'
import logoCtverec from '@/assets/logo_ctverec.png'

const route = useRoute()
const { isLoggedIn, name, username, hasAdmin, hasJudge, hasRacer, logout, authHeaders } = useAuth()
const emit = defineEmits<{ impersonate: [] }>()

const isDark = ref(document.documentElement.classList.contains('dark'))

function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

const mobileNavOpen = ref(false)
const dropdownOpen = ref(false)
const notificationOpen = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)

const currentEdition = ref<{ id: number; year: number; label: string } | null>(null)

const raceStarted = ref(false)
const raceFinished = ref(false)
const scheduleItems = ref<{ time: string; label: string; description: string | null }[]>([])
const finishedCount = ref(0)
const totalRacers = ref(0)
let progressPoll: ReturnType<typeof setInterval> | null = null

const racePhase = computed(() => {
  if (raceFinished.value) return { label: 'Ukončen', class: 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400' }
  if (raceStarted.value) return { label: 'Probíhá', class: 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 animate-pulse' }
  return null
})

const currentScheduleItem = computed(() => {
  if (!scheduleItems.value.length) return null
  const now = new Date()
  const nowMinutes = now.getHours() * 60 + now.getMinutes()
  const sorted = [...scheduleItems.value].sort((a, b) => a.time.localeCompare(b.time))
  const current = sorted.findLast(item => {
    const [h, m] = item.time.split(':').map(Number)
    return h * 60 + m <= nowMinutes
  })
  return current || sorted[0]
})

const appInfo = ref<{ version: string; deployedAt: string; changelog: { version: string; description: string; createdAt: string }[] } | null>(null)
const showInfo = ref(false)
const infoRef = ref<HTMLElement | null>(null)
let notificationPoll: ReturnType<typeof setInterval> | null = null

async function loadNotifications() {
  if (!isLoggedIn.value) return
  try {
    const data = await fetchNotifications(authHeaders())
    notifications.value = data.notifications
    unreadCount.value = data.unreadCount
  } catch { /* ignore */ }
}

async function onMarkRead(id: number) {
  await markNotificationRead(id, authHeaders())
  await loadNotifications()
}

async function onMarkAllRead() {
  await markAllNotificationsRead(authHeaders())
  await loadNotifications()
}

function toggleNotifications() {
  notificationOpen.value = !notificationOpen.value
  if (notificationOpen.value) loadNotifications()
}

function onLogout() {
  logout()
}

async function loadCheckpointProgress() {
  if (!isLoggedIn.value || !hasAdmin.value) return
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/checkpoints/progress`, { headers: authHeaders() })
    if (res.ok) {
      const data = await res.json()
      totalRacers.value = data.totalRacers ?? 0
      const checkpoints = data.checkpoints as { scoredCount: number }[]
      finishedCount.value = checkpoints.length > 0 ? checkpoints[checkpoints.length - 1].scoredCount : 0
    }
  } catch { /* ignore */ }
}

async function toggleInfo() {
  if (showInfo.value) {
    showInfo.value = false
    return
  }
  if (!appInfo.value) {
    try {
      const res = await fetch(`${apiBaseUrl}/api/public/info`)
      if (res.ok) appInfo.value = await res.json()
    } catch { /* ignore */ }
  }
  showInfo.value = true
}

function onDocumentClick(e: MouseEvent) {
  if (showInfo.value && infoRef.value && !infoRef.value.contains(e.target as Node)) {
    showInfo.value = false
  }
  if (dropdownOpen.value) {
    const target = e.target as HTMLElement
    if (!target.closest('.dropdown-menu') && !target.closest('.dropdown-toggle')) {
      dropdownOpen.value = false
    }
  }
  if (notificationOpen.value) {
    const target = e.target as HTMLElement
    if (!target.closest('.notification-menu') && !target.closest('.notification-toggle')) {
      notificationOpen.value = false
    }
  }
}

watch(() => route.path, () => { mobileNavOpen.value = false; dropdownOpen.value = false; notificationOpen.value = false })

onMounted(async () => {
  document.addEventListener('click', onDocumentClick)
  if (isLoggedIn.value) {
    loadNotifications()
    notificationPoll = setInterval(loadNotifications, 30000)
  }
  try {
    currentEdition.value = await fetchCurrentEdition()
    const infoRes = await fetch(`${apiBaseUrl}/api/public/info`)
    if (infoRes.ok) {
      const info = await infoRes.json()
      raceStarted.value = info.raceStarted ?? false
      raceFinished.value = info.raceFinished ?? false
    }
    if (isLoggedIn.value && (hasRacer.value || hasAdmin.value)) {
      const h = authHeaders()
      const endpoint = hasAdmin.value ? `${apiBaseUrl}/api/admin/schedule` : `${apiBaseUrl}/api/racer/schedule`
      const schedRes = await fetch(endpoint, { headers: h })
      if (schedRes.ok) scheduleItems.value = await schedRes.json()
    }
    await loadCheckpointProgress()
    if (raceStarted.value) {
      progressPoll = setInterval(loadCheckpointProgress, 15000)
    }
  } catch { /* ignore */ }
})

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
  if (notificationPoll) clearInterval(notificationPoll)
  if (progressPoll) clearInterval(progressPoll)
})
</script>

<template>
  <header class="h-18 border-b border-border bg-surface shrink-0 relative">
    <div class="flex h-full items-center justify-between gap-4 px-4 lg:px-8">
      <RouterLink to="/" class="flex items-center gap-3 no-underline group shrink-0" @click="mobileNavOpen = false">
        <img
          :src="logoCtverec"
          alt="Novobydžovský čtverec"
          class="h-10 w-auto object-contain"
        />
        <div class="flex flex-col leading-tight max-sm:hidden">
          <span class="font-display text-2xl tracking-[0.04em] text-text">Novobydžovský</span>
          <span class="font-display text-2xl tracking-[0.04em] text-primary -mt-1">Čtverec</span>
          <span class="text-meta text-red -mt-0.5">Memoriál Elišky Junkové</span>
        </div>
      </RouterLink>

      <div class="hidden xl:flex items-center gap-4 px-5 border-x border-border min-w-0">
        <div v-if="currentEdition" class="flex items-center gap-3">
          <span class="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
            {{ currentEdition.year }}
          </span>
        </div>
        <div v-if="racePhase" class="flex items-center gap-2">
          <span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold" :class="racePhase.class">
            {{ racePhase.label }}
          </span>
        </div>
        <div v-if="finishedCount > 0 && totalRacers > 0" class="flex items-center gap-2">
          <span class="flex items-center gap-1.5 rounded-full bg-amber-100 dark:bg-amber-900/30 px-2.5 py-0.5 text-xs font-semibold text-amber-700 dark:text-amber-400">
            <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ finishedCount }} / {{ totalRacers }} v cíli
          </span>
        </div>
        <div v-if="currentScheduleItem && raceStarted" class="flex flex-col leading-tight">
          <span class="text-meta text-text-soft font-mono">{{ currentScheduleItem.time }}</span>
          <span class="text-label font-medium text-text whitespace-nowrap">{{ currentScheduleItem.label }}</span>
        </div>
        <div v-if="!raceStarted" class="flex items-center gap-4">
          <span class="flex items-center gap-1.5 text-meta text-text-muted">
            <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {{ currentEdition?.label }}
          </span>
        </div>
      </div>

      <div class="flex items-center gap-2 md:hidden">
        <button @click="mobileNavOpen = !mobileNavOpen" class="flex h-10 w-10 items-center justify-center rounded-lg text-text-muted transition-colors hover:bg-bg-alt">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!mobileNavOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <nav class="hidden md:flex items-center gap-0.5">
        <RouterLink to="/" class="nav-link" active-class="nav-link-active">
          Domů
        </RouterLink>
        <RouterLink to="/registrace" class="nav-link" active-class="nav-link-active">
          Přihláška
        </RouterLink>
        <RouterLink to="/vysledky/2026" class="nav-link" active-class="nav-link-active">
          Výsledky
        </RouterLink>
        <RouterLink to="/archiv" class="nav-link" active-class="nav-link-active">
          Archiv
        </RouterLink>

        <div class="ml-3 pl-3 border-l border-border">
          <RouterLink
            v-if="!isLoggedIn"
            to="/admin/login"
            class="btn-primary btn-sm no-underline"
          >
            Přihlásit
          </RouterLink>

          <div v-else class="flex items-center gap-0.5">
            <div class="relative">
              <button @click.stop="dropdownOpen = !dropdownOpen" @mouseenter="dropdownOpen = true"
                class="dropdown-toggle flex items-center gap-2 rounded-lg px-3 py-1.5 text-label text-text-muted transition-colors hover:bg-bg-alt"
              >
                <svg class="h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <span>{{ name }}<span v-if="username" class="ml-1 text-text-soft text-meta">@{{ username }}</span></span>
                <svg class="h-3 w-3 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              <div v-if="dropdownOpen" @mouseleave="dropdownOpen = false"
                class="absolute right-0 top-full z-40 mt-1 w-56 origin-top-right scale-100 dropdown-menu">
                <template v-if="hasAdmin">
                  <div class="admin-sidebar-section">Administrace</div>
                  <RouterLink to="/admin/prihlaseni" class="dropdown-item">Přihlášky</RouterLink>
                  <RouterLink to="/admin/varianty" class="dropdown-item">Závod</RouterLink>
                  <RouterLink to="/admin/program" class="dropdown-item">Program</RouterLink>
                  <RouterLink to="/admin/kategorie" class="dropdown-item">Kategorie</RouterLink>
                  <RouterLink :to="`/ceremoniál/${new Date().getFullYear()}`" class="dropdown-item">Ceremoniál</RouterLink>
                  <RouterLink to="/admin/trasy" class="dropdown-item">Trasy</RouterLink>
                  <RouterLink to="/admin/stanoviste" class="dropdown-item">Stanoviště</RouterLink>
                  <RouterLink to="/admin/bodovani" class="dropdown-item">Bodování</RouterLink>
                  <RouterLink to="/admin/incidenty" class="dropdown-item">Úkoly pro pořadatele</RouterLink>
                  <RouterLink to="/admin/komunikace" class="dropdown-item">Komunikace</RouterLink>
                  <RouterLink to="/admin/logovani" class="dropdown-item">Logování</RouterLink>
                  <RouterLink to="/admin/changelog" class="dropdown-item">ChangeLog</RouterLink>
                  <RouterLink to="/admin/role" class="dropdown-item">Role</RouterLink>
                  <RouterLink to="/admin/uzivatele" class="dropdown-item">Uživatelé</RouterLink>
                  <button @click="emit('impersonate')" class="dropdown-item">Přihlásit jako</button>
                  <hr class="my-1 mx-3 border-border" />
                </template>
                <template v-if="hasJudge">
                  <RouterLink to="/komisari/prehled" class="dropdown-item">Přehled</RouterLink>
                  <RouterLink to="/komisari" class="dropdown-item">Bodování</RouterLink>
                </template>
                <template v-if="hasRacer">
                  <div class="admin-sidebar-section">Závodník</div>
                  <RouterLink to="/zavodnik/itinerar" class="dropdown-item">Itinerář</RouterLink>
                  <RouterLink to="/zavodnik/mapa" class="dropdown-item">Mapa</RouterLink>
                  <RouterLink to="/zavodnik/stav" class="dropdown-item">Můj stav</RouterLink>
                  <RouterLink to="/zavodnik/vozidla" class="dropdown-item">Vozový park</RouterLink>
                  <RouterLink to="/zavodnik/ukoly" class="dropdown-item">Moje úkoly</RouterLink>
                </template>
                <hr class="my-1 mx-3 border-border" />
                <RouterLink to="/ucet" class="dropdown-item">Nastavení účtu</RouterLink>
                <button @click="onLogout" class="dropdown-item text-text-soft">Odhlásit</button>
              </div>
            </div>

            <span class="relative">
              <button @click.stop="toggleNotifications"
                class="notification-toggle flex h-8 w-8 items-center justify-center rounded-lg text-text-soft transition-colors hover:bg-bg-alt hover:text-text-muted"
                title="Notifikace"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                </svg>
                <span v-if="unreadCount > 0"
                  class="absolute -right-0.5 -top-0.5 flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-error px-1 text-[10px] font-bold leading-none text-white"
                >{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </button>
              <div v-if="notificationOpen" @click.stop
                class="notification-menu absolute right-0 top-full z-50 mt-1 w-80 max-h-96 overflow-y-auto rounded-xl border border-border bg-surface shadow-lg"
              >
                <div class="flex items-center justify-between px-4 py-3 border-b border-border">
                  <span class="text-label font-semibold text-text">Notifikace</span>
                  <button v-if="unreadCount > 0" @click="onMarkAllRead" class="text-meta text-primary hover:underline">Označit vše jako přečtené</button>
                </div>
                <div v-if="notifications.length === 0" class="px-4 py-8 text-center text-body-sm text-text-soft">
                  Žádné notifikace
                </div>
                <div v-for="n in notifications" :key="n.id"
                  class="flex items-start gap-3 px-4 py-3 transition-colors hover:bg-bg-alt cursor-pointer border-b border-border/50 last:border-b-0"
                  :class="{ 'bg-primary/5': !n.isRead }"
                  @click="onMarkRead(n.id)"
                >
                  <div class="flex-1 min-w-0">
                    <p class="text-body-sm font-medium text-text truncate">{{ n.title }}</p>
                    <p class="text-meta text-text-soft mt-0.5 line-clamp-2">{{ n.message }}</p>
                    <p class="text-meta text-text-soft mt-1">{{ new Date(n.createdAt).toLocaleString('cs') }}</p>
                  </div>
                  <div v-if="!n.isRead" class="mt-1.5 h-2 w-2 rounded-full bg-primary shrink-0"></div>
                </div>
              </div>
            </span>

            <button @click="toggleDark"
              class="flex h-8 w-8 items-center justify-center rounded-lg text-text-soft transition-colors hover:bg-bg-alt hover:text-text-muted"
              :title="isDark ? 'Přepnout na světlý režim' : 'Přepnout na tmavý režim'"
            >
              <svg v-if="!isDark" class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
              </svg>
              <svg v-else class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            </button>

            <span ref="infoRef" class="relative">
              <button @click.stop="toggleInfo"
                class="flex h-8 items-center gap-1 rounded-lg px-2 text-text-soft transition-colors hover:bg-bg-alt hover:text-text-muted"
                title="Info o aplikaci"
              >
                <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span class="text-meta font-mono leading-none">{{ appInfo?.version ?? '' }}</span>
              </button>
              <div
                v-if="showInfo"
                @click.stop
                class="absolute right-0 top-full z-50 mt-1 w-80 rounded-xl border border-border bg-surface p-5 shadow-lg"
              >
                <p class="text-meta text-text-soft">verze <span class="font-mono text-text">{{ appInfo?.version ?? '…' }}</span></p>
                <p class="mt-1 text-meta text-text-soft">nasazeno {{ appInfo?.deployedAt ? new Date(appInfo.deployedAt).toLocaleString('cs') : '…' }}</p>
                <div v-if="appInfo?.changelog?.length" class="mt-4 border-t border-border pt-4">
                  <p class="mb-3 text-meta font-semibold text-text-muted">ChangeLog</p>
                  <div v-for="(entry, i) in appInfo.changelog" :key="i" class="mb-3 last:mb-0">
                    <p class="text-label text-primary font-mono">{{ entry.version }}</p>
                    <p class="mt-0.5 text-body-sm text-text-muted">{{ entry.description }}</p>
                    <p class="text-meta text-text-soft">{{ new Date(entry.createdAt).toLocaleDateString('cs') }}</p>
                  </div>
                </div>
                <button @click="showInfo = false" class="mt-3 text-label text-primary hover:text-primary-hover">Zavřít</button>
              </div>
            </span>
          </div>
        </div>
      </nav>
    </div>

    <transition name="mobile-nav">
      <div v-if="mobileNavOpen" class="absolute left-0 right-0 top-full z-50 border-b border-border bg-surface shadow-lg md:hidden">
        <nav class="flex flex-col px-4 py-3">
          <RouterLink to="/" class="mobile-nav-item" @click="mobileNavOpen = false">Domů</RouterLink>
          <RouterLink to="/registrace" class="mobile-nav-item" @click="mobileNavOpen = false">Přihláška</RouterLink>
          <RouterLink to="/vysledky/2026" class="mobile-nav-item" @click="mobileNavOpen = false">Výsledky</RouterLink>
          <RouterLink to="/archiv" class="mobile-nav-item" @click="mobileNavOpen = false">Archiv</RouterLink>

          <template v-if="!isLoggedIn">
            <hr class="my-2 border-border" />
            <RouterLink to="/admin/login" class="btn-primary text-center no-underline py-3" @click="mobileNavOpen = false">Přihlásit</RouterLink>
          </template>

          <template v-else>
            <hr class="my-2 border-border" />
            <div class="text-label text-text-soft px-3 py-1">{{ name }}<span v-if="username" class="ml-1 text-text-soft">@{{ username }}</span></div>
            <template v-if="hasAdmin">
              <div class="mobile-nav-section">Administrace</div>
              <RouterLink to="/admin/prihlaseni" class="mobile-nav-item" @click="mobileNavOpen = false">Přihlášky</RouterLink>
              <RouterLink to="/admin/varianty" class="mobile-nav-item" @click="mobileNavOpen = false">Závod</RouterLink>
              <RouterLink to="/admin/program" class="mobile-nav-item" @click="mobileNavOpen = false">Program</RouterLink>
              <RouterLink to="/admin/kategorie" class="mobile-nav-item" @click="mobileNavOpen = false">Kategorie</RouterLink>
              <RouterLink :to="`/ceremoniál/${new Date().getFullYear()}`" class="mobile-nav-item" @click="mobileNavOpen = false">Ceremoniál</RouterLink>
              <RouterLink to="/admin/trasy" class="mobile-nav-item" @click="mobileNavOpen = false">Trasy</RouterLink>
              <RouterLink to="/admin/stanoviste" class="mobile-nav-item" @click="mobileNavOpen = false">Stanoviště</RouterLink>
              <RouterLink to="/admin/bodovani" class="mobile-nav-item" @click="mobileNavOpen = false">Bodování</RouterLink>
              <RouterLink to="/admin/incidenty" class="mobile-nav-item" @click="mobileNavOpen = false">Úkoly pro pořadatele</RouterLink>
              <RouterLink to="/admin/komunikace" class="mobile-nav-item" @click="mobileNavOpen = false">Komunikace</RouterLink>
              <RouterLink to="/admin/logovani" class="mobile-nav-item" @click="mobileNavOpen = false">Logování</RouterLink>
              <RouterLink to="/admin/changelog" class="mobile-nav-item" @click="mobileNavOpen = false">ChangeLog</RouterLink>
              <RouterLink to="/admin/role" class="mobile-nav-item" @click="mobileNavOpen = false">Role</RouterLink>
              <RouterLink to="/admin/uzivatele" class="mobile-nav-item" @click="mobileNavOpen = false">Uživatelé</RouterLink>
              <button @click="emit('impersonate'); mobileNavOpen = false" class="mobile-nav-item w-full text-left">Přihlásit jako</button>
            </template>
            <template v-if="hasJudge">
              <div class="mobile-nav-section">Komisař</div>
              <RouterLink to="/komisari/prehled" class="mobile-nav-item" @click="mobileNavOpen = false">Přehled</RouterLink>
              <RouterLink to="/komisari" class="mobile-nav-item" @click="mobileNavOpen = false">Bodování</RouterLink>
            </template>
            <template v-if="hasRacer">
              <div class="mobile-nav-section">Závodník</div>
              <RouterLink to="/zavodnik/itinerar" class="mobile-nav-item" @click="mobileNavOpen = false">Itinerář</RouterLink>
              <RouterLink to="/zavodnik/mapa" class="mobile-nav-item" @click="mobileNavOpen = false">Mapa</RouterLink>
              <RouterLink to="/zavodnik/stav" class="mobile-nav-item" @click="mobileNavOpen = false">Můj stav</RouterLink>
              <RouterLink to="/zavodnik/vozidla" class="mobile-nav-item" @click="mobileNavOpen = false">Vozový park</RouterLink>
              <RouterLink to="/zavodnik/ukoly" class="mobile-nav-item" @click="mobileNavOpen = false">Moje úkoly</RouterLink>
            </template>
            <hr class="my-2 border-border" />
            <RouterLink to="/ucet" class="mobile-nav-item" @click="mobileNavOpen = false">Nastavení účtu</RouterLink>
            <button @click="onLogout" class="mobile-nav-item w-full text-left text-text-soft">Odhlásit</button>
          </template>
        </nav>
      </div>
    </transition>
  </header>
</template>
