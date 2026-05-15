<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { isLoggedIn, name, hasAdmin, hasJudge, hasRacer, logout } = useAuth()

function onLogout() {
  logout()
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen">
    <header class="border-b border-slate-800 bg-slate-900/80 backdrop-blur">
      <div class="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-4">
        <RouterLink to="/" class="text-lg font-semibold tracking-tight text-amber-400">
          Novobydžovský čtverec
        </RouterLink>
        <div class="flex items-center gap-3 text-sm text-slate-400">
          <RouterLink to="/" class="hover:text-slate-200" active-class="text-amber-300">
            Domů
          </RouterLink>
          <RouterLink to="/registrace" class="hover:text-slate-200" active-class="text-amber-300">
            Přihláška
          </RouterLink>
          <RouterLink to="/vysledky/2026" class="hover:text-slate-200" active-class="text-amber-300">
            Výsledky
          </RouterLink>
          <RouterLink to="/archiv" class="hover:text-slate-200" active-class="text-amber-300">
            Archiv
          </RouterLink>
          <RouterLink
            v-if="!isLoggedIn"
            to="/admin/login"
            class="rounded-lg border border-slate-700 px-3 py-1.5 text-slate-300 transition hover:bg-slate-800"
          >
            Přihlásit
          </RouterLink>
          <div
            v-else
            class="group relative"
          >
            <button
              class="flex items-center gap-1.5 rounded-lg border border-slate-700 px-3 py-1.5 text-slate-300 transition hover:bg-slate-800"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              {{ name }}
            </button>
            <div
              class="absolute right-0 top-full z-50 mt-1 hidden w-48 rounded-lg border border-slate-700 bg-slate-900 py-1 shadow-xl group-hover:block"
            >
              <template v-if="hasAdmin">
                <RouterLink to="/admin/prihlaseni" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Přihlášky
                </RouterLink>
                <RouterLink to="/admin/stanoviste" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Stanoviště
                </RouterLink>
                <RouterLink to="/admin/komunikace" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Komunikace
                </RouterLink>
                <RouterLink to="/admin/logovani" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Logování
                </RouterLink>
                <RouterLink to="/admin/role" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Role
                </RouterLink>
                <RouterLink to="/admin/uzivatele" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Uživatelé
                </RouterLink>
                <hr class="my-1 border-slate-700" />
              </template>
              <RouterLink v-if="hasJudge" to="/rozhodci" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                Rozhodčí
              </RouterLink>
              <template v-if="hasRacer">
                <RouterLink to="/zavodnik/itinerar" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Itinerář
                </RouterLink>
                <RouterLink to="/zavodnik/mapa" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Mapa
                </RouterLink>
                <RouterLink to="/zavodnik/stav" class="block px-4 py-2 text-sm text-slate-300 hover:bg-slate-800">
                  Můj stav
                </RouterLink>
              </template>
              <hr v-if="hasJudge || hasRacer" class="my-1 border-slate-700" />
              <button @click="onLogout" class="block w-full px-4 py-2 text-left text-sm text-slate-500 hover:bg-slate-800">
                Odhlásit
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-3xl px-4 py-10">
      <RouterView />
    </main>
  </div>
</template>
