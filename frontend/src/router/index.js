import { createRouter, createWebHistory } from 'vue-router'
import AuthPage from '../views/AuthPage.vue'
import DashboardPage from '../views/DashboardPage.vue'
import EditorPage from '../views/EditorPage.vue'
import ProfilePage from '../views/ProfilePage.vue'
import SearchPage from '../views/SearchPage.vue'
import KbHomePage from '../views/KbHomePage.vue'
import UserHomePage from '../views/UserHomePage.vue'
import SettingsPage from '../views/SettingsPage.vue'

function readStoredUserRole() {
  try {
    const user = JSON.parse(localStorage.getItem('wiki-user') || 'null')
    return user?.role || ''
  } catch {
    return ''
  }
}

function readTokenRole(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(normalized)
        .split('')
        .map(ch => `%${(`00${ch.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    )
    return JSON.parse(json)?.role || ''
  } catch {
    return ''
  }
}

function isAdminToken(token) {
  return readStoredUserRole() === 'ADMIN' || readTokenRole(token) === 'ADMIN'
}

const routes = [
  { path: '/auth', component: AuthPage, meta: { public: true } },
  { path: '/', component: DashboardPage },
  { path: '/editor/:kbId/:docId?', component: EditorPage },
  { path: '/search', component: SearchPage },
  { path: '/kb/:kbId', component: KbHomePage },
  { path: '/user/:userId', component: UserHomePage },
  { path: '/settings/:kbId?', component: SettingsPage },
  { path: '/profile', component: ProfilePage }
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
    return { path: '/auth', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
