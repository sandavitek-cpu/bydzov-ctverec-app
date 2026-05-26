import { ref, type Ref } from 'vue'
import { ApiError, authFetch } from '@/api'

export interface AsyncState<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  error: Ref<string | null>
  execute: (...args: unknown[]) => Promise<T | null>
}

export function useApi<T>(
  fetcher: () => Promise<T>
): AsyncState<T> {
  const data = ref<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function execute(): Promise<T | null> {
    loading.value = true
    error.value = null
    try {
      const result = await fetcher()
      data.value = result
      return result
    } catch (e) {
      const msg = e instanceof ApiError ? e.message
        : e instanceof Error ? e.message
        : 'Neočekávaná chyba'
      error.value = msg
      return null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, execute }
}

export function useApiSave<T>(
  fetcher: () => Promise<T>
): AsyncState<T> {
  return useApi(fetcher)
}

export async function safeFetch(url: string, options?: RequestInit): Promise<Response> {
  try {
    return await authFetch(url, options)
  } catch (e) {
    throw new ApiError(
      e instanceof TypeError ? 'Síťová chyba – zkontrolujte připojení' : 'Neočekávaná chyba',
      0
    )
  }
}
