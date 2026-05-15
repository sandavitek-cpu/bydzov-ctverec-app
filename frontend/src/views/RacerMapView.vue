<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
import L from 'leaflet'

const router = useRouter()
const { isLoggedIn } = useAuth()

interface CheckpointData {
  name: string
  lat: number
  lng: number
  radius: number
}

const checkpoints = ref<CheckpointData[]>([])
const loading = ref(true)
const gpsActive = ref(false)
const gpsLat = ref(0)
const gpsLng = ref(0)
const gpsError = ref<string | null>(null)
const mapReady = ref(false)
let map: L.Map | null = null
let marker: L.Marker | null = null
let watchId: number | null = null

onMounted(async () => {
  try {
    const res = await fetch(`${apiBaseUrl}/api/public/checkpoints/2026`, {
      headers: { Accept: 'application/json' },
    })
    if (!res.ok) throw new Error(`API ${res.status}`)
    checkpoints.value = await res.json()
  } catch {
  }

  await nextTick()

  map = L.map('race-map', {
    center: [50.2415, 15.49],
    zoom: 14,
    zoomControl: true,
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://openstreetmap.org">OSM</a>',
    maxZoom: 18,
  }).addTo(map)

  for (const cp of checkpoints.value) {
    L.circle([cp.lat, cp.lng], {
      radius: cp.radius,
      color: '#f59e0b',
      fillColor: '#f59e0b',
      fillOpacity: 0.1,
      weight: 2,
    }).addTo(map).bindPopup(cp.name)
    L.marker([cp.lat, cp.lng], {
      icon: L.divIcon({
        className: 'text-amber-400 text-xs font-bold whitespace-nowrap',
        html: cp.name,
        iconSize: [100, 20],
        iconAnchor: [50, 10],
      }),
    }).addTo(map)
  }

  mapReady.value = true
  loading.value = false

  if ('geolocation' in navigator) {
    watchId = navigator.geolocation.watchPosition(
      (pos) => {
        gpsLat.value = pos.coords.latitude
        gpsLng.value = pos.coords.longitude
        gpsActive.value = true
        gpsError.value = null
        if (marker) {
          marker.setLatLng([gpsLat.value, gpsLng.value])
        } else {
          marker = L.marker([gpsLat.value, gpsLng.value], {
            icon: L.divIcon({
              className: 'gps-dot',
              html: '<div style="width:16px;height:16px;background:#3b82f6;border:3px solid white;border-radius:50%;box-shadow:0 0 8px rgba(59,130,246,0.6)"></div>',
              iconSize: [16, 16],
              iconAnchor: [8, 8],
            }),
          }).addTo(map!).bindPopup('Vaše poloha')
        }
      },
      (err) => {
        gpsError.value = 'GPS nedostupná: ' + err.message
      },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  } else {
    gpsError.value = 'GPS není v prohlížeči dostupná'
  }
})

onUnmounted(() => {
  if (watchId !== null) navigator.geolocation.clearWatch(watchId)
  if (map) map.remove()
})

if (!isLoggedIn.value) {
  router.push('/admin/login')
}
</script>

<template>
  <div class="mx-auto max-w-lg">
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-xl font-bold text-white">Mapa trasy</h1>
      <div class="flex items-center gap-2 text-xs">
        <span class="text-slate-500">GPS</span>
        <span
          class="inline-block h-2 w-2 rounded-full"
          :class="gpsActive ? 'bg-emerald-500' : 'bg-slate-600'"
        ></span>
      </div>
    </div>

    <p v-if="loading" class="text-slate-500">Načítám mapu…</p>
    <p v-else-if="gpsError" class="mb-2 text-xs text-amber-400">{{ gpsError }}</p>

    <div
      id="race-map"
      class="h-[70vh] w-full rounded-xl border border-slate-800 shadow-lg"
      :class="{ 'opacity-0': loading }"
    ></div>

    <div class="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
      <span class="rounded bg-slate-800 px-2 py-1">
        🟡 Stanoviště — přibližná oblast
      </span>
      <span class="rounded bg-slate-800 px-2 py-1">
        🔵 Vaše GPS poloha
      </span>
    </div>
  </div>
</template>

<style>
.gps-dot {
  background: none !important;
  border: none !important;
}
</style>
