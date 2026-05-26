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
  <div class="overflow-x-auto rounded-xl border border-border">
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
              >👁</button>
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
</template>
