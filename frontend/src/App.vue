<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
import logoCtverec from '@/assets/logo_ctverec.png'
import logoPrevit from '@/assets/logo_previt.png'

const route = useRoute()
const { isLoggedIn, name, username, hasAdmin, hasJudge, hasRacer, logout } = useAuth()

const appInfo = ref<{ version: string; deployedAt: string; changelog: { version: string; description: string; createdAt: string }[] } | null>(null)
const showInfo = ref(false)
const infoRef = ref<HTMLElement | null>(null)

const isLoginPage = computed(() => route.path === '/admin/login')
const showAdminSidebar = computed(() => isLoggedIn.value && route.path.startsWith('/admin') && !isLoginPage.value)

function onLogout() {
  logout()
}

function onDocumentClick(e: MouseEvent) {
  if (showInfo.value && infoRef.value && !infoRef.value.contains(e.target as Node)) {
    showInfo.value = false
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))

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
</script>

<template>
  <div class="min-h-screen bg-bg flex flex-col">
    <!-- Top bar -->
    <header class="h-18 border-b border-border bg-surface shrink-0">
      <div class="mx-auto flex h-full max-w-wide items-center justify-between gap-4 px-4 lg:px-8">
        <RouterLink to="/" class="flex items-center gap-3 no-underline group">
          <img
            :src="logoCtverec"
            alt="Novobydžovský čtverec"
            class="h-10 w-auto object-contain"
          />
          <div class="flex flex-col leading-tight">
            <span class="font-display text-2xl tracking-[0.04em] text-text">Novobydžovský</span>
            <span class="font-display text-2xl tracking-[0.04em] text-primary -mt-1">Čtverec</span>
          </div>
        </RouterLink>
        <nav class="flex items-center gap-1">
          <RouterLink to="/" class="px-3 py-2 text-label text-text-muted no-underline transition-colors hover:text-primary" active-class="text-primary bg-primary/10 rounded-md">
            Domů
          </RouterLink>
          <RouterLink to="/registrace" class="px-3 py-2 text-label text-text-muted no-underline transition-colors hover:text-primary" active-class="text-primary bg-primary/10 rounded-md">
            Přihláška
          </RouterLink>
          <RouterLink to="/vysledky/2026" class="px-3 py-2 text-label text-text-muted no-underline transition-colors hover:text-primary" active-class="text-primary bg-primary/10 rounded-md">
            Výsledky
          </RouterLink>
          <RouterLink to="/archiv" class="px-3 py-2 text-label text-text-muted no-underline transition-colors hover:text-primary" active-class="text-primary bg-primary/10 rounded-md">
            Archiv
          </RouterLink>

          <RouterLink
            v-if="!isLoggedIn"
            to="/admin/login"
            class="btn-primary btn-sm ml-2 no-underline"
          >
            Přihlásit
          </RouterLink>

          <div v-else class="relative ml-2 flex items-center">
            <div class="group relative">
              <button
                class="flex items-center gap-2 rounded-pill border border-border bg-surface px-4 py-1.5 text-label text-text-muted transition-colors hover:bg-bg-alt"
              >
                <svg class="h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                <span>{{ name }}<span v-if="username" class="ml-1 text-text-soft text-meta">@{{ username }}</span></span>
                <svg class="h-3 w-3 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>
              <div class="invisible absolute right-0 top-full z-40 mt-1 w-56 origin-top-right scale-95 rounded-xl border border-border bg-surface py-1 shadow-md transition-all duration-160 ease-out group-hover:visible group-hover:scale-100">
                <template v-if="hasAdmin">
                  <div class="admin-sidebar-section">Administrace</div>
                  <RouterLink to="/admin/prihlaseni" class="admin-sidebar-item">Přihlášky</RouterLink>
                  <RouterLink to="/admin/stanoviste" class="admin-sidebar-item">Stanoviště</RouterLink>
                  <RouterLink to="/admin/komunikace" class="admin-sidebar-item">Komunikace</RouterLink>
                  <RouterLink to="/admin/logovani" class="admin-sidebar-item">Logování</RouterLink>
                  <RouterLink to="/admin/role" class="admin-sidebar-item">Role</RouterLink>
                  <RouterLink to="/admin/uzivatele" class="admin-sidebar-item">Uživatelé</RouterLink>
                  <hr class="my-1 mx-3 border-border" />
                </template>
                <template v-if="hasJudge">
                  <RouterLink to="/komisari" class="admin-sidebar-item">Komisaři</RouterLink>
                </template>
                <template v-if="hasRacer">
                  <div class="admin-sidebar-section">Závodník</div>
                  <RouterLink to="/zavodnik/itinerar" class="admin-sidebar-item">Itinerář</RouterLink>
                  <RouterLink to="/zavodnik/mapa" class="admin-sidebar-item">Mapa</RouterLink>
                  <RouterLink to="/zavodnik/stav" class="admin-sidebar-item">Můj stav</RouterLink>
                </template>
                <hr class="my-1 mx-3 border-border" />
                <RouterLink to="/ucet" class="admin-sidebar-item">Nastavení účtu</RouterLink>
                <button @click="onLogout" class="admin-sidebar-item w-full text-left text-text-soft">Odhlásit</button>
              </div>
            </div>

            <span ref="infoRef" class="relative ml-1">
              <button @click.stop="toggleInfo" class="flex h-8 w-8 items-center justify-center rounded-md text-text-soft transition-colors hover:bg-bg-alt hover:text-text-muted" title="Info o aplikaci">
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </button>
              <div
                v-if="showInfo"
                @click.stop
                class="absolute right-0 top-full z-50 mt-1 w-72 rounded-xl border border-border bg-surface p-5 shadow-lg"
              >
                <p class="text-meta text-text-soft">verze {{ appInfo?.version ?? '…' }}</p>
                <p class="mt-1 text-meta text-text-soft">nasazeno {{ appInfo?.deployedAt ? new Date(appInfo.deployedAt).toLocaleString('cs') : '…' }}</p>
                <div v-if="hasAdmin && appInfo?.changelog?.length" class="mt-4 border-t border-border pt-4">
                  <p class="mb-3 text-meta font-semibold text-text-muted">ChangeLog</p>
                  <div v-for="(entry, i) in appInfo.changelog" :key="i" class="mb-3 last:mb-0">
                    <p class="text-label text-primary">{{ entry.version }}</p>
                    <p class="mt-0.5 text-body-sm text-text-muted">{{ entry.description }}</p>
                    <p class="text-meta text-text-soft">{{ new Date(entry.createdAt).toLocaleDateString('cs') }}</p>
                  </div>
                </div>
                <button @click="showInfo = false" class="mt-3 text-label text-primary hover:text-primary-hover">Zavřít</button>
              </div>
            </span>
          </div>
        </nav>
      </div>
    </header>

    <!-- Main content area -->
    <div class="mx-auto w-full max-w-wide flex-1 flex flex-col">
      <div v-if="showAdminSidebar" class="flex flex-1">
        <aside class="hidden w-56 shrink-0 lg:block admin-sidebar">
          <nav class="py-4">
            <div class="admin-sidebar-section">Administrace</div>
            <RouterLink to="/admin/prihlaseni" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Přihlášky</RouterLink>
            <RouterLink to="/admin/stanoviste" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Stanoviště</RouterLink>
            <RouterLink to="/admin/komunikace" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Komunikace</RouterLink>
            <RouterLink to="/admin/logovani" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Logování</RouterLink>
            <RouterLink to="/admin/role" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Role</RouterLink>
            <RouterLink to="/admin/uzivatele" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Uživatelé</RouterLink>
          </nav>
        </aside>
        <main class="flex-1 px-4 lg:px-8 py-8">
          <RouterView />
        </main>
      </div>
      <main v-else class="flex-1 px-4 lg:px-8 py-8 lg:py-10" :class="isLoginPage ? 'max-w-form mx-auto w-full' : 'max-w-content mx-auto w-full'">
        <RouterView />
      </main>
    </div>

    <!-- Footer -->
    <footer class="border-t-2 border-accent-gold/20 bg-surface mt-12 shrink-0" style="border-top-color: rgba(184,138,59,0.2)">
      <div class="mx-auto max-w-wide px-4 lg:px-8 py-6">
        <div class="flex flex-col sm:flex-row items-start justify-between gap-6">
          <div class="text-meta text-text-soft space-y-1">
            <p>&copy; {{ new Date().getFullYear() }} Novobydžovský čtverec</p>
            <p>Novobydžovský čtverec – Klub přátel historických vozidel, z.s.</p>
            <p>V. Kl. Klicpery 624, 504 01 Nový Bydžov</p>
            <p>IČO: 26630061 &middot; Účet: 1086360369/0800</p>
            <a
              href="#"
              class="block mt-2 text-meta text-text-soft hover:text-primary no-underline transition-colors"
              title="Helpdesk – technická podpora"
            >HELPDESK</a>
          </div>
          <a
            href="https://pre-vit.cz"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-2 no-underline text-meta text-text-soft hover:text-primary transition-colors group shrink-0"
            title="Previt – vývojář a správce aplikace"
          >
            <span>vytvořil</span>
            <img
              :src="logoPrevit"
              alt="Previt"
              class="h-5 w-auto object-contain opacity-60 group-hover:opacity-100 transition-opacity"
            />
          </a>
        </div>
      </div>
    </footer>
  </div>
</template>
