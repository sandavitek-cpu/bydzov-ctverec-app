import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
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

const MAX_RECONNECT_ATTEMPTS = 10

export function useLiveResults(year: number) {
  const results = ref<LiveResultRow[]>([])
  const connected = ref(false)
  let stompClient: Client | null = null
  let pollingTimer: ReturnType<typeof setInterval> | null = null
  let reconnectAttempts = 0

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
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      connected.value = false
      pollingTimer = setInterval(fetchResults, 10000)
      return
    }
    reconnectAttempts++

    const wsUrl = apiBaseUrl.replace(/^http/, 'ws') + '/ws/results'
    stompClient = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        reconnectAttempts = 0
        stompClient?.subscribe('/topic/results', (message) => {
          try {
            const data = JSON.parse(message.body)
            if (data.year === year || !data.year) {
              results.value = data.results ?? []
            }
          } catch { /* ignore */ }
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      },
    })
    stompClient.activate()
  }

  async function start() {
    await fetchResults()
    connectWs()
  }

  function stop() {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
    if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
  }

  onUnmounted(stop)

  return { results, connected, start, stop }
}
