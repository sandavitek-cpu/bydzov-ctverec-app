import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RegistrationView from '../views/RegistrationView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/registrace', name: 'registration', component: RegistrationView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/prihlaseni', name: 'admin-dashboard', component: AdminDashboardView },
  ],
})

export default router
