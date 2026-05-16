<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import { apiBaseUrl } from '@/api'
import logoCtverec from '@/assets/logo_ctverec.png'
import logoPrevit from '@/assets/logo_previt.png'
import ImpersonateModal from '@/components/ImpersonateModal.vue'

const route = useRoute()
const { isLoggedIn, name, username, hasAdmin, hasJudge, hasRacer, impersonating, logout, restoreFromImpersonation } = useAuth()
const { toasts, remove } = useToast()

const appInfo = ref<{ version: string; deployedAt: string; changelog: { version: string; description: string; createdAt: string }[] } | null>(null)
const showInfo = ref(false)
const showImpersonateModal = ref(false)
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
    <!-- Toast container -->
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-sm">
      <div v-for="t in toasts" :key="t.id"
        class="flex items-start gap-3 rounded-xl border px-4 py-3 shadow-lg transition-all animate-slide-in cursor-pointer"
        :class="{
          'bg-success/10 border-success text-success': t.type === 'success',
          'bg-error/10 border-error text-error': t.type === 'error',
          'bg-info/10 border-info text-info': t.type === 'info',
        }"
        @click="remove(t.id)"
      >
        <span class="text-body-sm flex-1">{{ t.message }}</span>
        <button @click.stop="remove(t.id)" class="text-current opacity-60 hover:opacity-100 leading-none">&times;</button>
      </div>
    </div>

    <!-- Impersonation bar -->
    <div v-if="impersonating" class="bg-accent-gold/20 border-b border-accent-gold/30 px-4 py-2 text-center text-body-sm text-text">
      <span class="font-semibold">Impersonace:</span> {{ name }} (@{{ username }})
      <button @click="restoreFromImpersonation" class="ml-3 btn-primary btn-xs">Zpět</button>
    </div>
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
            <span class="text-meta text-accent-gold -mt-0.5">Memoriál Elišky Junkové</span>
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
                  <RouterLink to="/admin/trasy" class="admin-sidebar-item">Trasy</RouterLink>
                  <RouterLink to="/admin/stanoviste" class="admin-sidebar-item">Stanoviště</RouterLink>
                  <RouterLink to="/admin/komunikace" class="admin-sidebar-item">Komunikace</RouterLink>
                  <RouterLink to="/admin/changelog" class="admin-sidebar-item">ChangeLog</RouterLink>
                  <RouterLink to="/admin/logovani" class="admin-sidebar-item">Logování</RouterLink>
                  <RouterLink to="/admin/role" class="admin-sidebar-item">Role</RouterLink>
                  <RouterLink to="/admin/uzivatele" class="admin-sidebar-item">Uživatelé</RouterLink>
                  <button @click="showImpersonateModal = true" class="admin-sidebar-item w-full text-left">Přihlásit jako</button>
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
            <RouterLink to="/admin/trasy" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Trasy</RouterLink>
            <RouterLink to="/admin/stanoviste" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Stanoviště</RouterLink>
            <RouterLink to="/admin/komunikace" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">Komunikace</RouterLink>
            <RouterLink to="/admin/changelog" class="admin-sidebar-item" active-class="!bg-surface !border-l-primary !text-primary">ChangeLog</RouterLink>
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
    <footer class="bg-surface-strong mt-12 shrink-0">
      <div class="h-0.5 w-full" style="background: linear-gradient(90deg, var(--accent-gold), var(--primary), var(--accent-gold))"></div>
      <div class="mx-auto max-w-wide px-4 lg:px-8 py-10">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- Club info -->
          <div>
            <div class="flex items-center gap-2 mb-3">
              <img :src="logoCtverec" alt="" class="h-7 w-auto object-contain opacity-60" />
              <span class="font-display text-lg tracking-[0.04em] text-text">Novobydžovský<br />Čtverec</span>
            <span class="text-meta text-accent-gold/60">Memoriál Elišky Junkové</span>
            </div>
            <p class="text-body-sm text-text-muted leading-relaxed">
              Klub přátel historických vozidel, z.s.
            </p>
            <p class="mt-2 text-body-sm text-text-soft leading-relaxed">
              V. Kl. Klicpery 624<br />
              504 01 Nový Bydžov
            </p>
          </div>

          <!-- Contact & bank -->
          <div>
            <h4 class="text-label text-text mb-3">Kontaktní údaje</h4>
            <div class="space-y-2 text-body-sm text-text-muted">
              <p><span class="text-text-soft">IČO:</span> 26630061</p>
              <p><span class="text-text-soft">Bankovní účet:</span><br />
                <span class="font-mono text-text">1086360369/0800</span>
              </p>
              <a
                href="#"
                class="inline-flex items-center gap-1.5 mt-2 rounded-md border border-accent-gold/30 px-3 py-1.5 text-meta text-accent-gold no-underline hover:bg-accent-gold/10 transition-colors"
                title="Helpdesk – technická podpora"
              >
                <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                HELPDESK
              </a>
            </div>
          </div>

          <!-- Built by -->
          <div class="flex flex-col items-start sm:items-end justify-between">
            <div>
              <p class="text-meta text-text-soft mb-2">&copy; {{ new Date().getFullYear() }}</p>
            </div>
            <a
              href="https://pre-vit.cz"
              target="_blank"
              rel="noopener noreferrer"
              class="flex items-center gap-2 no-underline text-body-sm text-text-muted hover:text-primary transition-colors group"
              title="Previt – vývojář a správce aplikace"
            >
              <span>vytvořil</span>
              <img
                :src="logoPrevit"
                alt="Previt"
                class="h-6 w-auto object-contain opacity-60 group-hover:opacity-100 transition-opacity"
              />
            </a>
          </div>
        </div>
      </div>
    </footer>

    <ImpersonateModal
      v-if="showImpersonateModal"
      @close="showImpersonateModal = false"
    />
  </div>
</template>

<style>
@keyframes slide-in {
  from { opacity: 0; transform: translateX(100%); }
  to { opacity: 1; transform: translateX(0); }
}
.animate-slide-in {
  animation: slide-in 0.3s ease-out;
}
</style>
