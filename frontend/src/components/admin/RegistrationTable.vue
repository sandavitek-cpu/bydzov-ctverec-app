<script setup lang="ts">
import type { AdminReg } from '@/views/admin/AdminDashboardView.vue'

const props = defineProps<{
  registrations: AdminReg[]
  selectedIds: Set<number>
  resendingId: number | null
  remindingId: number | null
  variantDeadline: Record<string, string>
  variantLabel: Record<string, string>
  categoryLabel: Record<string, string>
}>()

const emit = defineEmits<{
  (e: 'toggleSelect', id: number): void
  (e: 'toggleSelectAll'): void
  (e: 'toggleStatus', reg: AdminReg): void
  (e: 'resend', reg: AdminReg): void
  (e: 'sendReminder', reg: AdminReg): void
  (e: 'downloadPdf', reg: AdminReg): void
  (e: 'impersonate', reg: AdminReg): void
  (e: 'assignStartNumber', reg: AdminReg): void
  (e: 'selectReg', reg: AdminReg): void
}>()

function overdueInfo(reg: AdminReg): number | null {
  if (reg.status === 'PAID' || reg.status === 'CANCELLED' || !reg.variant) return null
  const deadline = props.variantDeadline[reg.variant]
  if (!deadline) return null
  const d = new Date(deadline + 'T00:00:00')
  const now = new Date()
  if (now <= d) return null
  const days = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
  if (days <= 0) return null
  return days
}
</script>

