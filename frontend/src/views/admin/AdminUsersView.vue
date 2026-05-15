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
    <div class="flex items-center justify-between gap-4">
      <h1 class="text-2xl font-bold text-white">Uživatelé</h1>
      <button @click="showCreate = true"
        class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500">
        Nový uživatel
      </button>
    </div>

    <div class="mt-6">
      <div class="flex gap-2">
        <input v-model="searchQuery" placeholder="Hledat podle jména nebo emailu…" @input="loadUsers"
          class="flex-1 rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500" />
      </div>
      <p v-if="error" class="mt-2 text-sm text-red-400">{{ error }}</p>
    </div>

    <p v-if="loading" class="mt-6 text-slate-500">Načítám…</p>

    <div v-else-if="users.length === 0" class="mt-6 text-slate-500">
      Žádní uživatelé.
    </div>

    <div v-else class="mt-4 space-y-2">
      <div v-for="u in users" :key="u.id"
        @click="selectUser(u.id)"
        class="cursor-pointer rounded-lg border border-slate-800 bg-slate-900/40 p-3 transition hover:bg-slate-900/60">
        <div class="flex items-center justify-between">
          <div>
            <span class="font-medium text-white">{{ u.lastName ? u.firstName + ' ' + u.lastName : u.firstName }}</span>
            <span class="ml-2 text-xs text-slate-500">{{ u.email }}</span>
            <span class="ml-2 text-xs text-slate-600">{{ u.username }}</span>
            <span v-if="u.phone" class="ml-2 text-xs text-slate-600">{{ u.phone }}</span>
            <span v-if="u.memberSince" class="ml-2 text-xs text-slate-600">od {{ u.memberSince }}</span>
          </div>
          <div class="flex gap-1">
            <span v-for="r in u.appRoles" :key="r.id"
              class="rounded-full bg-amber-900/30 px-2 py-0.5 text-xs text-amber-400">
              {{ r.displayName }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create user dialog -->
    <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div class="mx-4 w-full max-w-sm rounded-lg border border-slate-800 bg-slate-900 p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-white">Nový uživatel</h2>
          <button @click="showCreate = false" class="text-slate-500 hover:text-slate-300">✕</button>
        </div>
        <form @submit.prevent="createUser" class="mt-4 space-y-3">
          <div>
            <label class="block text-xs text-slate-500">Uživatelské jméno</label>
            <input v-model="createForm.username" required
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div class="flex gap-2">
            <div class="flex-1">
              <label class="block text-xs text-slate-500">Jméno</label>
              <input v-model="createForm.firstName" required
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
            </div>
            <div class="flex-1">
              <label class="block text-xs text-slate-500">Příjmení</label>
              <input v-model="createForm.lastName"
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
            </div>
          </div>
          <div>
            <label class="block text-xs text-slate-500">Email</label>
            <input v-model="createForm.email" type="email" required
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-500">Telefon</label>
            <input v-model="createForm.phone" type="tel"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-500">V klubu od</label>
            <input v-model="createForm.memberSince" type="date"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-500">Heslo (min. 6 znaků)</label>
            <input v-model="createForm.password" type="password" required minlength="6"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <p v-if="error" class="text-sm text-red-400">{{ error }}</p>
          <button type="submit" :disabled="saving"
            class="w-full rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50">
            {{ saving ? 'Vytvářím…' : 'Vytvořit uživatele' }}
          </button>
        </form>
      </div>
    </div>

    <!-- User detail dialog -->
    <div v-if="selectedUser" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div class="mx-4 w-full max-w-lg rounded-lg border border-slate-800 bg-slate-900 p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-white">{{ selectedUser.lastName ? selectedUser.firstName + ' ' + selectedUser.lastName : selectedUser.firstName }}</h2>
          <div class="flex gap-2">
            <button @click="deleteConfirm = true" class="text-sm text-red-500 hover:text-red-400">Smazat</button>
            <button @click="closeUser" class="text-slate-500 hover:text-slate-300">✕</button>
          </div>
        </div>

        <div v-if="deleteConfirm" class="mt-4 rounded-lg border border-red-800 bg-red-900/30 p-4">
          <p class="text-sm text-red-400">Opravdu smazat tohoto uživatele?</p>
          <div class="mt-3 flex gap-2">
            <button @click="deleteUser" :disabled="saving"
              class="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-500 disabled:opacity-50">
              {{ saving ? 'Mažu…' : 'Ano, smazat' }}
            </button>
            <button @click="deleteConfirm = false"
              class="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 transition hover:bg-slate-800">
              Zrušit
            </button>
          </div>
        </div>

        <div class="mt-4 space-y-3">
          <div class="flex gap-2">
            <div class="flex-1">
              <label class="block text-xs text-slate-500">Jméno</label>
              <input v-model="editFirstName"
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
            </div>
            <div class="flex-1">
              <label class="block text-xs text-slate-500">Příjmení</label>
              <input v-model="editLastName"
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
            </div>
          </div>
          <div>
            <label class="block text-xs text-slate-500">Email</label>
            <input v-model="editEmail" type="email"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-500">Telefon</label>
            <input v-model="editPhone" type="tel"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div>
            <label class="block text-xs text-slate-500">V klubu od</label>
            <input v-model="editMemberSince" type="date"
              class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white" />
          </div>
          <div class="text-xs text-slate-600">
            Uživatelské jméno: {{ selectedUser.username }} · Registrován: {{ new Date(selectedUser.createdAt).toLocaleString('cs') }}
          </div>
          <button @click="saveUser" :disabled="saving"
            class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50">
            {{ saving ? 'Ukládám…' : 'Uložit změny' }}
          </button>
        </div>

        <hr class="my-4 border-slate-800" />

        <div>
          <h3 class="text-sm font-medium text-slate-400">Nové heslo</h3>
          <div class="mt-2 flex gap-2">
            <input v-model="pwNewPassword" type="password" placeholder="Nové heslo" minlength="6"
              class="flex-1 rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white placeholder-slate-500" />
            <button @click="setPassword" :disabled="pwSaving || !pwNewPassword"
              class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50">
              {{ pwSaving ? '…' : 'Nastavit' }}
            </button>
          </div>
          <p v-if="pwError" class="mt-1 text-xs text-red-400">{{ pwError }}</p>
          <p v-if="pwSuccess" class="mt-1 text-xs text-emerald-400">Heslo změněno</p>
        </div>

        <hr class="my-4 border-slate-800" />

        <div>
          <h3 class="text-sm font-medium text-slate-400">Aktuální role</h3>
          <div v-if="selectedUser.appRoles.length === 0" class="mt-2 text-xs text-slate-600">
            Žádné role
          </div>
          <div v-else class="mt-2 space-y-1">
            <div v-for="r in selectedUser.appRoles" :key="r.id"
              class="flex items-center justify-between rounded border border-slate-800 bg-slate-950/50 px-3 py-2">
              <span class="text-sm text-white">{{ r.displayName }}</span>
              <button @click="removeRole(r.id)"
                class="text-xs text-red-500 hover:text-red-400">
                Odebrat
              </button>
            </div>
          </div>
        </div>

        <div class="mt-4">
          <h3 class="text-sm font-medium text-slate-400">Přidat roli</h3>
          <div class="mt-2 flex flex-wrap gap-2">
            <template v-for="r in roles" :key="r.id">
              <button v-if="!selectedUser.appRoles.some((a: any) => a.id === r.id)"
                @click="addRole(r.id)"
                class="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 transition hover:bg-slate-800">
                + {{ r.displayName }}
              </button>
            </template>
            <span v-if="roles.every((r: any) => selectedUser.appRoles.some((a: any) => a.id === r.id))"
              class="text-xs text-slate-600">
              Uživatel má všechny role
            </span>
          </div>
        </div>

        <p v-if="error" class="mt-3 text-sm text-red-400">{{ error }}</p>
      </div>
    </div>
  </div>
</template>
