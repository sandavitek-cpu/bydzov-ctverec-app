<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'
import {
  fetchAdminSchedule,
  createAdminScheduleItem,
  updateAdminScheduleItem,
  deleteAdminScheduleItem,
  type ScheduleItemData,
} from '@/api'

const { authHeaders } = useAuth()
const { show } = useToast()
const success = (msg: string) => show(msg, 'success')
const error = (msg: string) => show(msg, 'error')

const items = ref<ScheduleItemData[]>([])
const loading = ref(true)
const editing = ref<Record<number, ScheduleItemData>>({})
const adding = ref(false)
const saving = ref(false)
const newItem = ref<ScheduleItemData>({ time: '', label: '', description: null, sortOrder: 0 })

async function load() {
  try {
    items.value = await fetchAdminSchedule(authHeaders())
  } catch (e) {
    error(e instanceof Error ? e.message : 'Chyba načítání')
  } finally {
    loading.value = false
  }
}

function startAdd() {
  newItem.value = { time: '', label: '', description: null, sortOrder: items.value.length }
  adding.value = true
}

function startEdit(item: ScheduleItemData) {
  editing.value[item.id!] = { ...item }
}

function cancelEdit(id: number) {
  delete editing.value[id]
}

async function saveEdit(id: number) {
  const data = editing.value[id]
  if (!data) return
  if (!data.time || !data.label) {
    error('Čas a název jsou povinné')
    return
  }
  try {
    await updateAdminScheduleItem(id, data, authHeaders())
    success('Položka uložena')
    delete editing.value[id]
    await load()
  } catch (e) {
    error(e instanceof Error ? e.message : 'Chyba uložení')
  }
}

async function removeItem(id: number) {
  if (!confirm('Opravdu smazat?')) return
  try {
    await deleteAdminScheduleItem(id, authHeaders())
    success('Položka smazána')
    await load()
  } catch (e) {
    error(e instanceof Error ? e.message : 'Chyba mazání')
  }
}

async function addItem() {
  if (!newItem.value.time || !newItem.value.label) {
    error('Čas a název jsou povinné')
    return
  }
  try {
    await createAdminScheduleItem(newItem.value, authHeaders())
    success('Položka přidána')
    adding.value = false
    await load()
  } catch (e) {
    error(e instanceof Error ? e.message : 'Chyba přidání')
  }
}

async function moveUp(i: number) {
  if (i <= 0 || saving.value) return
  saving.value = true
  const arr = [...items.value]
  const tmp = arr[i]
  arr[i] = arr[i - 1]
  arr[i - 1] = tmp
  arr.forEach((item, idx) => item.sortOrder = idx)
  items.value = arr
  try {
    await Promise.all(arr.map(item =>
      updateAdminScheduleItem(item.id!, { sortOrder: item.sortOrder }, authHeaders())
    ))
  } catch {
    show('Chyba uložení', 'error')
  }
  await load()
  saving.value = false
}

async function moveDown(i: number) {
  if (i >= items.value.length - 1 || saving.value) return
  saving.value = true
  const arr = [...items.value]
  const tmp = arr[i]
  arr[i] = arr[i + 1]
  arr[i + 1] = tmp
  arr.forEach((item, idx) => item.sortOrder = idx)
  items.value = arr
  try {
    await Promise.all(arr.map(item =>
      updateAdminScheduleItem(item.id!, { sortOrder: item.sortOrder }, authHeaders())
    ))
  } catch {
    show('Chyba uložení', 'error')
  }
  await load()
  saving.value = false
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-page-title text-text">Program závodu</h1>
      <button v-if="!adding" @click="startAdd()" class="btn-primary btn-sm">+ Přidat položku</button>
    </div>

    <div v-if="adding" class="mb-6 rounded-xl border border-border bg-surface p-4 space-y-3">
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div>
          <label class="text-meta text-text-soft block mb-1">Čas</label>
          <input v-model="newItem.time" class="input w-full" placeholder="např. 8:30" />
        </div>
        <div>
          <label class="text-meta text-text-soft block mb-1">Název</label>
          <input v-model="newItem.label" class="input w-full" placeholder="např. Prezence" />
        </div>
        <div>
          <label class="text-meta text-text-soft block mb-1">Pořadí</label>
          <div class="input w-full bg-surface-strong/50 flex items-center text-text-muted">{{ items.length + 1 }}</div>
        </div>
      </div>
      <div>
        <label class="text-meta text-text-soft block mb-1">Popis (volitelný)</label>
        <input v-model="newItem.description" class="input w-full" placeholder="Popis položky" />
      </div>
      <div class="flex gap-2">
        <button @click="addItem" class="btn-primary btn-sm">Uložit</button>
        <button @click="adding = false" class="btn-ghost btn-sm">Zrušit</button>
      </div>
    </div>

    <div v-if="loading" class="text-body text-text-muted py-8 text-center">Načítám...</div>
    <div v-else-if="!items.length" class="text-body text-text-muted py-8 text-center">Žádné položky programu</div>

    <div v-else class="overflow-x-auto">
      <table class="w-full text-left">
        <thead>
          <tr class="text-meta text-text-soft uppercase border-b border-border">
            <th class="py-3 pr-4 w-16">Pořadí</th>
            <th class="py-3 pr-4 w-24">Čas</th>
            <th class="py-3 pr-4">Název</th>
            <th class="py-3 pr-4 max-sm:hidden">Popis</th>
            <th class="py-3 w-32">Akce</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, i) in items" :key="item.id" class="border-b border-border/50 hover:bg-surface-strong/30">
            <td class="py-3 pr-4">
              <div class="flex items-center gap-1">
                <span class="text-meta text-text-soft">{{ i + 1 }}</span>
                <div class="flex flex-col gap-0.5 ml-1">
                  <button @click="moveUp(i)" :disabled="i === 0 || saving" class="text-text-soft hover:text-primary disabled:opacity-30 leading-none text-xs">&uarr;</button>
                  <button @click="moveDown(i)" :disabled="i === items.length - 1 || saving" class="text-text-soft hover:text-primary disabled:opacity-30 leading-none text-xs">&darr;</button>
                </div>
              </div>
            </td>
            <td class="py-3 pr-4">
              <template v-if="editing[item.id!]">
                <input v-model="editing[item.id!].time" class="input w-20" />
              </template>
              <template v-else>
                <span class="font-mono text-label">{{ item.time }}</span>
              </template>
            </td>
            <td class="py-3 pr-4">
              <template v-if="editing[item.id!]">
                <input v-model="editing[item.id!].label" class="input w-full" />
              </template>
              <template v-else>
                <span class="font-semibold text-text">{{ item.label }}</span>
              </template>
            </td>
            <td class="py-3 pr-4 max-sm:hidden">
              <template v-if="editing[item.id!]">
                <input v-model="editing[item.id!].description" class="input w-full" placeholder="–" />
              </template>
              <template v-else>
                <span class="text-body-sm text-text-muted">{{ item.description ?? '–' }}</span>
              </template>
            </td>
            <td class="py-3">
              <template v-if="editing[item.id!]">
                <div class="flex gap-1">
                  <button @click="saveEdit(item.id!)" class="btn-primary btn-xs">Uložit</button>
                  <button @click="cancelEdit(item.id!)" class="btn-ghost btn-xs">Zrušit</button>
                </div>
              </template>
              <template v-else>
                <div class="flex gap-1">
                  <button @click="startEdit(item)" class="btn-ghost btn-xs">Upravit</button>
                  <button @click="removeItem(item.id!)" class="btn-ghost btn-xs text-text-soft hover:text-red">Smazat</button>
                </div>
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
