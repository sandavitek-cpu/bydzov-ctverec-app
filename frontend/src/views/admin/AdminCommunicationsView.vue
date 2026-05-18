<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
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
  { value: 'JUDGES', label: 'Komisaři' },
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
  JUDGES: 'Komisaři',
}

onMounted(loadHistory)
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <h1 class="text-page-title text-text">Hromadná komunikace</h1>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Nová zpráva</h2>
          <form @submit.prevent="send" class="space-y-4">
            <div>
              <label class="input-label">Příjemci</label>
              <select v-model="recipientType" class="input-field">
                <option v-for="o in recipientOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div>
              <label class="input-label">Předmět</label>
              <input v-model="subject" required class="input-field" />
            </div>
            <div>
              <label class="input-label">Zpráva</label>
              <textarea v-model="messageBody" required rows="6" class="input-field min-h-[140px] resize-y"></textarea>
            </div>
            <button type="submit" :disabled="sending" class="btn-primary w-full">
              {{ sending ? 'Odesílám…' : 'Odeslat' }}
            </button>
          </form>

          <div v-if="result" class="mt-4 alert alert-success">
            Odesláno {{ result.sent }} / {{ result.total }} e-mailů
          </div>
          <p v-if="error" class="mt-3 text-body-sm text-error">{{ error }}</p>
        </div>
      </div>

      <div>
        <div class="card">
          <h2 class="text-subsection text-text mb-4">Historie odeslaných zpráv</h2>
          <LoadingSpinner v-if="loadingHistory" :small="true" />
          <div v-else-if="history.length === 0" class="text-body-sm text-text-soft">Zatím žádné zprávy.</div>
          <div v-else class="space-y-3 max-h-96 overflow-y-auto">
            <div v-for="entry in history" :key="entry.id"
              class="rounded-md border border-border bg-bg p-3"
            >
              <div class="flex items-center justify-between gap-2">
                <span class="badge badge-judge text-meta">{{ recipientLabel[entry.recipientType] ?? entry.recipientType }}</span>
                <span class="text-meta text-text-soft">{{ new Date(entry.createdAt).toLocaleString('cs') }}</span>
              </div>
              <div class="mt-1.5 text-body font-medium text-text">{{ entry.subject }}</div>
              <div class="mt-0.5 text-body-sm text-text-muted line-clamp-2">{{ entry.body }}</div>
              <div class="mt-1 text-meta text-text-soft">{{ entry.recipientCount }} příjemců</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
