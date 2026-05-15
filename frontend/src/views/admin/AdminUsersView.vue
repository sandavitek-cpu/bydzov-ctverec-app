<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, fetchAdminUsers } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const users = ref<any[]>([])
const roles = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const searchQuery = ref('')
const selectedUser = ref<any | null>(null)
const editFirstName = ref('')
const editLastName = ref('')
const editEmail = ref('')
const editPhone = ref('')
const editMemberSince = ref('')
const saving = ref(false)

const showCreate = ref(false)
const createForm = ref({ username: '', firstName: '', lastName: '', email: '', phone: '', memberSince: '', password: '' })

const pwNewPassword = ref('')
const pwError = ref<string | null>(null)
const pwSuccess = ref(false)
const pwSaving = ref(false)

const deleteConfirm = ref(false)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function loadUsers() {
  loading.value = true
  error.value = null
  try {
    users.value = await fetchAdminUsers(authHeaders())
  } catch (e) {
    if (e instanceof Error && e.message.includes('403')) { logout(); router.push('/admin/login'); return }
    error.value = 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/roles`, {
      headers: { ...authHeaders() },
    })
    if (res.ok) roles.value = await res.json()
  } catch { /* silent */ }
}

async function selectUser(id: number) {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${id}`, {
      headers: { ...authHeaders() },
    })
    if (res.ok) {
      selectedUser.value = await res.json()
      editFirstName.value = selectedUser.value.firstName
      editLastName.value = selectedUser.value.lastName ?? ''
      editEmail.value = selectedUser.value.email
      editPhone.value = selectedUser.value.phone ?? ''
      editMemberSince.value = selectedUser.value.memberSince ?? ''
      pwNewPassword.value = ''
      pwError.value = null
      pwSuccess.value = false
      deleteConfirm.value = false
    }
  } catch {
    error.value = 'Chyba načítání'
  }
}

function closeUser() {
  selectedUser.value = null
  saving.value = false
}

async function saveUser() {
  if (!selectedUser.value) return
  saving.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${selectedUser.value.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ firstName: editFirstName.value, lastName: editLastName.value, email: editEmail.value, phone: editPhone.value, memberSince: editMemberSince.value }),
    })
    if (!res.ok) { const body = await res.json(); throw new Error(body.error ?? 'Chyba uložení') }
    await selectUser(selectedUser.value.id)
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  } finally {
    saving.value = false
  }
}

async function createUser() {
  saving.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(createForm.value),
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.error ?? 'Chyba vytvoření')
    showCreate.value = false
    createForm.value = { username: '', firstName: '', lastName: '', email: '', phone: '', memberSince: '', password: '' }
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba vytvoření'
  } finally {
    saving.value = false
  }
}

