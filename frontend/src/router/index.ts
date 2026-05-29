import { createRouter, createWebHashHistory } from 'vue-router'

// ── Lazy-loaded route components for code splitting ──
// HomeView is eagerly loaded for instant first render
const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
  },
  {
    path: '/registrace',
    name: 'registration',
    component: () => import('../views/RegistrationView.vue'),
  },
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('../views/admin/AdminLoginView.vue'),
  },
  {
    path: '/admin/prihlaseni',
    name: 'admin-dashboard',
    component: () => import('../views/admin/AdminDashboardView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/stanoviste',
    name: 'admin-checkpoints',
    component: () => import('../views/admin/AdminCheckpointsView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/komunikace',
    name: 'admin-communications',
    component: () => import('../views/admin/AdminCommunicationsView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/logovani',
    name: 'admin-logging',
    component: () => import('../views/admin/AdminLoggingView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/role',
    name: 'admin-roles',
    component: () => import('../views/admin/AdminRolesView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/trasy',
    name: 'admin-routes',
    component: () => import('../views/admin/AdminRoutesView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/uzivatele',
    name: 'admin-users',
    component: () => import('../views/admin/AdminUsersView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/bodovani',
    name: 'admin-scores',
    component: () => import('../views/admin/AdminCheckpointScoresView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/varianty',
    name: 'admin-variants',
    component: () => import('../views/admin/AdminVariantsView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/kategorie',
    name: 'admin-categories',
    component: () => import('../views/admin/AdminCategoriesView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/program',
    name: 'admin-schedule',
    component: () => import('../views/admin/AdminScheduleView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/ukoly',
    name: 'admin-tasks',
    component: () => import('../views/admin/AdminTasksView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/incidenty',
    name: 'admin-incidents',
    component: () => import('../views/admin/AdminIncidentsView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/zavodnik/ukoly',
    name: 'user-incidents',
    component: () => import('../views/UserIncidentsView.vue'),
  },
  {
    path: '/ceremoniál/:rok',
    name: 'ceremony',
    component: () => import('../views/CeremonyView.vue'),
  },
  {
    path: '/rozhodci',
    redirect: '/komisari',
  },
  {
    path: '/komisari',
    name: 'judge-scoring',
    component: () => import('../views/JudgeScoringView.vue'),
  },
  {
    path: '/komisari/prehled',
    name: 'judge-dashboard',
    component: () => import('../views/JudgeDashboardView.vue'),
  },
  {
    path: '/vysledky/:rok',
    name: 'results',
    component: () => import('../views/ResultsView.vue'),
  },
  {
    path: '/archiv',
    name: 'archive',
    component: () => import('../views/ArchiveView.vue'),
  },
  {
    path: '/archiv/:rok',
    name: 'archive-year',
    component: () => import('../views/ArchiveView.vue'),
  },
  {
    path: '/zavodnik/itinerar',
    name: 'racer-itinerary',
    component: () => import('../views/RacerItineraryView.vue'),
  },
  {
    path: '/zavodnik/stav',
    name: 'racer-standing',
    component: () => import('../views/RacerStandingView.vue'),
  },
  {
    path: '/zavodnik/mapa',
    name: 'racer-map',
    component: () => import('../views/RacerMapView.vue'),
  },
  {
    path: '/zavodnik/vozidla',
    name: 'racer-vehicles',
    component: () => import('../views/RacerVehiclesView.vue'),
  },
  {
    path: '/ucet',
    name: 'account',
    component: () => import('../views/AccountSettingsView.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  const role = localStorage.getItem('admin_role')
  const roles = role?.split(',') ?? []

  if (to.meta?.requiresAdmin) {
    if (!token || !roles.includes('ADMIN')) {
      next('/admin/login')
      return
    }
  }

  if (to.path.startsWith('/zavodnik/') && !token) {
    next('/admin/login')
    return
  }

  if (to.path.startsWith('/komisari')) {
    if (!token || (!roles.includes('ADMIN') && !roles.includes('JUDGE'))) {
      next('/admin/login')
      return
    }
  }

  next()
})

export default router
