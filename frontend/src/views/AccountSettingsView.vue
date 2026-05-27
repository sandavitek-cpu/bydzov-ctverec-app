<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { apiBaseUrl, fetchAccount } from '@/api'

const router = useRouter()
const { isLoggedIn, authHeaders } = useAuth()

const form = ref({ username: '', firstName: '', lastName: '', email: '', phone: '', memberSince: '' })
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

const pwForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })

const memberDuration = computed(() => {
  if (!form.value.memberSince) return 0
  const since = new Date(form.value.memberSince)
  const now = new Date()
  const diff = now.getTime() - since.getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24))
})

const memberLabel = computed(() => {
  const d = memberDuration.value
  if (d === 1) return '1 den'
  if (d >= 2 && d <= 4) return `${d} dny`
  return `${d} dní`
})
const pwSaving = ref(false)
const pwError = ref<string | null>(null)
const pwSuccess = ref(false)

if (!isLoggedIn.value) {
  router.push('/admin/login')
}

onMounted(async () => {
  try {
    const data = await fetchAccount(authHeaders())
    form.value = { username: data.username, firstName: data.firstName, lastName: data.lastName ?? '', email: data.email, phone: data.phone ?? '', memberSince: data.memberSince ?? '' }
  } catch {
    error.value = 'Nepodařilo se načíst účet'
  } finally {
    loading.value = false
  }
})

async function save() {
    saving.value = true
    error.value = null
    success.value = false
    try {
        const res = await fetch(`${apiBaseUrl}/api/account`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json', ...authHeaders() },
            body: JSON.stringify({ firstName: form.value.firstName, lastName: form.value.lastName, email: form.value.email, phone: form.value.phone }),
        })
        const data = await res.json()
        if (!res.ok) throw new Error(data.error ?? 'Chyba uložení')
        success.value = true
        form.value = { 
            username: data.username, 
            firstName: data.firstName, 
            lastName: data.lastName ?? '', 
            email: data.email, 
            phone: data.phone ?? '', 
            memberSince: data.memberSince ?? '' 
        }
    } catch (e) {
        error.value = e instanceof Error ? e.message : 'Chyba uložení'
    } finally {
        saving.value = false
    }
}

async function changePassword() {
  if (pwForm.value.newPassword !== pwForm.value.confirmPassword) {
    pwError.value = 'Hesla se neshodují'
    return
  }
  pwSaving.value = true
  pwError.value = null
  pwSuccess.value = false
  try {
    const res = await fetch(`${apiBaseUrl}/api/account/password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ currentPassword: pwForm.value.currentPassword, newPassword: pwForm.value.newPassword }),
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.error ?? 'Chyba')
    pwSuccess.value = true
    pwForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  } catch (e) {
    pwError.value = e instanceof Error ? e.message : 'Chyba změny hesla'
  } finally {
    pwSaving.value = false
  }
}
</script>

<template>
  <div class="max-w-form">
    <h1 class="text-page-title text-text">Nastavení účtu</h1>
    <p class="mt-2 text-body-lg text-text-muted">Upravte své základní údaje</p>

    <LoadingSpinner v-if="loading" text="Načítám účet…" />

    <!-- Profile form -->
    <form v-else @submit.prevent="save" class="mt-6 space-y-6">
      <div class="card space-y-5">
        <h2 class="text-subsection text-text">Profil</h2>

        <div>
          <label class="input-label">Uživatelské jméno</label>
          <p class="text-body text-text-muted border border-border rounded-md bg-bg-alt px-4 py-2.5 flex items-center gap-2">
            <svg class="h-4 w-4 text-text-soft shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
            {{ form.username }}
          </p>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="input-label">Jméno</label>
            <input v-model="form.firstName" required class="input-field" />
          </div>
          <div>
            <label class="input-label">Příjmení</label>
            <input v-model="form.lastName" class="input-field" />
          </div>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="input-label">Email</label>
            <input v-model="form.email" type="email" required class="input-field" />
          </div>
          <div>
            <label class="input-label">Telefon</label>
            <input v-model="form.phone" type="tel" class="input-field" />
          </div>
        </div>
        <div v-if="form.memberSince" class="rounded-lg bg-primary/5 border border-primary/20 px-4 py-3">
          <p class="text-meta text-primary uppercase tracking-[0.05em]">V klubu od</p>
          <p class="text-body text-text mt-1">{{ form.memberSince }} <span class="text-text-soft">({{ memberLabel }})</span></p>
        </div>
        <p v-if="error" class="text-body-sm text-error flex items-center gap-1">
          <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          {{ error }}
        </p>
        <p v-if="success" class="text-body-sm text-success flex items-center gap-1">
          <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          Údaje uloženy
        </p>
        <button type="submit" :disabled="saving" class="btn-primary w-full">
          <svg v-if="saving" class="h-4 w-4 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ saving ? 'Ukládám…' : 'Uložit' }}
        </button>
      </div>

      <!-- Password change -->
      <div class="card space-y-5">
        <h2 class="text-subsection text-text">Změna hesla</h2>
        <div>
          <label class="input-label">Současné heslo</label>
          <input v-model="pwForm.currentPassword" type="password" required class="input-field" autocomplete="current-password" />
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="input-label">Nové heslo</label>
            <input v-model="pwForm.newPassword" type="password" required minlength="6" class="input-field" autocomplete="new-password" />
          </div>
          <div>
            <label class="input-label">Potvrzení nového hesla</label>
            <input v-model="pwForm.confirmPassword" type="password" required class="input-field" autocomplete="new-password" />
          </div>
        </div>
        <p v-if="pwError" class="text-body-sm text-error flex items-center gap-1">
          <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          {{ pwError }}
        </p>
        <p v-if="pwSuccess" class="text-body-sm text-success flex items-center gap-1">
          <svg class="h-4 w-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          Heslo změněno
        </p>
        <button type="submit" :disabled="pwSaving" class="btn-primary">
          <svg v-if="pwSaving" class="h-4 w-4 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          {{ pwSaving ? 'Měním…' : 'Změnit heslo' }}
        </button>
      </div>
    </form>
  </div>
</template>
