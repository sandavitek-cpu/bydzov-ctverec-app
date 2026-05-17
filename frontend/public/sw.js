const CACHE = 'bydzov-ctverec-v1'
const ASSETS = [
  '/bydzov-ctverec-app/',
  '/bydzov-ctverec-app/index.html',
  '/bydzov-ctverec-app/manifest.json',
]

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(ASSETS))
  )
})

self.addEventListener('fetch', (e) => {
  if (e.request.url.includes('/api/')) return
  e.respondWith(
    caches.match(e.request).then((cached) => cached || fetch(e.request))
  )
})
