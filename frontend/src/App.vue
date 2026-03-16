<template>
  <div class="app-shell">
    <aside v-if="!isPublicRoute" class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="currentColor" opacity="0.3"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span class="logo-text">Wiki</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item">
          <span class="nav-icon">📊</span>
          <span>工作台</span>
        </router-link>
        <router-link to="/profile" class="nav-item">
          <span class="nav-icon">👤</span>
          <span>个人中心</span>
        </router-link>
        <router-link to="/recycle" class="nav-item">
          <span class="nav-icon">🗑️</span>
          <span>回收站</span>
        </router-link>
        <router-link v-if="auth.isAdmin" to="/admin/logs" class="nav-item">
          <span class="nav-icon">📋</span>
          <span>管理日志</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-text theme-toggle" @click="toggleTheme" :title="`当前主题: ${theme.mode}`">
          <span v-if="theme.mode === 'light'">☀️</span>
          <span v-else-if="theme.mode === 'dark'">🌙</span>
          <span v-else>🔄</span>
        </button>
        <button class="btn-text logout-btn" @click="logout" title="退出登录">
          <span>🚪</span>
        </button>
      </div>
    </aside>

    <main :class="['main-content', { full: isPublicRoute }]">
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
  display: flex;
}

.sidebar {
  width: 240px;
  background: var(--panel);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--line);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--brand);
  font-size: 18px;
  font-weight: 600;
}

.logo svg {
  flex-shrink: 0;
}

.logo-text {
  color: var(--text);
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--line-light);
  color: var(--text);
}

.nav-item.router-link-active {
  background: var(--brand-light);
  color: var(--brand);
}

.nav-icon {
  font-size: 18px;
  width: 20px;
  text-align: center;
}

.sidebar-footer {
  padding: 12px 8px;
  border-top: 1px solid var(--line);
  display: flex;
  gap: 8px;
  justify-content: center;
}

.theme-toggle,
.logout-btn {
  padding: 8px;
  font-size: 18px;
  border-radius: var(--radius);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  min-width: 0;
  padding: 24px;
}

.main-content.full {
  margin-left: 0;
}

@media (max-width: 900px) {
  .sidebar {
    width: 200px;
  }

  .main-content {
    margin-left: 200px;
    padding: 16px;
  }
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .main-content {
    margin-left: 0;
  }
}
</style>
