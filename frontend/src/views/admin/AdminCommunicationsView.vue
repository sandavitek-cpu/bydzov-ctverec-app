<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl, sendNotify, type NotifyResult, type MessageLogEntry } from '@/api'

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const recipientType = ref('ALL_RACERS')
const subject = ref('')
const messageBody = ref('')
const sending = ref(false)
const result = ref<NotifyResult | null>(null)
const error = ref<string | null>(null)

const history = ref<MessageLogEntry[]>([])
const loadingHistory = ref(true)

if (!isAdmin.value) {
  router.push('/admin/login')
}

const recipientOptions = [
  { value: 'ALL_RACERS', label: 'Všichni závodníci' },
  { value: 'ADMINS', label: 'Pořadatelé' },
  { value: 'JUDGES', label: 'Rozhodčí' },
]

async function send() {
  error.value = null
  result.value = null
  sending.value = true
  try {
    const r = await sendNotify({
      recipientType: recipientType.value,
      subject: subject.value,
      body: messageBody.value,
    }, authHeaders())
    result.value = r
    subject.value = ''
    messageBody.value = ''
    await loadHistory()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba odeslání'
  } finally {
    sending.value = false
  }
}

async function loadHistory() {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/notify`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    history.value = await res.json()
  } catch {
    // silent
  } finally {
    loadingHistory.value = false
  }
}

const recipientLabel: Record<string, string> = {
  ALL_RACERS: 'Všichni závodníci',
  ADMINS: 'Pořadatelé',
  JUDGES: 'Rozhodčí',
}

onMounted(loadHistory)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4">
      <h1 class="text-2xl font-bold text-white">Hromadná komunikace</h1>
    </div>

    <div class="mt-6 grid gap-6 lg:grid-cols-2">
      <div>
        <div class="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
          <h2 class="mb-4 text-lg font-semibold text-white">Nová zpráva</h2>
          <form @submit.prevent="send" class="space-y-3">
            <div>
              <label class="block text-xs text-slate-500">Příjemci</label>
              <select
                v-model="recipientType"
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white"
              >
                <option v-for="o in recipientOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-xs text-slate-500">Předmět</label>
              <input
                v-model="subject"
                required
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white"
              />
            </div>
            <div>
              <label class="block text-xs text-slate-500">Zpráva</label>
              <textarea
                v-model="messageBody"
                required
                rows="6"
                class="mt-1 w-full rounded border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm text-white"
              ></textarea>
            </div>
            <button
              type="submit"
              :disabled="sending"
              class="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-amber-500 disabled:opacity-50"
            >
              {{ sending ? 'Odesílám…' : 'Odeslat' }}
            </button>
          </form>

          <div v-if="result" class="mt-4 rounded bg-emerald-900/30 p-3 text-sm text-emerald-400">
            Odesláno {{ result.sent }} / {{ result.total }} e-mailů
          </div>
          <p v-if="error" class="mt-3 text-sm text-red-400">{{ error }}</p>
        </div>
      </div>

      <div>
        <div class="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
          <h2 class="mb-4 text-lg font-semibold text-white">Historie odeslaných zpráv</h2>
          <p v-if="loadingHistory" class="text-sm text-slate-500">Načítám…</p>
          <div v-else-if="history.length === 0" class="text-sm text-slate-500">Zatím žádné zprávy.</div>
          <div v-else class="space-y-3 max-h-96 overflow-y-auto">
            <div
              v-for="entry in history"
              :key="entry.id"
              class="rounded border border-slate-800 bg-slate-950/50 p-3"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="text-xs font-medium text-amber-400">{{ recipientLabel[entry.recipientType] ?? entry.recipientType }}</span>
                <span class="text-xs text-slate-600">{{ new Date(entry.createdAt).toLocaleString('cs') }}</span>
              </div>
              <div class="mt-1 text-sm font-medium text-white">{{ entry.subject }}</div>
              <div class="mt-1 text-xs text-slate-500 line-clamp-2">{{ entry.body }}</div>
              <div class="mt-1 text-xs text-slate-600">{{ entry.recipientCount }} příjemců</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
