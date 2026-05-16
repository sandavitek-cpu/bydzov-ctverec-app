const routeCache = new Map<string, [number, number][]>()
const addressCache = new Map<string, string>()

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
    routeCache.set(key, result)
    return result
  } catch {
    return toLatLng(points)
  }
}

function toLatLng(points: { lat: number; lng: number }[]): [number, number][] {
  return points.map(p => [p.lat, p.lng])
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
      addressCache.set(key, `${lat.toFixed(4)}, ${lng.toFixed(4)}`)
      return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
    }
    const a = data.address || {}
    const parts = [a.road || a.pedestrian || a.cycleway || '', a.house_number || ''].filter(Boolean)
    const street = parts.join(' ') || data.name || ''
    const village = a.village || a.town || a.city || a.municipality || ''
    const addr = [street, village].filter(Boolean).join(', ') || data.display_name || `${lat.toFixed(4)}, ${lng.toFixed(4)}`
    addressCache.set(key, addr)
    return addr
  } catch {
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  }
}
