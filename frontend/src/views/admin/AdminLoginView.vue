<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { isAdmin, loginRequest } = useAuth()

const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function handleLogin() {
  error.value = null
  loading.value = true
  try {
    await loginRequest(email.value, password.value)
    router.push('/admin/prihlaseni')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba přihlášení'
  } finally {
    loading.value = false
  }
}

if (isAdmin.value) {
  router.push('/admin/prihlaseni')
}
</script>

<template>
  <div class="mx-auto max-w-sm">
    <h1 class="text-2xl font-bold text-white">Přihlášení pořadatele</h1>
    <p class="mt-1 text-sm text-slate-400">Admin rozhraní – JWT autentizace</p>

    <form @submit.prevent="handleLogin" class="mt-6 space-y-4">
      <div>
        <label class="block text-sm font-medium text-slate-300">E-mail</label>
        <input
          v-model="email"
          type="email"
          required
          class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-slate-300">Heslo</label>
        <input
          v-model="password"
          type="password"
          required
          class="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white focus:border-amber-500 focus:outline-none"
        />
      </div>
      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>
      <button
        type="submit"
        :disabled="loading"
        class="w-full rounded-lg bg-amber-500 px-4 py-3 font-semibold text-black transition hover:bg-amber-400 disabled:opacity-50"
      >
        {{ loading ? 'Přihlašuji…' : 'Přihlásit' }}
      </button>
    </form>
  </div>
</template>
