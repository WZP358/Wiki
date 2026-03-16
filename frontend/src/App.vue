<template>
  <div class="app-shell">
    <aside v-if="!isPublicRoute" :class="['sidebar', { collapsed: sidebarCollapsed }]">
      <div class="sidebar-header">
        <div class="logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="currentColor" opacity="0.3"/>
            <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <span v-show="!sidebarCollapsed" class="logo-text">Wiki</span>
        </div>
        <button class="collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path :d="sidebarCollapsed ? 'M9 18l6-6-6-6' : 'M15 18l-6-6 6-6'"/>
          </svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" :title="sidebarCollapsed ? '工作台' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
          <span v-show="!sidebarCollapsed">工作台</span>
        </router-link>

        <div class="nav-item" @click="toggleFavorites" :title="sidebarCollapsed ? '收藏' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
          <span v-show="!sidebarCollapsed">收藏</span>
          <svg v-show="!sidebarCollapsed" class="nav-arrow" :class="{ expanded: favoritesExpanded }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </div>
        <div v-show="favoritesExpanded && !sidebarCollapsed" class="nav-submenu">
          <router-link
            v-for="fav in favorites"
            :key="fav.id"
            :to="`/editor/${fav.kbId}/${fav.docId}`"
            class="nav-subitem"
          >
            <svg class="nav-icon-mini" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <path d="M14 2v6h6"/>
            </svg>
            <span class="fav-title">{{ fav.docTitle }}</span>
          </router-link>
          <div v-if="favorites.length === 0" class="nav-subitem empty-state">暂无收藏</div>
        </div>

        <div class="nav-item" @click="toggleKnowledgeBases" :title="sidebarCollapsed ? '知识库' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
          </svg>
          <span v-show="!sidebarCollapsed">知识库</span>
          <button
            v-show="!sidebarCollapsed"
            class="nav-add-btn"
            @click.stop="showCreateKb = !showCreateKb"
            title="新建知识库"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
          </button>
          <svg v-show="!sidebarCollapsed && !showCreateKb" class="nav-arrow" :class="{ expanded: kbExpanded }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </div>
        <div v-show="showCreateKb && !sidebarCollapsed" class="create-kb-form">
          <input v-model="newKb.name" class="input-mini" placeholder="知识库名称" @keyup.enter="createKb" />
          <select v-model="newKb.type" class="input-mini">
            <option value="COMPANY">公司公开</option>
            <option value="DEPARTMENT">部门</option>
            <option value="PRIVATE">私有</option>
          </select>
          <div class="form-actions">
            <button class="btn-mini btn-primary" @click="createKb">创建</button>
            <button class="btn-mini" @click="showCreateKb = false">取消</button>
          </div>
        </div>
        <div v-show="kbExpanded && !sidebarCollapsed" class="nav-submenu">
          <router-link
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :to="`/editor/${kb.id}`"
            class="nav-subitem"
          >
            <div class="kb-avatar-mini" :class="getKbClass(kb.type)">
              {{ kb.name.charAt(0).toUpperCase() }}
            </div>
            <span class="kb-name">{{ kb.name }}</span>
          </router-link>
          <div v-if="knowledgeBases.length === 0" class="nav-subitem empty-state">暂无知识库</div>

          <div class="kb-actions">
            <button class="kb-action-btn" @click="handleNewDoc" title="新建文档">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6"/>
                <path d="M12 11v6M9 14h6"/>
              </svg>
              <span>新建文档</span>
            </button>
            <button class="kb-action-btn" title="导入文件">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
              </svg>
              <span>导入文件</span>
            </button>
          </div>
        </div>

        <router-link to="/profile" class="nav-item" :title="sidebarCollapsed ? '个人中心' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="8" r="4"/>
            <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
          </svg>
          <span v-show="!sidebarCollapsed">个人中心</span>
        </router-link>
        <router-link to="/recycle" class="nav-item" :title="sidebarCollapsed ? '回收站' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14z"/>
          </svg>
          <span v-show="!sidebarCollapsed">回收站</span>
        </router-link>
        <router-link v-if="auth.isAdmin" to="/admin/logs" class="nav-item" :title="sidebarCollapsed ? '管理日志' : ''">
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
          </svg>
          <span v-show="!sidebarCollapsed">管理日志</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-text theme-toggle" @click="toggleTheme" :title="`当前主题: ${theme.mode}`">
          <svg v-if="theme.mode === 'light'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5"/>
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
          </svg>
          <svg v-else-if="theme.mode === 'dark'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="14" rx="2"/>
            <path d="M8 21h8M12 17v4"/>
          </svg>
        </button>
        <button class="btn-text logout-btn" @click="logout" title="退出登录">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>
          </svg>
        </button>
      </div>
    </aside>

    <main :class="['main-content', { full: isPublicRoute, 'sidebar-collapsed': sidebarCollapsed }]">
      <router-view />
    </main>

    <LoadingMask />
    <ErrorDialog />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './store/auth'
