<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
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

    <div v-if="loading" class="mt-8 text-body text-text-soft">Načítám…</div>

    <!-- Profile form -->
    <form v-else @submit.prevent="save" class="mt-6 space-y-5">
      <div>
        <label class="input-label">Uživatelské jméno</label>
        <p class="text-body text-text-muted border border-border rounded-md bg-bg-alt px-4 py-2.5">{{ form.username }}</p>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="input-label">Jméno</label>
          <input v-model="form.firstName" required class="input-field" />
        </div>
        <div>
          <label class="input-label">Příjmení</label>
          <input v-model="form.lastName" class="input-field" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="input-label">Email</label>
          <input v-model="form.email" type="email" required class="input-field" />
        </div>
        <div>
          <label class="input-label">Telefon</label>
          <input v-model="form.phone" type="tel" class="input-field" />
        </div>
      </div>
      <div v-if="form.memberSince" class="card !p-4">
        <p class="text-meta text-text-soft uppercase tracking-[0.05em]">V klubu od</p>
        <p class="text-body text-text mt-1">{{ form.memberSince }} <span class="text-text-soft">({{ memberDuration }} dní)</span></p>
      </div>
      <p v-if="error" class="text-body-sm text-error">{{ error }}</p>
      <p v-if="success" class="text-body-sm text-success">Údaje uloženy</p>
      <button type="submit" :disabled="saving" class="btn-primary w-full">
        {{ saving ? 'Ukládám…' : 'Uložit' }}
      </button>
    </form>

    <hr class="my-10 border-border" />

    <!-- Password change -->
    <h2 class="text-section-title text-text">Změna hesla</h2>
    <form @submit.prevent="changePassword" class="mt-6 space-y-5 max-w-form">
      <div>
        <label class="input-label">Současné heslo</label>
        <input v-model="pwForm.currentPassword" type="password" required class="input-field" />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="input-label">Nové heslo</label>
          <input v-model="pwForm.newPassword" type="password" required minlength="6" class="input-field" />
        </div>
        <div>
          <label class="input-label">Potvrzení nového hesla</label>
          <input v-model="pwForm.confirmPassword" type="password" required class="input-field" />
        </div>
      </div>
      <p v-if="pwError" class="text-body-sm text-error">{{ pwError }}</p>
      <p v-if="pwSuccess" class="text-body-sm text-success">Heslo změněno</p>
      <button type="submit" :disabled="pwSaving" class="btn-primary">
        {{ pwSaving ? 'Měním…' : 'Změnit heslo' }}
      </button>
    </form>
  </div>
</template>
