import { createRouter, createWebHistory } from 'vue-router'
import AuthPage from '../views/AuthPage.vue'
import DashboardPage from '../views/DashboardPage.vue'
import EditorPage from '../views/EditorPage.vue'
import ProfilePage from '../views/ProfilePage.vue'
import SearchPage from '../views/SearchPage.vue'
import KbHomePage from '../views/KbHomePage.vue'
import UserHomePage from '../views/UserHomePage.vue'
import SettingsPage from '../views/SettingsPage.vue'

const ADMIN_APP_URL = import.meta.env.VITE_ADMIN_APP_URL || 'http://localhost:5181'

function openAdminApp() {
  window.location.href = ADMIN_APP_URL
}

const routes = [
  { path: '/auth', component: AuthPage, meta: { public: true } },
  { path: '/', component: DashboardPage },
  { path: '/editor/:kbId/:docId?', component: EditorPage },
  { path: '/search', component: SearchPage },
  { path: '/kb/:kbId', component: KbHomePage },
  { path: '/user/:userId', component: UserHomePage },
  { path: '/settings/:kbId?', component: SettingsPage },
  { path: '/profile', component: ProfilePage },
  {
    path: '/admin/:pathMatch(.*)*',
    meta: { public: true },
    beforeEnter: () => {
      openAdminApp()
      return false
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }

  const token = localStorage.getItem('wiki-token')
  if (!token) {
    return '/auth'
  }

  return true
})

export default router
