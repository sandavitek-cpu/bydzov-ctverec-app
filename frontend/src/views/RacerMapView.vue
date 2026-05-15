<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

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
const gpsError = ref<string | null>(null)
let map: L.Map | null = null
let marker: L.Marker | null = null
let watchId: number | null = null

const icon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
})

onMounted(async () => {
  try {
    const res = await fetch(`${apiBaseUrl}/api/public/editions/current`, {
      headers: { Accept: 'application/json' },
    })
    if (!res.ok) throw new Error(`API ${res.status}`)
    const data = await res.json()
    checkpoints.value = data.areas ?? []
  } catch {}

  await new Promise(r => setTimeout(r, 100))

  map = L.map('race-map', {
    center: [50.2415, 15.49],
    zoom: 14,
    zoomControl: true,
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://openstreetmap.org/copyright">OSM</a>',
    maxZoom: 18,
  }).addTo(map)

  for (const cp of checkpoints.value) {
    L.circle([cp.lat, cp.lng], {
      radius: cp.radius,
      color: '#f59e0b',
      fillColor: '#f59e0b',
      fillOpacity: 0.08,
      weight: 2,
    }).addTo(map)
    L.marker([cp.lat, cp.lng], { icon }).addTo(map).bindPopup(`<b>${cp.name}</b>`)
  }

  map.invalidateSize()
  loading.value = false

  if ('geolocation' in navigator) {
    watchId = navigator.geolocation.watchPosition(
      (pos) => {
        gpsActive.value = true
        gpsError.value = null
        const latlng: L.LatLngExpression = [pos.coords.latitude, pos.coords.longitude]
        if (marker) {
          marker.setLatLng(latlng)
        } else {
          marker = L.marker(latlng, {
            icon: L.divIcon({
              className: 'gps-marker',
              html: '<div></div>',
              iconSize: [20, 20],
              iconAnchor: [10, 10],
            }),
          }).addTo(map!)
        }
      },
      (err) => { gpsError.value = 'GPS: ' + err.message },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  } else {
    gpsError.value = 'GPS není v prohlížeči'
  }
})

onUnmounted(() => {
  if (watchId !== null) navigator.geolocation.clearWatch(watchId)
  if (map) map.remove()
})

if (!isLoggedIn.value) router.push('/admin/login')
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <h1 class="text-lg font-bold text-white">Mapa trasy</h1>
      <div class="flex items-center gap-2 text-xs text-slate-500">
        <span class="flex items-center gap-1">
          <span class="inline-block h-2 w-2 rounded-full" :class="gpsActive ? 'bg-emerald-500' : 'bg-slate-600'"></span>
          GPS
        </span>
        <span v-if="gpsError" class="text-amber-400">{{ gpsError }}</span>
      </div>
    </div>

    <div v-if="loading" class="mt-2 flex h-[60vh] items-center justify-center rounded-xl border border-slate-800 text-sm text-slate-500">Načítám mapu…</div>
    <div v-show="!loading" id="race-map" class="mt-2 h-[60vh] rounded-xl border border-slate-800 shadow-lg"></div>

    <div class="mt-2 flex flex-wrap gap-3 text-xs text-slate-500">
      <span class="flex items-center gap-1"><span class="text-amber-400">●</span> Stanoviště</span>
      <span class="flex items-center gap-1"><span class="text-blue-500">●</span> GPS poloha</span>
    </div>
  </div>
</template>

<style>
.gps-marker {
  background: none !important;
  border: none !important;
}
.gps-marker div {
  width: 20px;
  height: 20px;
  background: #3b82f6;
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 0 0 2px #3b82f6, 0 0 12px rgba(59,130,246,0.5);
}
</style>
