<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const router = useRouter()
const { isAdmin, loginRequest } = useAuth()

const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function handleLogin() {
  error.value = null
  loading.value = true
  try {
    await loginRequest(username.value, password.value)
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
  <div>
    <div class="card">
      <div class="text-center mb-8">
        <p class="text-meta text-text-soft uppercase tracking-[0.12em]">Administrace</p>
        <h1 class="text-page-title text-text mt-1">Přihlášení</h1>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-5">
        <div>
          <label class="input-label">Uživatelské jméno nebo e-mail</label>
          <input v-model="username" required class="input-field" />
        </div>
        <div>
          <label class="input-label">Heslo</label>
          <input v-model="password" type="password" required class="input-field" />
        </div>
        <p v-if="error" class="text-body-sm text-error">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-primary w-full">
          {{ loading ? 'Přihlašuji…' : 'Přihlásit' }}
        </button>
      </form>
    </div>
  </div>
</template>
