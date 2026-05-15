import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RegistrationView from '../views/RegistrationView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminCheckpointsView from '../views/admin/AdminCheckpointsView.vue'
import AdminCommunicationsView from '../views/admin/AdminCommunicationsView.vue'
import AdminLoggingView from '../views/admin/AdminLoggingView.vue'
import JudgeScoringView from '../views/JudgeScoringView.vue'
import ResultsView from '../views/ResultsView.vue'
import ArchiveView from '../views/ArchiveView.vue'
import RacerItineraryView from '../views/RacerItineraryView.vue'
import RacerStandingView from '../views/RacerStandingView.vue'
import RacerMapView from '../views/RacerMapView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/registrace', name: 'registration', component: RegistrationView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/prihlaseni', name: 'admin-dashboard', component: AdminDashboardView },
    { path: '/admin/stanoviste', name: 'admin-checkpoints', component: AdminCheckpointsView },
    { path: '/admin/komunikace', name: 'admin-communications', component: AdminCommunicationsView },
    { path: '/admin/logovani', name: 'admin-logging', component: AdminLoggingView },
    { path: '/rozhodci', name: 'judge-scoring', component: JudgeScoringView },
    { path: '/vysledky/:rok', name: 'results', component: ResultsView },
    { path: '/archiv', name: 'archive', component: ArchiveView },
    { path: '/archiv/:rok', name: 'archive-year', component: ArchiveView },
    { path: '/zavodnik/itinerar', name: 'racer-itinerary', component: RacerItineraryView },
    { path: '/zavodnik/stav', name: 'racer-standing', component: RacerStandingView },
    { path: '/zavodnik/mapa', name: 'racer-map', component: RacerMapView },
  ],
})

export default router
