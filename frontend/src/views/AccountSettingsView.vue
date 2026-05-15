<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, fetchAccount } from '@/api'

const router = useRouter()
const { isLoggedIn, logout, authHeaders } = useAuth()

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
  <div class="mx-auto max-w-sm">
    <h1 class="text-2xl font-bold text-white">Nastavení účtu</h1>
    <p class="mt-1 text-sm text-slate-400">Upravte své základní údaje</p>

    <div v-if="loading" class="mt-8 text-slate-500">Načítám…</div>

    <form v-else @submit.prevent="save" class="mt-6 space-y-4">
      <div>
        <label class="block text-xs text-slate-500">Uživatelské jméno</label>
        <div class="mt-1 text-sm text-slate-400">{{ form.username }}</div>
      </div>
      <div class="flex gap-2">
        <div class="flex-1">
          <label class="block text-xs text-slate-500">Jméno</label>
          <input v-model="form.firstName" required
            class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
        </div>
        <div class="flex-1">
          <label class="block text-xs text-slate-500">Příjmení</label>
          <input v-model="form.lastName"
            class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
        </div>
      </div>
      <div>
        <label class="block text-xs text-slate-500">Email</label>
        <input v-model="form.email" type="email" required
          class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
      </div>
      <div>
        <label class="block text-xs text-slate-500">Telefon</label>
        <input v-model="form.phone" type="tel"
          class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
      </div>
      <div v-if="form.memberSince">
        <label class="block text-xs text-slate-500">V klubu od</label>
        <div class="mt-1 text-sm text-slate-400">{{ form.memberSince }} ({{ memberDuration }} dní)</div>
      </div>
      <p v-if="error" class="text-sm text-red-400">{{ error }}</p>
      <p v-if="success" class="text-sm text-emerald-400">Údaje uloženy</p>
      <button type="submit" :disabled="saving"
        class="w-full rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50">
        {{ saving ? 'Ukládám…' : 'Uložit' }}
      </button>
    </form>

    <hr class="my-8 border-slate-800" />

    <h2 class="text-lg font-semibold text-white">Změna hesla</h2>
    <form @submit.prevent="changePassword" class="mt-4 space-y-4">
      <div>
        <label class="block text-xs text-slate-500">Současné heslo</label>
        <input v-model="pwForm.currentPassword" type="password" required
          class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
      </div>
      <div>
        <label class="block text-xs text-slate-500">Nové heslo</label>
        <input v-model="pwForm.newPassword" type="password" required minlength="6"
          class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
      </div>
      <div>
        <label class="block text-xs text-slate-500">Potvrzení nového hesla</label>
        <input v-model="pwForm.confirmPassword" type="password" required
          class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
      </div>
      <p v-if="pwError" class="text-sm text-red-400">{{ pwError }}</p>
      <p v-if="pwSuccess" class="text-sm text-emerald-400">Heslo změněno</p>
      <button type="submit" :disabled="pwSaving"
        class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50">
        {{ pwSaving ? 'Měním…' : 'Změnit heslo' }}
      </button>
    </form>
  </div>
</template>
