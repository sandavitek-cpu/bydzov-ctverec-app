import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { showApiErrorToast } from '@/composables/useToast'

const savedTheme = localStorage.getItem('theme')
if (savedTheme === 'dark' || (!savedTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
  document.documentElement.classList.add('dark')
}

const app = createApp(App)

app.config.errorHandler = (err, _instance, info) => {
  const msg = err instanceof Error ? err.message : String(err)
  console.error(`[Vue error] ${info}: ${msg}`, err)
  showApiErrorToast(msg)
}

app.use(router).mount('#app')