async function deleteUser() {
  if (!selectedUser.value) return
  saving.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${selectedUser.value.id}`, {
      method: 'DELETE',
      headers: { ...authHeaders() },
    })
    if (!res.ok) throw new Error()
    await loadUsers()
    closeUser()
  } catch {
    error.value = 'Chyba smazání'
  } finally {
    saving.value = false
  }
}

async function setPassword() {
  if (!selectedUser.value) return
  pwSaving.value = true
  pwError.value = null
  pwSuccess.value = false
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${selectedUser.value.id}/password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ newPassword: pwNewPassword.value }),
    })
    const data = await res.json()
    if (!res.ok) throw new Error(data.error ?? 'Chyba')
    pwSuccess.value = true
    pwNewPassword.value = ''
  } catch (e) {
    pwError.value = e instanceof Error ? e.message : 'Chyba změny hesla'
  } finally {
    pwSaving.value = false
  }
}

async function addRole(roleId: number) {
  if (!selectedUser.value) return
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${selectedUser.value.id}/roles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({ roleId }),
    })
    if (!res.ok) { const body = await res.json(); throw new Error(body.error ?? 'Chyba') }
    await selectUser(selectedUser.value.id)
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba přidání role'
  }
}

async function removeRole(roleId: number) {
  if (!selectedUser.value || !confirm('Odebrat roli?')) return
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/users/${selectedUser.value.id}/roles/${roleId}`, {
      method: 'DELETE',
      headers: { ...authHeaders() },
    })
    if (!res.ok) throw new Error()
    await selectUser(selectedUser.value.id)
    await loadUsers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba odebrání role'
  }
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Uživatelé</h1>
        <p class="text-body-sm text-text-soft">{{ users.length }} uživatelů</p>
      </div>
      <button @click="showCreate = true" class="btn-primary btn-sm">
        Nový uživatel
      </button>
    </div>

    <div class="mb-6">
      <div class="flex gap-3 items-center">
        <div class="relative flex-1 max-w-sm">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-soft" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input v-model="searchQuery" @input="loadUsers" placeholder="Hledat podle jména nebo emailu…"
            class="input-field pl-10" />
        </div>
      </div>
      <p v-if="error" class="mt-2 text-body-sm text-error">{{ error }}</p>
    </div>

    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>

    <div v-else-if="users.length === 0" class="py-12 text-center">
      <p class="text-section-title text-text-soft">Žádní uživatelé</p>
    </div>

    <div v-else class="space-y-3">
      <div v-for="u in users" :key="u.id"
        @click="selectUser(u.id)"
        class="card cursor-pointer transition-all hover:shadow-md"
      >
        <div class="flex items-center justify-between gap-4">
          <div class="flex items-center gap-3 min-w-0">
            <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-strong text-text-muted font-semibold text-body">
              {{ (u.firstName?.[0] || '').toUpperCase() }}{{ (u.lastName?.[0] || '').toUpperCase() }}
            </div>
            <div class="min-w-0">
              <span class="font-medium text-text">{{ u.lastName ? u.firstName + ' ' + u.lastName : u.firstName }}</span>
              <div class="flex flex-wrap gap-x-3 gap-y-0.5 text-meta text-text-soft">
                <span>{{ u.email }}</span>
                <span>@{{ u.username }}</span>
                <span v-if="u.phone">{{ u.phone }}</span>
                <span v-if="u.memberSince">od {{ u.memberSince }}</span>
              </div>
            </div>
          </div>
          <div class="flex gap-1.5 shrink-0">
            <span v-for="r in u.appRoles" :key="r.id"
              class="badge"
              :class="r.name === 'ADMIN' ? 'badge-admin' : r.name === 'JUDGE' ? 'badge-judge' : 'badge-racer'"
            >{{ r.displayName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create dialog -->
    <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40" @click.self="showCreate = false">
      <div class="mx-4 w-full max-w-form rounded-xl border border-border bg-surface p-6 shadow-lg">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-subsection text-text">Nový uživatel</h2>
          <button @click="showCreate = false" class="text-text-soft hover:text-text">&times;</button>
        </div>
        <form @submit.prevent="createUser" class="space-y-4">
          <div>
            <label class="input-label">Uživatelské jméno</label>
            <input v-model="createForm.username" required class="input-field" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="input-label">Jméno</label>
              <input v-model="createForm.firstName" required class="input-field" />
            </div>
            <div>
              <label class="input-label">Příjmení</label>
              <input v-model="createForm.lastName" class="input-field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="input-label">Email</label>
              <input v-model="createForm.email" type="email" required class="input-field" />
            </div>
            <div>
              <label class="input-label">Telefon</label>
              <input v-model="createForm.phone" type="tel" class="input-field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="input-label">V klubu od</label>
              <input v-model="createForm.memberSince" type="date" class="input-field" />
            </div>
            <div>
              <label class="input-label">Heslo (min. 6 znaků)</label>
              <input v-model="createForm.password" type="password" required minlength="6" class="input-field" />
            </div>
          </div>
          <p v-if="error" class="text-body-sm text-error">{{ error }}</p>
          <button type="submit" :disabled="saving" class="btn-primary w-full">
            {{ saving ? 'Vytvářím…' : 'Vytvořit uživatele' }}
          </button>
        </form>
      </div>
    </div>

    <!-- User detail dialog -->
    <div v-if="selectedUser" class="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 py-8" @click.self="closeUser">
      <div class="mx-4 w-full max-w-form rounded-xl border border-border bg-surface p-6 shadow-lg">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-subsection text-text">{{ selectedUser.lastName ? selectedUser.firstName + ' ' + selectedUser.lastName : selectedUser.firstName }}</h2>
          <button @click="closeUser" class="text-text-soft hover:text-text">&times;</button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="input-label">Jméno</label>
              <input v-model="editFirstName" class="input-field" />
            </div>
            <div>
              <label class="input-label">Příjmení</label>
              <input v-model="editLastName" class="input-field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="input-label">Email</label>
              <input v-model="editEmail" type="email" class="input-field" />
            </div>
            <div>
              <label class="input-label">Telefon</label>
              <input v-model="editPhone" type="tel" class="input-field" />
            </div>
          </div>
          <div>
            <label class="input-label">V klubu od</label>
            <input v-model="editMemberSince" type="date" class="input-field" />
          </div>
          <div class="text-meta text-text-soft bg-bg-alt -mx-6 px-6 py-3">
            Uživatelské jméno: {{ selectedUser.username }} · Registrován: {{ new Date(selectedUser.createdAt).toLocaleString('cs') }}
          </div>
        </div>

        <hr class="my-6 border-border" />

        <div>
          <h3 class="text-subsection text-text">Nové heslo</h3>
          <div class="mt-3 flex gap-3">
            <input v-model="pwNewPassword" type="password" placeholder="Nové heslo" minlength="6" class="input-field flex-1" />
            <button @click="setPassword" :disabled="pwSaving || !pwNewPassword" class="btn-primary btn-sm">
              {{ pwSaving ? '…' : 'Nastavit' }}
            </button>
          </div>
          <p v-if="pwError" class="mt-1 text-body-sm text-error">{{ pwError }}</p>
          <p v-if="pwSuccess" class="mt-1 text-body-sm text-success">Heslo změněno</p>
        </div>

        <hr class="my-6 border-border" />

        <div>
          <h3 class="text-subsection text-text">Aktuální role</h3>
          <div v-if="selectedUser.appRoles.length === 0" class="mt-3 text-body-sm text-text-soft">Žádné role</div>
          <div v-else class="mt-3 space-y-2">
            <div v-for="r in selectedUser.appRoles" :key="r.id"
              class="flex items-center justify-between rounded-md border border-border bg-bg px-4 py-2.5"
            >
              <span class="text-body text-text">{{ r.displayName }}</span>
              <button @click="removeRole(r.id)" class="text-body-sm text-error hover:text-error/80">Odebrat</button>
            </div>
          </div>
        </div>

        <div class="mt-6">
          <h3 class="text-subsection text-text">Přidat roli</h3>
          <div class="mt-3 flex flex-wrap gap-2">
            <template v-for="r in roles" :key="r.id">
              <button v-if="!selectedUser.appRoles.some((a: any) => a.id === r.id)"
                @click="addRole(r.id)"
                class="btn-secondary btn-sm"
              >+ {{ r.displayName }}</button>
            </template>
            <span v-if="roles.every((r: any) => selectedUser.appRoles.some((a: any) => a.id === r.id))"
              class="text-body-sm text-text-soft">Uživatel má všechny role</span>
          </div>
        </div>

        <hr class="my-6 border-border" />

        <p v-if="error" class="mb-4 text-body-sm text-error">{{ error }}</p>

        <button @click="saveUser" :disabled="saving" class="btn-primary w-full">
          {{ saving ? 'Ukládám…' : 'Uložit změny' }}
        </button>

        <div v-if="deleteConfirm" class="mt-6 alert alert-error">
          <p class="font-semibold">Opravdu smazat tohoto uživatele?</p>
          <div class="mt-3 flex gap-3">
            <button @click="deleteUser" :disabled="saving" class="btn-danger btn-sm">
              {{ saving ? 'Mažu…' : 'Ano, smazat' }}
            </button>
            <button @click="deleteConfirm = false" class="btn-secondary btn-sm">Zrušit</button>
          </div>
        </div>

        <button v-else @click="deleteConfirm = true" class="mt-4 w-full btn-ghost btn-sm text-error">
          Smazat uživatele
        </button>
      </div>
    </div>
  </div>
</template>
