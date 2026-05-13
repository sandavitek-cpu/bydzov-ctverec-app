/**
 * Veřejné API — výchozí je Render (funguje hned po nasazení).
 * Až poběží vlastní doména, nastav v GitLab CI proměnnou VITE_API_BASE_URL.
 */
export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL ?? 'https://bydzov-ctverec-api.onrender.com'

export async function fetchCurrentEdition() {
  const res = await fetch(`${apiBaseUrl}/api/public/editions/current`)
  if (!res.ok) {
    throw new Error(`API ${res.status}`)
  }
  return res.json() as Promise<{ id: number; year: number; label: string }>
}
