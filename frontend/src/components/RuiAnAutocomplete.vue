<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { searchRuiAnAddress, type RuiAnAddress } from '@/api'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  required?: boolean
}>(), {
  placeholder: 'Město',
  required: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const query = ref(props.modelValue)
const results = ref<RuiAnAddress[]>([])
const loading = ref(false)
const open = ref(false)
const selectedIndex = ref(-1)
let debounceTimer: ReturnType<typeof setTimeout> | null = null
let blurTimer: ReturnType<typeof setTimeout> | null = null
const inputRef = ref<HTMLInputElement | null>(null)

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (blurTimer) clearTimeout(blurTimer)
})

watch(() => props.modelValue, (val) => {
  if (val !== query.value) {
    query.value = val
  }
})

function onInput(value: string) {
  query.value = value
  emit('update:modelValue', value)
  if (debounceTimer) clearTimeout(debounceTimer)
  if (value.trim().length < 2) {
    results.value = []
    open.value = false
    return
  }
  loading.value = true
  debounceTimer = setTimeout(async () => {
    try {
      results.value = await searchRuiAnAddress(value.trim())
      open.value = results.value.length > 0
      selectedIndex.value = -1
    } catch {
      results.value = []
    } finally {
      loading.value = false
    }
  }, 250)
}

function select(item: RuiAnAddress) {
  query.value = item.adresa
  emit('update:modelValue', item.adresa)
  results.value = []
  open.value = false
}

function onBlur() {
  if (blurTimer) clearTimeout(blurTimer)
  blurTimer = setTimeout(() => {
    open.value = false
    blurTimer = null
  }, 200)
}

function onKeydown(e: KeyboardEvent) {
  if (!open.value || results.value.length === 0) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = Math.min(selectedIndex.value + 1, results.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
  } else if (e.key === 'Enter' && selectedIndex.value >= 0) {
    e.preventDefault()
    select(results.value[selectedIndex.value])
  } else if (e.key === 'Escape') {
    open.value = false
  }
}
</script>

<template>
  <div class="relative">
    <input
      ref="inputRef"
      :value="query"
      @input="onInput(($event.target as HTMLInputElement).value)"
      @keydown="onKeydown"
      @focus="query.trim().length >= 2 && results.length > 0 && (open = true)"
      @blur="onBlur"
      :required="required"
      class="input-field"
      :placeholder="placeholder"
      autocomplete="off"
    />
    <div v-if="loading" class="absolute right-3 top-1/2 -translate-y-1/2">
      <div class="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
    </div>
    <ul
      v-if="open && results.length > 0"
      class="absolute z-50 left-0 right-0 mt-1 max-h-48 overflow-y-auto rounded-lg border border-border bg-bg shadow-lg"
    >
      <li
        v-for="(item, i) in results"
        :key="item.kod"
        @mousedown.prevent="select(item)"
        :class="[
          'px-3 py-2 cursor-pointer text-body-sm transition-colors',
          i === selectedIndex ? 'bg-primary/10 text-primary' : 'hover:bg-bg-alt'
        ]"
      >
        {{ item.adresa }}
      </li>
    </ul>
  </div>
</template>