<template>
  <div>
    <!-- Desktop table -->
    <div class="hidden sm:block overflow-x-auto rounded-xl border border-border">
      <table class="w-full">
        <thead class="table-header">
          <tr>
            <th class="w-8">
              <input type="checkbox" :checked="selectedIds.size === registrations.length && registrations.length > 0" @change="emit('toggleSelectAll')" class="cursor-pointer" />
            </th>
            <th class="w-8 text-center">#</th>
            <th>Posádka</th>
            <th class="hidden sm:table-cell">Varianta</th>
            <th>Členové</th>
            <th>Stav</th>
            <th class="text-right">Akce</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in registrations" :key="r.id"
            @click="emit('selectReg', r)"
            class="table-row cursor-pointer"
            :class="{
              'border-l-2 border-l-success': r.status === 'PAID' && r.arrived,
              'border-l-2 border-l-transparent': r.status !== 'PAID' || !r.arrived,
            }"
          >
            <td class="text-center" @click.stop>
              <input type="checkbox" :checked="selectedIds.has(r.id)" @change="emit('toggleSelect', r.id)" class="cursor-pointer" />
            </td>
            <td class="text-center font-mono font-bold text-primary">{{ r.startNumber }}</td>
            <td>
              <div class="font-medium text-text">{{ r.teamName }}</div>
              <div class="text-meta text-text-soft">{{ r.vehicleCategory ? categoryLabel[r.vehicleCategory] ?? r.vehicleCategory : '' }}{{ r.vehicleMake ? ' · ' + r.vehicleMake : '' }}</div>
            </td>
            <td class="hidden sm:table-cell">
              <span v-if="r.variant" class="text-body-sm text-text-muted">{{ variantLabel[r.variant] ?? r.variant }}</span>
              <span v-else class="text-body-sm text-text-soft">—</span>
            </td>
            <td>
              <div class="flex flex-wrap gap-1">
                <span v-for="(cm, i) in r.crewMembers" :key="i"
                  class="inline-flex items-center gap-1 rounded-full bg-primary/5 px-2 py-0.5 text-meta text-text-muted"
                  :title="cm.email"
                >{{ cm.firstName }} {{ cm.lastName }}</span>
                <span v-if="!r.crewMembers?.length" class="text-meta text-text-soft">—</span>
              </div>
            </td>
            <td>
              <div class="flex flex-wrap gap-1.5">
                <span v-if="r.status === 'CANCELLED'" class="badge !bg-red/10 !text-red">Stornováno</span>
                <button v-else @click.stop="emit('toggleStatus', r)"
                  class="badge cursor-pointer transition-colors"
                  :class="r.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'"
                >{{ r.status === 'PAID' ? 'Přihlášen a zaplaceno' : 'Přihlášen, nezaplaceno' }}</button>
                <span v-if="r.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
                <span v-if="r.firstTime" class="badge !bg-red/10 !text-red">Nový</span>
                <span v-if="overdueInfo(r) !== null" class="badge !bg-warning/10 !text-warning"
                  :title="'Splatnost ' + (variantDeadline[r.variant ?? ''] ?? '?')">Po splatnosti {{ overdueInfo(r) }} dní</span>
              </div>
            </td>
            <td class="text-right whitespace-nowrap">
              <div class="flex gap-1.5 justify-end" @click.stop>
                <button @click="emit('resend', r)" :disabled="resendingId === r.id"
                  class="btn-ghost btn-xs whitespace-nowrap" title="Znovu odeslat přihlašovací údaje"
                >{{ resendingId === r.id ? '…' : 'Poslat údaje' }}</button>
                <button v-if="r.status !== 'PAID'" @click="emit('sendReminder', r)" :disabled="remindingId === r.id"
                  class="btn-ghost btn-xs whitespace-nowrap" title="Odeslat upomínku o nezaplacení"
                >{{ remindingId === r.id ? '…' : 'Upomínka' }}</button>
                <button @click="emit('downloadPdf', r)" class="btn-ghost btn-xs whitespace-nowrap" title="Stáhnout přihlášku k prezenci (PDF)"
                >PDF</button>
                <button @click="emit('impersonate', r)"
                  class="btn-ghost btn-xs"
                  title="Přihlásit jako tento tým"
                ><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg></button>
                <button v-if="r.status === 'PAID' && (!r.startNumber || r.startNumber === 0)" @click.stop="emit('assignStartNumber', r)"
                  class="btn-secondary btn-xs whitespace-nowrap"
                  title="Přidělit startovní číslo"
                >#</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Mobile cards -->
    <div class="sm:hidden space-y-3">
      <div v-for="r in registrations" :key="r.id"
        @click="emit('selectReg', r)"
        class="card !p-4 cursor-pointer relative overflow-hidden"
        :class="{
          'border-l-success': r.status === 'PAID' && r.arrived,
          'border-l-warning': r.status === 'PENDING',
          'border-l-red': r.status === 'CANCELLED',
        }"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex items-center gap-2" @click.stop>
            <input type="checkbox" :checked="selectedIds.has(r.id)" @change="emit('toggleSelect', r.id)" class="cursor-pointer" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-mono font-bold text-primary text-sm">#{{ r.startNumber }}</span>
              <span class="font-medium text-text truncate">{{ r.teamName }}</span>
            </div>
            <div class="text-meta text-text-soft mt-0.5">
              {{ r.vehicleCategory ? (categoryLabel[r.vehicleCategory] ?? r.vehicleCategory) : '' }}{{ r.vehicleMake ? ' · ' + r.vehicleMake : '' }}
              <span v-if="r.variant" class="ml-2">· {{ variantLabel[r.variant] ?? r.variant }}</span>
            </div>
            <div class="flex flex-wrap gap-1 mt-2">
              <span v-if="r.status === 'CANCELLED'" class="badge !bg-red/10 !text-red">Stornováno</span>
              <span v-else class="badge"
                :class="r.status === 'PAID' ? '!bg-success/10 !text-success' : 'badge-admin'"
              >{{ r.status === 'PAID' ? 'Zaplaceno' : 'Nezaplaceno' }}</span>
              <span v-if="r.arrived" class="badge !bg-info/10 !text-info">Přijel</span>
              <span v-if="r.firstTime" class="badge !bg-red/10 !text-red">Nový</span>
            </div>
          </div>
          <button @click.stop="emit('selectReg', r)" class="text-text-soft hover:text-text shrink-0">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
