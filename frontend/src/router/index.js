import { createRouter, createWebHistory } from 'vue-router'
import AuthPage from '../views/AuthPage.vue'
import DashboardPage from '../views/DashboardPage.vue'
import EditorPage from '../views/EditorPage.vue'
import RecyclePage from '../views/RecyclePage.vue'
import AdminLogsPage from '../views/AdminLogsPage.vue'
import SharePage from '../views/SharePage.vue'
import ProfilePage from '../views/ProfilePage.vue'

const routes = [
  { path: '/auth', component: AuthPage, meta: { public: true } },
  { path: '/share/:token', component: SharePage, meta: { public: true } },
  { path: '/', component: DashboardPage },
  { path: '/editor/:kbId/:docId?', component: EditorPage },
  { path: '/recycle', component: RecyclePage },
  { path: '/profile', component: ProfilePage },
  { path: '/admin/logs', component: AdminLogsPage, meta: { admin: true } }
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

  if (to.meta.admin) {
    const user = JSON.parse(localStorage.getItem('wiki-user') || 'null')
    if (user?.role !== 'ADMIN') {
      return '/'
    }
  }

  return true
})

export default router
