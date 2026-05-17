import { ref, onUnmounted } from 'vue'
import { apiBaseUrl } from '@/api'

export interface LiveRunScore {
  checkpointOrder: number
  checkpointName: string
  points: number
}

export interface LiveResultRow {
  rank: number
  startNumber: number
  teamName: string
  vehicleCategory: string
  vehiclePlate: string
  totalPoints: number
  runs: LiveRunScore[]
}

export function useLiveResults(year: number) {
  const results = ref<LiveResultRow[]>([])
  const connected = ref(false)
  let ws: WebSocket | null = null
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  async function fetchResults() {
    try {
      const res = await fetch(`${apiBaseUrl}/api/public/results/${year}`)
      if (res.ok) {
        const data = await res.json()
        results.value = data.results ?? []
      }
    } catch {
      // silent
    }
  }

  function connectWs() {
    const wsUrl = apiBaseUrl.replace(/^http/, 'ws') + '/ws/results'
    try {
      ws = new WebSocket(wsUrl)
      ws.onopen = () => {
        connected.value = true
        ws?.send(JSON.stringify({ destination: '/topic/results' }))
      }
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.year === year || !data.year) {
            results.value = data.results ?? []
          }
        } catch { /* ignore */ }
      }
      ws.onclose = () => {
        connected.value = false
        setTimeout(connectWs, 5000)
      }
      ws.onerror = () => {
        ws?.close()
      }
    } catch {
      // WS failed, fall back to polling
      connected.value = false
      pollingTimer = setInterval(fetchResults, 10000)
    }
  }

  async function start() {
    await fetchResults()
    connectWs()
  }

  function stop() {
    if (ws) { ws.close(); ws = null }
    if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
  }

  onUnmounted(stop)

  return { results, connected, start, stop }
}
