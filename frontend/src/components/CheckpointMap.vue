<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { fetchRoadRoute, addLocateControl, addFullscreenControl } from '@/utils/mapUtils'

const props = defineProps<{
  lat: number
  lng: number
  checkpoints: { id?: number; name: string; lat: number; lng: number; sortOrder: number }[]
  routeLines?: { points: { lat: number; lng: number }[]; color: string }[]
}>()

const emit = defineEmits<{
  click: [lat: number, lng: number]
  selectCheckpoint: [id: number]
}>()

const container = ref<HTMLElement | null>(null)
let map: L.Map | null = null
let marker: L.Marker | null = null
let cpMarkers: L.Marker[] = []
let polylines: L.Polyline[] = []

function initMap() {
  if (!container.value || map) return

  map = L.map(container.value, {
    center: [props.lat, props.lng],
    zoom: 14,
    zoomControl: true,
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(map)

  addLocateControl(map)
  addFullscreenControl(map)

  const icon = L.divIcon({
    html: '<div style="width:16px;height:16px;background:#09097B;border:3px solid #fff;border-radius:50%;box-shadow:0 1px 4px rgba(0,0,0,0.3)"></div>',
    iconSize: [16, 16],
    iconAnchor: [8, 8],
    className: '',
  })

  marker = L.marker([props.lat, props.lng], { draggable: true, icon }).addTo(map)

  marker.on('dragend', () => {
    const pos = marker!.getLatLng()
    emit('click', pos.lat, pos.lng)
  })

  map.on('click', (e: L.LeafletMouseEvent) => {
    marker!.setLatLng(e.latlng)
    emit('click', e.latlng.lat, e.latlng.lng)
  })

  setTimeout(() => map?.invalidateSize(), 200)
}

function updateCenter() {
  if (!map || !marker) return
  marker.setLatLng([props.lat, props.lng])
  map.setView([props.lat, props.lng], map.getZoom())
}

function updateCpMarkers() {
  if (!map) return
  cpMarkers.forEach(m => m.remove())
  cpMarkers = []

  const icon = L.divIcon({
    html: '<div style="width:12px;height:12px;background:#6F7750;border:2px solid #fff;border-radius:50%;box-shadow:0 1px 3px rgba(0,0,0,0.3)"></div>',
    iconSize: [12, 12],
    iconAnchor: [6, 6],
    className: '',
  })

  props.checkpoints.forEach(cp => {
    const m = L.marker([cp.lat, cp.lng], { icon })
      .addTo(map!)
      .bindTooltip(`${cp.sortOrder}. ${cp.name}`, { direction: 'top' })
    if (cp.id != null) {
      m.on('click', () => emit('selectCheckpoint', cp.id!))
    }
    cpMarkers.push(m)
  })
}

async function updateRouteLines() {
  if (!map) return
  polylines.forEach(p => p.remove())
  polylines = []

  if (!props.routeLines || props.routeLines.length === 0) return

  const allLatLngs: [number, number][] = []
  for (const rl of props.routeLines) {
    if (rl.points.length < 2) continue
    const geo = await fetchRoadRoute(rl.points)
    if (geo.length > 0) {
      const pl = L.polyline(geo, {
        color: rl.color,
        weight: 3,
        opacity: 0.7,
      }).addTo(map)
      polylines.push(pl)
      allLatLngs.push(...geo)
    }
  }

  if (allLatLngs.length > 0) {
    const bounds = L.latLngBounds(allLatLngs.map(p => L.latLng(p[0], p[1])))
    map.fitBounds(bounds.pad(0.15))
  }
}

watch(() => [props.lat, props.lng], updateCenter)
watch(() => props.checkpoints, updateCpMarkers, { deep: true })
watch(() => props.routeLines, updateRouteLines, { deep: true })

onMounted(() => {
  initMap()
  setTimeout(updateCpMarkers, 300)
  setTimeout(updateRouteLines, 500)
})

onUnmounted(() => {
  map?.remove()
  map = null
})
</script>

<template>
  <div ref="container" class="h-60 sm:h-72 md:h-80 w-full rounded-lg border border-border"></div>
</template>
