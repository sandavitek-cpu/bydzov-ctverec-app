<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps<{
  amount: number
  variableSymbol: number
  message?: string
}>()

const qrDataUrl = ref('')

function generateSpd() {
  const iban = 'CZ6508000000001086360369'
  const amount = props.amount.toFixed(2)
  const vs = String(props.variableSymbol)

  const spd = [
    'SPD*1.0',
    `ACC:${iban}`,
    `AM:${amount}`,
    'CC:CZK',
    `MSG:${props.message ? props.message : 'VS:' + vs}`,
    `X-VS:${vs}`,
  ].join('*')

  return spd
}

async function generate() {
  const spd = generateSpd()
  try {
    qrDataUrl.value = await QRCode.toDataURL(spd, {
      width: 300,
      margin: 2,
      color: { dark: '#1a1a2e', light: '#ffffff' },
    })
  } catch {
    qrDataUrl.value = ''
  }
}

onMounted(generate)
watch(() => props.amount, generate)
watch(() => props.variableSymbol, generate)
</script>

<template>
  <div v-if="qrDataUrl" class="flex flex-col items-center">
    <img :src="qrDataUrl" alt="QR platba" class="w-48 h-48 sm:w-56 sm:h-56" />
    <p class="text-meta text-text-soft mt-2 text-center">
      VS: {{ variableSymbol }}<br />
      {{ amount }} Kč
    </p>
  </div>
  <div v-else class="flex items-center justify-center w-48 h-48 bg-bg-alt rounded-xl">
    <p class="text-body-sm text-text-soft">Generování QR…</p>
  </div>
</template>
