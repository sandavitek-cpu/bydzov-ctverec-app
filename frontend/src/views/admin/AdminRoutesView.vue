<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import { addLocateControl, addFullscreenControl, fetchRoadRoute } from '@/utils/mapUtils'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

interface RoutePointData {
  id?: number
  sortOrder: number
  lat: number
  lng: number
  distanceFromStart: number
}

interface RouteData {
  id: number
  variant: string
  name: string
  totalDistance: number
  published: boolean
  avgSpeedKmph: number
  points: RoutePointData[]
}

const router = useRouter()
const { isAdmin, authHeaders, logout } = useAuth()

const routes = ref<RouteData[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

const editing = ref(false)
const editingRoute = ref<RouteData | null>(null)

const form = ref({
  name: '',
  variant: 'JEDNODENNI',
  avgSpeedKmph: 30,
})

const localPoints = ref<RoutePointData[]>([])

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let polyline: L.Polyline | null = null
let markers: L.Marker[] = []
const osrmPreview = ref(false)
const osrmLoading = ref(false)
const importing = ref(false)

if (!isAdmin.value) {
  router.push('/admin/login')
}

function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLng = (lng2 - lng1) * Math.PI / 180
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function recalcPoints() {
  let cum = 0
  for (let i = 0; i < localPoints.value.length; i++) {
    localPoints.value[i].sortOrder = i + 1
    if (i === 0) {
      localPoints.value[i].distanceFromStart = 0
    } else {
      cum += haversine(
        localPoints.value[i - 1].lat, localPoints.value[i - 1].lng,
        localPoints.value[i].lat, localPoints.value[i].lng,
      )
      localPoints.value[i].distanceFromStart = Math.round(cum * 100) / 100
    }
  }
}

const totalEditDistance = computed(() => {
  if (localPoints.value.length < 2) return 0
  const last = localPoints.value[localPoints.value.length - 1]
  return Math.round(last.distanceFromStart)
})

function formatDistance(meters: number): string {
  if (meters >= 1000) return (meters / 1000).toFixed(2) + ' km'
  return Math.round(meters) + ' m'
}

function formatTime(minutes: number): string {
  if (minutes < 1) return '< 1 min'
  if (minutes < 60) return Math.round(minutes) + ' min'
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return m > 0 ? `${h}h ${m}min` : `${h}h`
}

function estimatedMinutes(distanceMeters: number, speedKmph: number): number {
  if (speedKmph <= 0 || distanceMeters <= 0) return 0
  return distanceMeters / 1000 / speedKmph * 60
}

const totalEditMinutes = computed(() => estimatedMinutes(totalEditDistance.value, form.value.avgSpeedKmph))

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/routes`, {
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    routes.value = await res.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba načítání'
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editing.value = true
  editingRoute.value = null
  form.value = { name: '', variant: 'JEDNODENNI', avgSpeedKmph: 30 }
  localPoints.value = []
  nextTick(() => initMap())
}

function startEdit(route: RouteData) {
  editing.value = true
  editingRoute.value = route
  form.value = { name: route.name, variant: route.variant, avgSpeedKmph: route.avgSpeedKmph }
  localPoints.value = route.points.map(p => ({ ...p }))
  nextTick(() => initMap())
}

function cancelEdit() {
  editing.value = false
  editingRoute.value = null
  form.value = { name: '', variant: 'JEDNODENNI', avgSpeedKmph: 30 }
  localPoints.value = []
  destroyMap()
}

async function saveRoute() {
  error.value = null
  saving.value = true
  try {
    const h = authHeaders()
    let routeId: number

    if (editingRoute.value) {
      const res = await fetch(`${apiBaseUrl}/api/admin/routes/${editingRoute.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...h },
        body: JSON.stringify({ name: form.value.name, avgSpeedKmph: String(form.value.avgSpeedKmph) }),
      })
      if (res.status === 403) { logout(); router.push('/admin/login'); return }
      if (!res.ok) throw new Error((await res.json()).error ?? 'Chyba uložení')
      routeId = editingRoute.value.id
      const oldPoints = [...editingRoute.value.points]
      for (const pt of oldPoints) {
        if (pt.id) {
          const delRes = await fetch(`${apiBaseUrl}/api/admin/routes/${routeId}/points/${pt.id}`, {
            method: 'DELETE',
            headers: h,
          })
          if (!delRes.ok) {
            error.value = 'Chyba při mazání starých bodů'
            saving.value = false
            return
          }
        }
      }
    } else {
      const res = await fetch(`${apiBaseUrl}/api/admin/routes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...h },
        body: JSON.stringify({ name: form.value.name, variant: form.value.variant }),
      })
      if (res.status === 403) { logout(); router.push('/admin/login'); return }
      if (!res.ok) throw new Error((await res.json()).error ?? 'Chyba vytvoření')
      const created = await res.json() as RouteData
      routeId = created.id
    }

    for (const pt of localPoints.value) {
      const res = await fetch(`${apiBaseUrl}/api/admin/routes/${routeId}/points`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...h },
        body: JSON.stringify({ lat: pt.lat, lng: pt.lng }),
      })
      if (!res.ok) {
        await load()
        throw new Error('Chyba přidání bodu')
      }
    }

    cancelEdit()
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba uložení'
  } finally {
    saving.value = false
  }
}

async function togglePublish(route: RouteData) {
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/routes/${route.id}/publish`, {
      method: 'POST',
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    if (!res.ok) throw new Error('Chyba publikování')
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba publikování'
  }
}

async function removeRoute(id: number) {
  if (!confirm('Opravdu smazat celou trasu včetně bodů?')) return
  try {
    const res = await fetch(`${apiBaseUrl}/api/admin/routes/${id}`, {
      method: 'DELETE',
      headers: { ...authHeaders() },
    })
    if (res.status === 403) { logout(); router.push('/admin/login'); return }
    if (!res.ok) throw new Error('Chyba smazání')
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Chyba smazání'
  }
}

async function toggleRoadPreview() {
  if (!map || localPoints.value.length < 2) return
  if (osrmPreview.value) {
    osrmPreview.value = false
    rebuildMap()
    return
  }
  osrmLoading.value = true
  try {
    const geo = await fetchRoadRoute(localPoints.value)
    if (geo.length > 0 && polyline) {
      osrmPreview.value = true
      polyline.setLatLngs(geo)
      polyline.setStyle({ color: '#dc2626', weight: 3, opacity: 0.7 })
      map.fitBounds(polyline.getBounds().pad(0.1))
    }
  } finally {
    osrmLoading.value = false
  }
}

function addLocalPoint(lat: number, lng: number) {
  localPoints.value.push({ sortOrder: 0, lat, lng, distanceFromStart: 0 })
  recalcPoints()
  rebuildMap()
}

function removeLocalPoint(index: number) {
  localPoints.value.splice(index, 1)
  recalcPoints()
  rebuildMap()
}

function updateLocalPoint(index: number, lat: number, lng: number) {
  localPoints.value[index].lat = lat
  localPoints.value[index].lng = lng
  recalcPoints()
  rebuildMap()
}

const fileInput = ref<HTMLInputElement | null>(null)

function triggerImport() {
  fileInput.value?.click()
}

function simplifyRdp(points: { lat: number; lng: number }[], epsilon: number): { lat: number; lng: number }[] {
  if (points.length <= 2) return points

  let dmax = 0
  let idx = 0
  const first = points[0]
  const last = points[points.length - 1]

  for (let i = 1; i < points.length - 1; i++) {
    const d = perpendicularDistance(points[i], first, last)
    if (d > dmax) { dmax = d; idx = i }
  }

  if (dmax > epsilon) {
    const left = simplifyRdp(points.slice(0, idx + 1), epsilon)
    const right = simplifyRdp(points.slice(idx), epsilon)
    return [...left.slice(0, -1), ...right]
  }

  return [first, last]
}

function perpendicularDistance(p: { lat: number; lng: number }, a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const dx = b.lng - a.lng
  const dy = b.lat - a.lat
  if (dx === 0 && dy === 0) return Math.hypot(p.lat - a.lat, p.lng - a.lng)
  return Math.abs(dy * (p.lng - a.lng) - dx * (p.lat - a.lat)) / Math.hypot(dx, dy)
}

function handleFileUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  importing.value = true
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const text = reader.result as string
      const parser = new DOMParser()
      const doc = parser.parseFromString(text, 'text/xml')
      const trkpts = doc.querySelectorAll('trkpt')
      const wpts = doc.querySelectorAll('wpt')
      const points: { lat: number; lng: number }[] = []
      trkpts.forEach(el => {
        const lat = parseFloat(el.getAttribute('lat') || '')
        const lon = parseFloat(el.getAttribute('lon') || '')
        if (!isNaN(lat) && !isNaN(lon)) points.push({ lat, lng: lon })
      })
      wpts.forEach(el => {
        const lat = parseFloat(el.getAttribute('lat') || '')
        const lon = parseFloat(el.getAttribute('lon') || '')
        if (!isNaN(lat) && !isNaN(lon)) points.push({ lat, lng: lon })
      })
      if (points.length === 0) {
        error.value = 'Soubor neobsahuje žádné body (trkpt/wpt)'
        return
      }
      let simplified = points
      if (points.length > 200) {
        simplified = simplifyRdp(points, 0.0001)
      }
      localPoints.value = simplified.map(p => ({ sortOrder: 0, lat: p.lat, lng: p.lng, distanceFromStart: 0 }))
      recalcPoints()
      rebuildMap()
    } finally {
      importing.value = false
      input.value = ''
    }
  }
  reader.onerror = () => { importing.value = false }
  reader.readAsText(file)
}

