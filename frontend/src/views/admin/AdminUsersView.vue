<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const users = ref<any[]>([])
const roles = ref<any[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const searchQuery = ref('')
const selectedUser = ref<any | null>(null)

if (!isAdmin.value) {
  router.push('/admin/login')
}

async function loadUsers() {
  loading.value = true
  error.value = null
  try {
    const q = searchQuery.value.trim()
    const url = q
      ? `${apiBaseUrl}/api/admin/users?q=${encodeURIComponent(q)}`
      : `${apiBaseUrl}/api/admin/users`
    const res = await fetch(url, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    users.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
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
    if (res.ok) selectedUser.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  }
}

function closeUser() {
  selectedUser.value = null
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
      <div class="flex gap-2">
        <RouterLink
          to="/admin/prihlaseni"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Přihlášky
        </RouterLink>
        <RouterLink
          to="/admin/stanoviste"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Stanoviště
        </RouterLink>
        <RouterLink
          to="/admin/komunikace"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Komunikace
        </RouterLink>
        <RouterLink
          to="/admin/logovani"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Logování
        </RouterLink>
        <RouterLink
          to="/admin/role"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-300 transition hover:bg-slate-800"
        >
          Role
        </RouterLink>
        <button
          @click="logout(); router.push('/admin/login')"
          class="rounded-lg border border-slate-700 px-3 py-2 text-sm text-slate-500 transition hover:bg-slate-800"
        >
          Odhlásit
        </button>
      </div>
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
            <span class="font-medium text-white">{{ u.name }}</span>
            <span class="ml-2 text-xs text-slate-500">{{ u.email }}</span>
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

    <div v-if="selectedUser" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div class="mx-4 w-full max-w-lg rounded-lg border border-slate-800 bg-slate-900 p-6">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-white">{{ selectedUser.name }}</h2>
          <button @click="closeUser" class="text-slate-500 hover:text-slate-300">✕</button>
        </div>
        <div class="mt-4 space-y-2 text-sm">
          <div><span class="text-slate-500">Email:</span> <span class="text-white">{{ selectedUser.email }}</span></div>
          <div><span class="text-slate-500">Registrován:</span> <span class="text-white">{{ new Date(selectedUser.createdAt).toLocaleString('cs') }}</span></div>
        </div>

        <div class="mt-4">
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
            <button v-for="r in roles" :key="r.id"
              v-if="!selectedUser.appRoles.some((a: any) => a.id === r.id)"
              @click="addRole(r.id)"
              class="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 transition hover:bg-slate-800">
              + {{ r.displayName }}
            </button>
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
