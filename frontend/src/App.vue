<template>
  <div class="app-shell">
    <aside v-if="!isPublicRoute" class="side panel">
      <h2>Wiki</h2>
      <router-link to="/">工作台</router-link>
      <router-link to="/profile">个人中心</router-link>
      <router-link to="/recycle">回收站</router-link>
      <router-link v-if="auth.isAdmin" to="/admin/logs">管理日志</router-link>
      <button class="btn" @click="toggleTheme">主题：{{ theme.mode }}</button>
      <button class="btn" @click="logout">退出登录</button>
    </aside>
    <main :class="['main', { full: isPublicRoute }]">
      <router-view />
    </main>
    <LoadingMask />
    <ErrorDialog />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './store/auth'
import { useThemeStore } from './store/theme'
import ErrorDialog from './components/ErrorDialog.vue'
import LoadingMask from './components/LoadingMask.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const isPublicRoute = computed(() => route.meta.public)

onMounted(() => theme.apply())

function toggleTheme() {
  const order = ['system', 'light', 'dark']
  const index = order.indexOf(theme.mode)
  theme.apply(order[(index + 1) % order.length])
}

function logout() {
  auth.logout()
  router.push('/auth')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
  padding: 12px;
}

.side {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: calc(100vh - 24px);
  position: sticky;
  top: 12px;
}

.side h2 {
  margin: 0 0 6px;
}

.side a {
  color: var(--text);
  text-decoration: none;
  padding: 8px 10px;
  border-radius: 8px;
}

.side a.router-link-exact-active {
  background: var(--brand-soft);
  color: var(--brand);
}

.main {
  min-width: 0;
}

.main.full {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
    height: auto;
  }
}
</style>
