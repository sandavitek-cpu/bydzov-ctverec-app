import L from 'leaflet'

const routeCache = new Map<string, [number, number][]>()
const addressCache = new Map<string, string>()
const MAX_CACHE_SIZE = 200

function cacheSet<K, V>(cache: Map<K, V>, key: K, value: V) {
  if (cache.size >= MAX_CACHE_SIZE) {
    const firstKey = cache.keys().next().value
    if (firstKey !== undefined) cache.delete(firstKey)
  }
  cache.set(key, value)
}

export async function fetchRoadRoute(points: { lat: number; lng: number }[]): Promise<[number, number][]> {
  if (points.length < 2) return []
  const key = points.map(p => `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`).join('|')
  const cached = routeCache.get(key)
  if (cached) return cached

  const coords = points.map(p => `${p.lng},${p.lat}`).join(';')
  const url = `https://router.project-osrm.org/route/v1/driving/${coords}?geometries=geojson&overview=full`
  try {
    const res = await fetch(url)
    const data = await res.json()
    if (data.code !== 'Ok') return toLatLng(points)
    const result: [number, number][] = data.routes[0].geometry.coordinates.map(
      (c: [number, number]) => [c[1], c[0]]
    )
    cacheSet(routeCache, key, result)
    return result
  } catch {
    return toLatLng(points)
  }
}

function toLatLng(points: { lat: number; lng: number }[]): [number, number][] {
  return points.map(p => [p.lat, p.lng])
}

export function addFullscreenControl(map: L.Map) {
  const FullscreenControl = L.Control.extend({
    onAdd: function () {
      const btn = L.DomUtil.create('button', 'leaflet-bar')
      btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 3H5a2 2 0 00-2 2v3m18 0V5a2 2 0 00-2-2h-3m0 18h3a2 2 0 002-2v-3M3 16v3a2 2 0 002 2h3"/></svg>'
      btn.title = 'Celá obrazovka'
      btn.style.cssText = 'width:34px;height:34px;background:#fff;border:2px solid rgba(0,0,0,0.2);border-radius:4px;cursor:pointer;display:flex;align-items:center;justify-content:center;color:#333;margin-top:4px'
      const container = map.getContainer()
      btn.onclick = function () {
        if (document.fullscreenElement) {
          document.exitFullscreen()
        } else {
          container.requestFullscreen?.()
        }
      }
      document.addEventListener('fullscreenchange', () => {
        setTimeout(() => map.invalidateSize(), 300)
      })
      return btn
    },
  })
  new FullscreenControl({ position: 'topright' }).addTo(map)
}

export function addLocateControl(map: L.Map, getPosition?: () => { lat: number; lng: number } | null) {
  const LocateControl = L.Control.extend({
    onAdd: function () {
      const btn = L.DomUtil.create('button', 'leaflet-bar')
      btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>'
      btn.title = 'Vycentrovat na moji polohu'
      btn.style.cssText = 'width:34px;height:34px;background:#fff;border:2px solid rgba(0,0,0,0.2);border-radius:4px;cursor:pointer;display:flex;align-items:center;justify-content:center;color:#333;transition:background 0.2s'
      L.DomEvent.disableClickPropagation(btn)
      btn.onclick = function (e) {
        L.DomEvent.stopPropagation(e)
        if (!('geolocation' in navigator)) return

        if (getPosition) {
          const pos = getPosition()
          if (pos) {
            map.setView([pos.lat, pos.lng], map.getZoom())
            return
          }
        }

        navigator.geolocation.getCurrentPosition(
          (pos) => {
            map.setView([pos.coords.latitude, pos.coords.longitude], map.getZoom())
            btn.style.background = '#dcfce7'
            setTimeout(() => { btn.style.background = '#fff' }, 500)
          },
          () => {
            btn.style.background = '#fee2e2'
            setTimeout(() => { btn.style.background = '#fff' }, 800)
          },
          { enableHighAccuracy: true, timeout: 10000 },
        )
      }
      return btn
    },
  })
  new LocateControl({ position: 'topright' }).addTo(map)
}

export async function reverseGeocode(lat: number, lng: number): Promise<string> {
  const key = `${lat.toFixed(5)},${lng.toFixed(5)}`
  const cached = addressCache.get(key)
  if (cached) return cached

  const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&addressdetails=1&accept-language=cs`
  try {
    const res = await fetch(url, { headers: { 'User-Agent': 'BydzovCtverec/1.0' } })
    const data = await res.json()
    if (data.error) {
      cacheSet(addressCache, key, `${lat.toFixed(4)}, ${lng.toFixed(4)}`)
      return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
    }
    const a = data.address || {}
    const parts = [a.road || a.pedestrian || a.cycleway || '', a.house_number || ''].filter(Boolean)
    const street = parts.join(' ') || data.name || ''
    const village = a.village || a.town || a.city || a.municipality || ''
    const addr = [street, village].filter(Boolean).join(', ') || data.display_name || `${lat.toFixed(4)}, ${lng.toFixed(4)}`
    cacheSet(addressCache, key, addr)
    return addr
  } catch {
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  }
}