import { useThemeStore } from './store/theme'
import { kbApi, favoriteApi } from './api/modules'
import ErrorDialog from './components/ErrorDialog.vue'
import LoadingMask from './components/LoadingMask.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const isPublicRoute = computed(() => route.meta.public)
const sidebarCollapsed = ref(false)
const favoritesExpanded = ref(false)
const kbExpanded = ref(false)
const knowledgeBases = ref([])
const favorites = ref([])
const showCreateKb = ref(false)

const newKb = reactive({
  name: '',
  type: 'COMPANY'
})

onMounted(async () => {
  theme.apply()
  const saved = localStorage.getItem('sidebar-collapsed')
  if (saved !== null) {
    sidebarCollapsed.value = saved === 'true'
  }

  if (!isPublicRoute.value) {
    await loadKnowledgeBases()
    await loadFavorites()
  }
})

async function loadKnowledgeBases() {
  try {
    knowledgeBases.value = await kbApi.mine()
  } catch (error) {
    console.error('Failed to load knowledge bases:', error)
  }
}

async function loadFavorites() {
  try {
    favorites.value = await favoriteApi.mine()
  } catch (error) {
    console.error('Failed to load favorites:', error)
  }
}

async function createKb() {
  if (!newKb.name) {
    return
  }
  await kbApi.create(newKb)
  newKb.name = ''
  newKb.type = 'COMPANY'
  showCreateKb.value = false
  await loadKnowledgeBases()
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed.value))
}

function toggleFavorites() {
  favoritesExpanded.value = !favoritesExpanded.value
}

function toggleKnowledgeBases() {
  if (!showCreateKb.value) {
    kbExpanded.value = !kbExpanded.value
  }
}

function handleNewDoc() {
  router.push('/')
}

function getKbClass(type) {
  const typeMap = {
    'COMPANY': 'kb-company',
    'DEPARTMENT': 'kb-department',
    'PRIVATE': 'kb-private'
  }
  return typeMap[type] || 'kb-company'
}

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
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--brand);
  font-size: 18px;
  font-weight: 600;
  min-width: 0;
  flex: 1;
}

.logo svg {
  flex-shrink: 0;
}

.logo-text {
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  transition: opacity 0.2s ease;
}

.sidebar.collapsed .logo-text {
  opacity: 0;
  width: 0;
}

.collapse-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.collapse-btn svg {
  width: 18px;
  height: 18px;
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
  white-space: nowrap;
  overflow: hidden;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 10px;
}

.sidebar.collapsed .nav-item span {
  display: none;
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
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.nav-icon-mini {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--text-secondary);
}

.fav-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-arrow {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  margin-left: auto;
  transition: transform 0.2s ease;
}

.nav-arrow.expanded {
  transform: rotate(90deg);
}

.nav-submenu {
  margin-left: 28px;
  margin-top: 4px;
  margin-bottom: 8px;
}

.nav-subitem {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 2px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.nav-subitem:hover {
  background: var(--line-light);
  color: var(--text);
}

.nav-subitem.router-link-active {
  background: var(--brand-light);
  color: var(--brand);
}

.nav-subitem.empty-state {
  color: var(--muted);
  font-size: 12px;
  cursor: default;
  padding: 12px;
  text-align: center;
}

.nav-subitem.empty-state:hover {
  background: transparent;
  color: var(--muted);
}

.kb-avatar-mini {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.kb-avatar-mini.kb-company {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.kb-avatar-mini.kb-department {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.kb-avatar-mini.kb-private {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
}

.kb-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-add-btn {
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: auto;
  padding: 0;
}

.nav-add-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.nav-add-btn svg {
  width: 14px;
  height: 14px;
}

.create-kb-form {
  padding: 12px;
  margin: 0 8px 8px 8px;
  background: var(--line-light);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-mini {
  padding: 6px 10px;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-size: 13px;
  background: var(--panel);
  color: var(--text);
  outline: none;
  transition: border-color 0.2s;
}

.input-mini:focus {
  border-color: var(--brand);
}

.form-actions {
  display: flex;
  gap: 6px;
}

.btn-mini {
  padding: 5px 10px;
  font-size: 12px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--panel);
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s;
  flex: 1;
}

.btn-mini:hover {
  background: var(--line-light);
}

.btn-mini.btn-primary {
  background: var(--brand);
  color: white;
  border-color: var(--brand);
}

.btn-mini.btn-primary:hover {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.kb-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--line);
}

.kb-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: transparent;
  border: 1px solid var(--line);
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
}

.kb-action-btn:hover {
  background: var(--line-light);
  border-color: var(--brand);
  color: var(--text);
}

.kb-action-btn svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.theme-toggle svg,
.logout-btn svg {
  width: 18px;
  height: 18px;
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
  transition: margin-left 0.3s ease;
}

.main-content.sidebar-collapsed {
  margin-left: 64px;
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
