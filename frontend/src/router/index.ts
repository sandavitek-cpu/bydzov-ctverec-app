import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RegistrationView from '../views/RegistrationView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import JudgeScoringView from '../views/JudgeScoringView.vue'
import ResultsView from '../views/ResultsView.vue'
import RacerItineraryView from '../views/RacerItineraryView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/registrace', name: 'registration', component: RegistrationView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/prihlaseni', name: 'admin-dashboard', component: AdminDashboardView },
    { path: '/rozhodci', name: 'judge-scoring', component: JudgeScoringView },
    { path: '/vysledky/:rok', name: 'results', component: ResultsView },
    { path: '/zavodnik/itinerar', name: 'racer-itinerary', component: RacerItineraryView },
  ],
})

export default router