function exportGpx() {
  if (localPoints.value.length === 0) return
  const name = form.value.name || 'Trasa'
  const lines = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">',
    '  <trk>',
    `    <name>${name}</name>`,
    '    <trkseg>',
  ]
  for (const pt of localPoints.value) {
    lines.push(`      <trkpt lat="${pt.lat}" lon="${pt.lng}"></trkpt>`)
  }
  lines.push('    </trkseg>')
  lines.push('  </trk>')
  lines.push('</gpx>')
  const blob = new Blob([lines.join('\n')], { type: 'application/gpx+xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${name.replace(/\s+/g, '_')}.gpx`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function exportRouteGpx(route: RouteData) {
  if (route.points.length === 0) return
  const name = route.name
  const lines = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1">',
    '  <trk>',
    `    <name>${name}</name>`,
    '    <trkseg>',
  ]
  for (const pt of route.points) {
    lines.push(`      <trkpt lat="${pt.lat}" lon="${pt.lng}"></trkpt>`)
  }
  lines.push('    </trkseg>')
  lines.push('  </trk>')
  lines.push('</gpx>')
  const blob = new Blob([lines.join('\n')], { type: 'application/gpx+xml' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${name.replace(/\s+/g, '_')}.gpx`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function centerLat(): number {
  if (editing && localPoints.value.length > 0) {
    const avg = localPoints.value.reduce((a, p) => a + p.lat, 0) / localPoints.value.length
    return Math.round(avg * 10000) / 10000
  }
  return 50.2415
}

function centerLng(): number {
  if (editing && localPoints.value.length > 0) {
    const avg = localPoints.value.reduce((a, p) => a + p.lng, 0) / localPoints.value.length
    return Math.round(avg * 10000) / 10000
  }
  return 15.4900
}

function destroyMapFeatures() {
  markers.forEach(m => m.remove())
  markers = []
  if (polyline) { polyline.remove(); polyline = null }
}

function destroyMap() {
  destroyMapFeatures()
  if (map) { map.remove(); map = null }
}

function rebuildMap() {
  if (!map) return
  destroyMapFeatures()
  if (localPoints.value.length === 0) return

  const icon = L.divIcon({
    html: '<div style="width:14px;height:14px;background:#09097B;border:3px solid #fff;border-radius:50%;box-shadow:0 1px 4px rgba(0,0,0,0.3);cursor:grab"></div>',
    iconSize: [14, 14],
    iconAnchor: [7, 7],
    className: '',
  })

  const latlngs: L.LatLngTuple[] = []

  localPoints.value.forEach((pt, i) => {
    latlngs.push([pt.lat, pt.lng])
    const m = L.marker([pt.lat, pt.lng], { draggable: true, icon }).addTo(map!)
    m.bindTooltip(String(i + 1), { direction: 'top', permanent: true })
    m.bindPopup(`
      <div style="font-size:13px;line-height:1.6">
        <strong>Bod ${i + 1}</strong><br/>
        ${pt.lat.toFixed(5)}, ${pt.lng.toFixed(5)}<br/>
        <span style="color:#666">${formatDistance(pt.distanceFromStart)} od startu</span><br/>
        <span style="color:#999;font-size:11px">Pravým kliknutím smažeš</span>
      </div>
    `)
    m.on('dragend', () => {
      const pos = m.getLatLng()
      updateLocalPoint(i, pos.lat, pos.lng)
    })
    m.on('contextmenu', () => removeLocalPoint(i))
    markers.push(m)
  })

  polyline = L.polyline(latlngs, {
    color: '#09097B',
    weight: 3,
    opacity: 0.7,
  }).addTo(map!)
}

function initMap() {
  if (!mapContainer.value) return
  destroyMap()

  map = L.map(mapContainer.value, {
    center: [centerLat(), centerLng()],
    zoom: 14,
    zoomControl: true,
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(map)

  addLocateControl(map)

  addFullscreenControl(map)

  map.on('click', (e: L.LeafletMouseEvent) => {
    addLocalPoint(e.latlng.lat, e.latlng.lng)
  })

  rebuildMap()

  setTimeout(() => map?.invalidateSize(), 200)
}

onMounted(async () => {
  await load()
  if (editing.value) {
    nextTick(() => initMap())
  }
})

onUnmounted(() => {
  destroyMap()
})
</script>

<template>
  <div>
    <div class="flex items-center justify-between gap-4 mb-6">
      <div>
        <h1 class="text-page-title text-text">Trasy</h1>
        <p class="text-body-sm text-text-soft">{{ routes.length }} tras</p>
      </div>
      <div class="flex gap-2">
        <button v-if="!editing" @click="startCreate" class="btn-primary btn-sm">
          Přidat trasu
        </button>
      </div>
    </div>

    <p v-if="error" class="mb-4 text-body-sm text-error">{{ error }}</p>

    <!-- Editing form -->
    <div v-if="editing" class="mb-6">
      <div class="card mb-4">
        <h2 class="text-subsection text-text mb-4">
          {{ editingRoute ? 'Upravit trasu' : 'Nová trasa' }}
        </h2>
        <form @submit.prevent="saveRoute" class="space-y-4">
          <div>
            <label class="input-label">Název</label>
            <input v-model="form.name" required class="input-field" />
          </div>
          <div>
            <label class="input-label">Varianta</label>
            <select v-model="form.variant" :disabled="!!editingRoute" class="input-field">
              <option value="JEDNODENNI">Jednodenní</option>
              <option value="DVODENNI">Dvoudenní</option>
            </select>
          </div>
          <div>
            <label class="input-label">Průměrná rychlost (km/h)</label>
            <input v-model.number="form.avgSpeedKmph" type="number" min="1" max="200" class="input-field w-32" />
          </div>
          <div class="text-body-sm text-text-soft">
            Kliknutím do mapy přidáš bod. Tažením bod přemístíš. Pravým kliknutím nebo vyskakovacím oknem bod smažeš.
          </div>
          <div ref="mapContainer" class="h-60 sm:h-72 md:h-80 w-full rounded-lg border border-border"></div>
          <div v-if="localPoints.length > 0" class="space-y-1">
            <div class="flex items-center justify-between">
              <span class="text-meta font-medium text-text">Body trasy ({{ localPoints.length }})</span>
              <span class="text-meta text-text-soft">
                Celkem: {{ formatDistance(totalEditDistance) }}
                &middot; ~{{ formatTime(totalEditMinutes) }}
              </span>
            </div>
            <div class="max-h-48 overflow-y-auto space-y-1">
              <div v-for="(pt, i) in localPoints" :key="i"
                class="flex items-center gap-2 rounded bg-surface-strong px-3 py-1.5 text-meta"
              >
                <span class="flex h-5 w-5 items-center justify-center rounded-full bg-surface text-text-muted text-xs font-semibold">{{ i + 1 }}</span>
                <span class="font-mono text-text">{{ pt.lat.toFixed(5) }}, {{ pt.lng.toFixed(5) }}</span>
                <span class="text-text-soft ml-auto">{{ formatDistance(pt.distanceFromStart) }}</span>
                <button type="button" @click="removeLocalPoint(i)" class="btn-ghost !h-6 !w-6 !p-0 !text-error shrink-0">
                  <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div v-else class="text-body-sm text-text-soft py-4 text-center">
            Zatím žádné body – klikni do mapy pro přidání prvního bodu.
          </div>
          <div v-if="localPoints.length >= 2" class="flex items-center gap-2 text-meta text-text-soft">
            <button type="button" @click="toggleRoadPreview" :disabled="osrmLoading" class="btn-ghost btn-xs">
              {{ osrmLoading ? 'Načítám…' : osrmPreview ? 'Zpět na rovné čáry' : 'Zobrazit po silnici' }}
            </button>
            <span v-if="osrmPreview" class="inline-block h-2 w-2 rounded-full bg-success"></span>
          </div>
          <div class="flex flex-wrap gap-3">
            <button type="submit" :disabled="saving" class="btn-primary btn-sm">
              {{ saving ? 'Ukládám…' : 'Uložit trasu' }}
            </button>
            <button type="button" @click="triggerImport" :disabled="importing" class="btn-secondary btn-sm">
              {{ importing ? 'Importuji…' : 'Import GPX' }}
            </button>
            <button type="button" @click="exportGpx" :disabled="localPoints.length === 0" class="btn-secondary btn-sm">
              Export GPX
            </button>
            <button type="button" @click="cancelEdit" class="btn-secondary btn-sm">
              Zrušit
            </button>
          </div>
          <input ref="fileInput" type="file" accept=".gpx,.xml" @change="handleFileUpload" class="hidden" />
        </form>
      </div>
    </div>

    <!-- Route list -->
    <LoadingSpinner v-if="loading" />

    <div v-else-if="routes.length === 0 && !editing" class="card text-center text-text-soft py-8">
      Žádné trasy
    </div>

    <div v-else-if="!editing" class="space-y-3">
      <div v-for="route in routes" :key="route.id" class="card">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2">
              <span class="font-medium text-text">{{ route.name }}</span>
              <span class="rounded-full bg-surface-strong px-2 py-0.5 text-meta text-text-muted">
                {{ route.variant === 'JEDNODENNI' ? 'Jednodenní' : 'Dvoudenní' }}
              </span>
              <span v-if="route.published"
                class="rounded-full bg-accent-green/20 px-2 py-0.5 text-meta text-accent-green"
              >Publikováno</span>
              <span v-else
                class="rounded-full bg-surface-strong px-2 py-0.5 text-meta text-text-muted"
              >Nepublikováno</span>
            </div>
            <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-meta text-text-soft">
              <span>Vzdálenost: {{ formatDistance(route.totalDistance) }}</span>
              <span>{{ route.points.length }} bodů</span>
              <span>~{{ formatTime(estimatedMinutes(route.totalDistance, route.avgSpeedKmph)) }}</span>
            </div>
            <details class="mt-2">
              <summary class="text-meta text-primary cursor-pointer hover:underline">
                Body trasy ({{ route.points.length }})
              </summary>
              <div class="mt-1 space-y-0.5">
                <div v-for="pt in route.points" :key="pt.sortOrder"
                  class="flex items-center gap-2 rounded bg-surface-strong px-2.5 py-1 text-meta"
                >
                  <span class="text-text-muted">{{ pt.sortOrder }}.</span>
                  <span class="font-mono text-text">{{ pt.lat.toFixed(5) }}, {{ pt.lng.toFixed(5) }}</span>
                  <span class="text-text-soft ml-auto">{{ formatDistance(pt.distanceFromStart) }}</span>
                </div>
              </div>
            </details>
          </div>
          <div class="flex gap-1 shrink-0">
            <button @click="startEdit(route)" class="btn-ghost btn-sm !h-7 !px-2">
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button @click="togglePublish(route)" class="btn-ghost btn-sm !h-7 !px-2"
              :class="route.published ? '!text-accent-green' : '!text-text-muted'"
              :title="route.published ? 'Zrušit publikování' : 'Publikovat'"
            >
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path v-if="route.published" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z" />
              </svg>
            </button>
            <button @click="exportRouteGpx(route)" class="btn-ghost btn-sm !h-7 !px-2">
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
            </button>
            <button @click="removeRoute(route.id)" class="btn-ghost btn-sm !h-7 !px-2 !text-error">
              <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


