<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { apiBaseUrl } from '@/api'
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
})

const localPoints = ref<RoutePointData[]>([])

const mapContainer = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let polyline: L.Polyline | null = null
let markers: L.Marker[] = []

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
  form.value = { name: '', variant: 'JEDNODENNI' }
  localPoints.value = []
  nextTick(() => initMap())
}

function startEdit(route: RouteData) {
  editing.value = true
  editingRoute.value = route
  form.value = { name: route.name, variant: route.variant }
  localPoints.value = route.points.map(p => ({ ...p }))
  nextTick(() => initMap())
}

function cancelEdit() {
  editing.value = false
  editingRoute.value = null
  form.value = { name: '', variant: 'JEDNODENNI' }
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
        body: JSON.stringify({ name: form.value.name }),
      })
      if (res.status === 403) { logout(); router.push('/admin/login'); return }
      if (!res.ok) throw new Error((await res.json()).error ?? 'Chyba uložení')
      routeId = editingRoute.value.id
      for (const pt of editingRoute.value.points) {
        if (pt.id) {
          await fetch(`${apiBaseUrl}/api/admin/routes/${routeId}/points/${pt.id}`, {
            method: 'DELETE',
            headers: h,
          })
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
      if (!res.ok) throw new Error('Chyba přidání bodu')
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
          <div class="text-body-sm text-text-soft">
            Kliknutím do mapy přidáš bod. Tažením bod přemístíš. Pravým kliknutím nebo vyskakovacím oknem bod smažeš.
          </div>
          <div ref="mapContainer" class="h-80 w-full rounded-lg border border-border"></div>
          <div v-if="localPoints.length > 0" class="space-y-1">
            <div class="flex items-center justify-between">
              <span class="text-meta font-medium text-text">Body trasy ({{ localPoints.length }})</span>
              <span class="text-meta text-text-soft">Celkem: {{ formatDistance(totalEditDistance) }}</span>
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
          <div class="flex gap-3">
            <button type="submit" :disabled="saving" class="btn-primary btn-sm">
              {{ saving ? 'Ukládám…' : 'Uložit trasu' }}
            </button>
            <button type="button" @click="cancelEdit" class="btn-secondary btn-sm">
              Zrušit
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Route list -->
    <p v-if="loading" class="text-body text-text-soft py-8 text-center">Načítám…</p>

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

<style scoped>
details > summary {
  list-style: none;
}
details > summary::-webkit-details-marker {
  display: none;
}
</style>
