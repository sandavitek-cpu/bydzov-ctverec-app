<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
import { fetchRoadRoute } from '@/utils/mapUtils'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const router = useRouter()
const { isLoggedIn, token } = useAuth()

interface RoutePointData {
  lat: number
  lng: number
  distanceFromStart: number | null
}

interface CheckpointScoreData {
  name: string
  lat: number
  lng: number
  radius: number
  sortOrder: number
  taskDescription: string | null
  maxPoints: number | null
  scorePoints: number | null
}

interface RacerMapResponse {
  routePoints: RoutePointData[]
  checkpoints: CheckpointScoreData[]
  totalDistance: number
  totalScore: number
  rank: number
  totalRacers: number
  teamName: string
  startNumber: number
}

const mapData = ref<RacerMapResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const gpsActive = ref(false)
const gpsError = ref<string | null>(null)
let map: L.Map | null = null
let marker: L.Marker | null = null
let routeLine: L.Polyline | null = null
let watchId: number | null = null

const icon = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
})

async function loadMapData() {
  try {
    const h: Record<string, string> = { Accept: 'application/json' }
    if (token.value) h['Authorization'] = `Bearer ${token.value}`
    const res = await fetch(`${apiBaseUrl}/api/racer/map`, { headers: h })
    if (!res.ok) throw new Error(`API ${res.status}`)
    mapData.value = await res.json()
  } catch (e: any) {
    error.value = e.message
  }
}

onMounted(async () => {
  await loadMapData()
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

  if (mapData.value?.routePoints && mapData.value.routePoints.length > 1) {
    const raw = mapData.value.routePoints.map(p => ({ lat: p.lat, lng: p.lng }))
    const roadRoute = await fetchRoadRoute(raw)
    routeLine = L.polyline(roadRoute, {
      color: '#dc2626',
      weight: 4,
      opacity: 0.8,
    }).addTo(map)
    map.fitBounds(routeLine.getBounds().pad(0.1))
  }

  for (const cp of mapData.value?.checkpoints ?? []) {
    const scored = cp.scorePoints !== null
    L.circle([cp.lat, cp.lng], {
      radius: cp.radius,
      color: scored ? '#16a34a' : '#09097B',
      fillColor: scored ? '#16a34a' : '#09097B',
      fillOpacity: 0.08,
      weight: 2,
    }).addTo(map)
    const label = scored ? `${cp.name} (${cp.scorePoints}b)` : cp.name
    L.marker([cp.lat, cp.lng], { icon }).addTo(map).bindPopup(`<b>${label}</b>`)
  }

  if (!mapData.value?.routePoints || mapData.value.routePoints.length <= 1) {
    map.setView([50.2415, 15.49], 14)
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
          map!.setView(latlng, map!.getZoom())
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
    <div class="flex items-center justify-between mb-4">
      <h1 class="text-page-title text-text">Mapa trasy</h1>
      <div class="flex items-center gap-2 text-meta text-text-soft">
        <span class="flex items-center gap-1">
          <span class="inline-block h-2 w-2 rounded-full" :class="gpsActive ? 'bg-success' : 'bg-border-strong'"></span>
          GPS
        </span>
        <span v-if="gpsError" class="text-warning">{{ gpsError }}</span>
      </div>
    </div>

    <div v-if="loading" class="flex h-[60vh] items-center justify-center rounded-xl border border-border text-body-sm text-text-soft">
      Načítám mapu…
    </div>

    <div v-if="error" class="mb-3 rounded-lg border border-warning bg-warning/10 px-4 py-2 text-warning text-body-sm">
      {{ error }}
    </div>

    <div v-show="!loading" class="flex flex-col lg:flex-row gap-4">
      <div id="race-map" class="h-[60vh] rounded-xl border border-border shadow-sm lg:w-3/5"></div>

      <div class="lg:w-2/5 space-y-3">
        <div v-if="mapData" class="rounded-xl border border-border p-4">
          <h2 class="text-body font-semibold text-text mb-2">{{ mapData.teamName }} (č. {{ mapData.startNumber }})</h2>
          <div class="flex flex-wrap gap-x-6 gap-y-1 text-meta text-text-soft">
            <span>Skóre: <strong class="text-text">{{ mapData.totalScore }}</strong></span>
            <span>Pořadí: <strong class="text-text">{{ mapData.rank }}. / {{ mapData.totalRacers }}</strong></span>
            <span v-if="mapData.totalDistance > 0">Trasa: <strong class="text-text">{{ mapData.totalDistance.toFixed(1) }} km</strong></span>
          </div>
        </div>

        <div class="rounded-xl border border-border p-4">
          <h3 class="text-body-sm font-semibold text-text mb-2">Stanoviště</h3>
          <div class="space-y-1.5 max-h-[30vh] overflow-y-auto">
            <div v-for="cp in mapData?.checkpoints ?? []" :key="cp.sortOrder"
              class="flex items-center justify-between rounded-lg px-3 py-1.5 text-meta"
              :class="cp.scorePoints !== null ? 'bg-success/5' : 'bg-fill'">
              <div class="flex items-center gap-2 min-w-0">
                <span class="shrink-0 flex h-5 w-5 items-center justify-center rounded-full text-[11px] font-bold"
                  :class="cp.scorePoints !== null ? 'bg-success text-white' : 'bg-border-strong text-text-soft'">
                  {{ cp.sortOrder }}
                </span>
                <span class="truncate">{{ cp.name }}</span>
              </div>
              <span v-if="cp.scorePoints !== null" class="shrink-0 font-semibold text-text">{{ cp.scorePoints }} b</span>
              <span v-else class="shrink-0 text-text-soft">–</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-3 flex flex-wrap gap-4 text-meta text-text-soft">
      <span class="flex items-center gap-1.5">
        <span class="inline-block h-3 w-3 rounded-full border-2 border-primary bg-primary/10"></span>
        Stanoviště
      </span>
      <span class="flex items-center gap-1.5">
        <span class="inline-block h-3 w-3 rounded-full bg-green-600"></span>
        Splněno
      </span>
      <span class="flex items-center gap-1.5">
        <span class="inline-block h-3 w-3 rounded-full bg-red-500"></span>
        Trasa
      </span>
      <span class="flex items-center gap-1.5">
        <span class="inline-block h-3 w-3 rounded-full border-2 border-blue-600 bg-blue-500"></span>
        GPS poloha
      </span>
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
