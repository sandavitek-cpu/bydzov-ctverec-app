import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RegistrationView from '../views/RegistrationView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminCheckpointsView from '../views/admin/AdminCheckpointsView.vue'
import AdminCommunicationsView from '../views/admin/AdminCommunicationsView.vue'
import AdminLoggingView from '../views/admin/AdminLoggingView.vue'
import AdminRolesView from '../views/admin/AdminRolesView.vue'
import AdminUsersView from '../views/admin/AdminUsersView.vue'
import AdminRoutesView from '../views/admin/AdminRoutesView.vue'
import AdminChangelogView from '../views/admin/AdminChangelogView.vue'
import AdminCheckpointScoresView from '../views/admin/AdminCheckpointScoresView.vue'
import AdminVariantsView from '../views/admin/AdminVariantsView.vue'
import AdminCategoriesView from '../views/admin/AdminCategoriesView.vue'
import AdminScheduleView from '../views/admin/AdminScheduleView.vue'
import CeremonyView from '../views/CeremonyView.vue'
import JudgeScoringView from '../views/JudgeScoringView.vue'
import JudgeDashboardView from '../views/JudgeDashboardView.vue'
import ResultsView from '../views/ResultsView.vue'
import ArchiveView from '../views/ArchiveView.vue'
import RacerItineraryView from '../views/RacerItineraryView.vue'
import RacerStandingView from '../views/RacerStandingView.vue'
import RacerMapView from '../views/RacerMapView.vue'
import RacerVehiclesView from '../views/RacerVehiclesView.vue'
import AccountSettingsView from '../views/AccountSettingsView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/registrace', name: 'registration', component: RegistrationView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/prihlaseni', name: 'admin-dashboard', component: AdminDashboardView, meta: { requiresAdmin: true } },
    { path: '/admin/stanoviste', name: 'admin-checkpoints', component: AdminCheckpointsView, meta: { requiresAdmin: true } },
    { path: '/admin/komunikace', name: 'admin-communications', component: AdminCommunicationsView, meta: { requiresAdmin: true } },
    { path: '/admin/logovani', name: 'admin-logging', component: AdminLoggingView, meta: { requiresAdmin: true } },
    { path: '/admin/role', name: 'admin-roles', component: AdminRolesView, meta: { requiresAdmin: true } },
    { path: '/admin/trasy', name: 'admin-routes', component: AdminRoutesView, meta: { requiresAdmin: true } },
    { path: '/admin/uzivatele', name: 'admin-users', component: AdminUsersView, meta: { requiresAdmin: true } },
    { path: '/admin/changelog', name: 'admin-changelog', component: AdminChangelogView, meta: { requiresAdmin: true } },
    { path: '/admin/bodovani', name: 'admin-scores', component: AdminCheckpointScoresView, meta: { requiresAdmin: true } },
    { path: '/admin/varianty', name: 'admin-variants', component: AdminVariantsView, meta: { requiresAdmin: true } },
    { path: '/admin/kategorie', name: 'admin-categories', component: AdminCategoriesView, meta: { requiresAdmin: true } },
    { path: '/admin/program', name: 'admin-schedule', component: AdminScheduleView, meta: { requiresAdmin: true } },
    { path: '/ceremoniál/:rok', name: 'ceremony', component: CeremonyView },
    { path: '/rozhodci', redirect: '/komisari' },
    { path: '/komisari', name: 'judge-scoring', component: JudgeScoringView },
    { path: '/komisari/prehled', name: 'judge-dashboard', component: JudgeDashboardView },
    { path: '/vysledky/:rok', name: 'results', component: ResultsView },
    { path: '/archiv', name: 'archive', component: ArchiveView },
    { path: '/archiv/:rok', name: 'archive-year', component: ArchiveView },
    { path: '/zavodnik/itinerar', name: 'racer-itinerary', component: RacerItineraryView },
    { path: '/zavodnik/stav', name: 'racer-standing', component: RacerStandingView },
    { path: '/zavodnik/mapa', name: 'racer-map', component: RacerMapView },
    { path: '/zavodnik/vozidla', name: 'racer-vehicles', component: RacerVehiclesView },
    { path: '/ucet', name: 'account', component: AccountSettingsView },
  ],
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

  if (to.path.startsWith('/komisari') && !token) {
    next('/admin/login')
    return
  }

  next()
})

export default router
