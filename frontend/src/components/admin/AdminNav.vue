<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { name, logout } = useAuth()
const open = ref(false)

function onLogout() {
  open.value = false
  logout()
  router.push('/admin/login')
}

function goto(path: string) {
  open.value = false
  router.push(path)
}
</script>

<template>
  <div class="relative" @mouseenter="open = true" @mouseleave="open = false">
    <button
      class="flex items-center gap-2 rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
    >
      <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
      </svg>
      {{ name }}
      <svg class="h-3 w-3 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <div
      v-if="open"
      class="absolute right-0 top-full z-50 mt-1 w-48 rounded-lg border border-slate-700 bg-slate-900 py-1 shadow-xl"
    >
      <button @click="goto('/admin')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Přihlášky
      </button>
      <button @click="goto('/admin/stanoviste')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Stanoviště
      </button>
      <button @click="goto('/admin/komunikace')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Komunikace
      </button>
      <button @click="goto('/admin/logovani')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Logování
      </button>
      <button @click="goto('/admin/role')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Role
      </button>
      <button @click="goto('/admin/uzivatele')" class="block w-full px-4 py-2 text-left text-sm text-slate-300 transition hover:bg-slate-800">
        Uživatelé
      </button>
      <hr class="my-1 border-slate-700" />
      <button @click="onLogout" class="block w-full px-4 py-2 text-left text-sm text-slate-500 transition hover:bg-slate-800">
        Odhlásit
      </button>
    </div>
  </div>
</template>
