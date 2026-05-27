<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: { client_id: string; callback: (res: { credential: string }) => void }) => void
          renderButton: (el: HTMLElement, options: { theme: string; size: string; text?: string; width?: number }) => void
          prompt: () => void
        }
      }
    }
  }
}

const router = useRouter()
const { isAdmin, loginRequest, googleLogin } = useAuth()

const username = ref('')
const password = ref('')
const error = ref<string | null>(null)
const googleError = ref<string | null>(null)
const loading = ref(false)
const googleBtnContainer = ref<HTMLElement | null>(null)

const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? ''

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

async function handleGoogleResponse(response: { credential: string }) {
  googleError.value = null
  try {
    await googleLogin(response.credential)
    router.push('/')
  } catch (e) {
    googleError.value = e instanceof Error ? e.message : 'Google přihlášení selhalo'
  }
}

onMounted(() => {
  if (isAdmin.value) {
    router.push('/admin/prihlaseni')
    return
  }
  if (!clientId) return
  const script = document.createElement('script')
  script.src = 'https://accounts.google.com/gsi/client'
  script.async = true
  script.onload = () => {
    if (!window.google?.accounts?.id) return
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: handleGoogleResponse,
    })
    if (googleBtnContainer.value) {
      window.google.accounts.id.renderButton(googleBtnContainer.value, {
        theme: 'outline',
        size: 'large',
        text: 'signin_with',
        width: 320,
      })
    }
  }
  document.head.appendChild(script)
})

onUnmounted(() => {
  const scripts = document.querySelectorAll('script[src="https://accounts.google.com/gsi/client"]')
  scripts.forEach(s => s.remove())
})
</script>

<template>
  <div class="relative">
    <div class="absolute inset-0 opacity-[0.03] pointer-events-none select-none"
      style="background-image:
        linear-gradient(45deg, var(--text) 25%, transparent 25%),
        linear-gradient(-45deg, var(--text) 25%, transparent 25%),
        linear-gradient(45deg, transparent 75%, var(--text) 75%),
        linear-gradient(-45deg, transparent 75%, var(--text) 75%);
        background-size: 40px 40px;
        background-position: 0 0, 0 20px, 20px -20px, -20px 0px;
      "
    ></div>
    <div class="card relative">
      <div class="text-center mb-6">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary">
          <svg class="h-7 w-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
          </svg>
        </div>
        <p class="text-meta text-text-soft uppercase tracking-[0.12em]">Administrace</p>
        <h1 class="text-page-title text-text mt-1">Přihlášení</h1>
      </div>

      <div v-if="clientId" class="flex justify-center mb-6 overflow-hidden">
        <div ref="googleBtnContainer" class="max-w-full"></div>
      </div>
      <p v-if="googleError" class="text-body-sm text-error text-center mb-4">{{ googleError }}</p>

      <div v-if="clientId" class="relative mb-6">
        <div class="absolute inset-0 flex items-center">
          <div class="w-full border-t border-border"></div>
        </div>
        <div class="relative flex justify-center text-body-sm">
          <span class="bg-surface px-3 text-text-soft">nebo</span>
        </div>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-5">
        <div>
          <label class="input-label">Uživatelské jméno nebo e-mail</label>
          <input v-model="username" required class="input-field" autocomplete="username" />
        </div>
        <div>
          <label class="input-label">Heslo</label>
          <input v-model="password" type="password" required class="input-field" autocomplete="current-password" />
        </div>
        <p v-if="error" class="text-body-sm text-error">{{ error }}</p>
        <button type="submit" :disabled="loading" class="btn-primary w-full">
          <svg v-if="loading" class="h-4 w-4 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ loading ? 'Přihlašuji…' : 'Přihlásit' }}
        </button>
      </form>
    </div>
  </div>
</template>
