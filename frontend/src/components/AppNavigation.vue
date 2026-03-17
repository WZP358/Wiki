<template>
  <div>
    <!-- 顶部导航 -->
    <header class="top-nav">
    <div class="top-nav-left">
      <button class="menu-btn" @click="toggleDrawer" title="菜单">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 12h18M3 6h18M3 18h18"/>
        </svg>
      </button>

      <div class="logo">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="currentColor" opacity="0.3"/>
          <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <span class="logo-text">Wiki</span>
      </div>

      <nav class="primary-nav">
        <a
          v-for="item in primaryNav"
          :key="item.key"
          :class="['nav-link', { active: activeSection === item.key }]"
          @click="switchSection(item.key)"
        >
          <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path :d="item.icon"/>
          </svg>
          <span>{{ item.label }}</span>
        </a>
      </nav>
    </div>

    <div class="top-nav-right">
      <button class="icon-btn" @click="$emit('search')" title="搜索">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <path d="M21 21l-4.35-4.35"/>
        </svg>
      </button>

      <button class="icon-btn" @click="toggleTheme" :title="`当前主题: ${theme.mode}`">
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

      <div class="user-menu">
        <button class="user-avatar" @click="toggleUserMenu">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="8" r="4"/>
            <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
          </svg>
        </button>
        <div v-if="userMenuOpen" class="user-dropdown">
          <router-link to="/profile" class="dropdown-item" @click="userMenuOpen = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="8" r="4"/>
              <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
            </svg>
            <span>个人中心</span>
          </router-link>
          <div v-if="auth.isAdmin" class="dropdown-divider"></div>
          <router-link v-if="auth.isAdmin" to="/admin/logs" class="dropdown-item" @click="userMenuOpen = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
            </svg>
            <span>管理日志</span>
          </router-link>
          <div class="dropdown-divider"></div>
          <button class="dropdown-item" @click="logout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>
            </svg>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </header>

  <!-- 遮罩层 -->
  <div v-if="drawerOpen" class="drawer-overlay" @click="closeDrawer"></div>

  <!-- 左侧抽屉导航 -->
  <aside :class="['side-drawer', { open: drawerOpen }]">
    <!-- 工作台 -->
    <div v-if="activeSection === 'workspace'" class="nav-section">
      <div class="section-header">
        <h3>工作台</h3>
      </div>
      <div class="nav-list">
        <router-link to="/" class="nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
          <span>概览</span>
        </router-link>
        <router-link to="/search" class="nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          <span>搜索</span>
        </router-link>
        <router-link to="/recycle" class="nav-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6h14z"/>
          </svg>
          <span>回收站</span>
        </router-link>
      </div>
    </div>

    <!-- 知识库 -->
    <div v-if="activeSection === 'knowledge'" class="nav-section">
      <div class="section-header">
        <h3>知识库</h3>
        <button class="add-btn" @click="showCreateKb = true" title="新建知识库">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
        </button>
      </div>

      <div v-if="showCreateKb" class="create-form">
        <input v-model="newKb.name" class="form-input" placeholder="知识库名称" @keyup.enter="createKb" />
        <select v-model="newKb.type" class="form-select">
          <option value="COMPANY">公司公开</option>
          <option value="DEPARTMENT">部门</option>
          <option value="PRIVATE">私有</option>
        </select>
        <div class="form-actions">
          <button class="btn btn-primary" @click="createKb">创建</button>
          <button class="btn" @click="showCreateKb = false">取消</button>
        </div>
      </div>

      <div class="nav-list">
        <div
          v-for="kb in knowledgeBases"
          :key="kb.id"
          class="nav-item kb-item"
          @click="goToKb(kb.id)"
        >
          <div class="kb-avatar" :class="getKbClass(kb.type)">
            {{ kb.name.charAt(0).toUpperCase() }}
          </div>
          <span class="kb-name">{{ kb.name }}</span>
          <button class="kb-settings" @click.stop="goToSettings(kb.id)" title="设置">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M12 1v6m0 6v6M1 12h6m6 0h6"/>
            </svg>
          </button>
        </div>
        <div v-if="knowledgeBases.length === 0" class="empty-state">
          暂无知识库，点击右上角创建
        </div>
      </div>
    </div>

    <!-- 收藏 -->
    <div v-if="activeSection === 'favorites'" class="nav-section">
      <div class="section-header">
        <h3>收藏</h3>
      </div>
      <div class="nav-list">
        <div
          v-for="fav in groupedFavorites"
          :key="fav.kbId"
          class="fav-group"
        >
          <div class="fav-kb-header" @click="toggleFavGroup(fav.kbId)">
            <div class="kb-avatar-small kb-company">
              {{ fav.kbName.charAt(0).toUpperCase() }}
            </div>
            <span class="fav-kb-name">{{ fav.kbName }}</span>
            <svg class="expand-icon" :class="{ expanded: expandedFavGroups.has(fav.kbId) }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </div>
          <div v-show="expandedFavGroups.has(fav.kbId)" class="fav-docs">
            <div
              v-for="doc in fav.docs"
              :key="doc.docId"
              class="nav-item fav-doc"
              @click="openDoc(fav.kbId, doc.docId)"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6"/>
              </svg>
              <span>{{ doc.docTitle }}</span>
            </div>
          </div>
        </div>
        <div v-if="favorites.length === 0" class="empty-state">
          暂无收藏
        </div>
      </div>
    </div>
  </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useThemeStore } from '../store/theme'
import { kbApi, favoriteApi } from '../api/modules'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()

const drawerOpen = ref(false)
const activeSection = ref('workspace')
const userMenuOpen = ref(false)
const showCreateKb = ref(false)
const knowledgeBases = ref([])
const favorites = ref([])
const expandedFavGroups = ref(new Set())

const newKb = ref({
  name: '',
  type: 'COMPANY'
})

const primaryNav = [
  {
    key: 'workspace',
    label: '工作台',
    icon: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z'
  },
  {
    key: 'knowledge',
    label: '知识库',
    icon: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'
  },
  {
    key: 'favorites',
    label: '收藏',
    icon: 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'
  }
]

const groupedFavorites = computed(() => {
  const map = new Map()
  for (const fav of favorites.value) {
    if (!fav.kbId || !fav.kbName) continue
    if (!map.has(fav.kbId)) {
      map.set(fav.kbId, {
        kbId: fav.kbId,
        kbName: fav.kbName,
        docs: []
      })
    }
    if (fav.docId && fav.docTitle) {
      map.get(fav.kbId).docs.push({
        docId: fav.docId,
        docTitle: fav.docTitle
      })
    }
  }
  return Array.from(map.values())
})

onMounted(async () => {
  await loadKnowledgeBases()
  await loadFavorites()
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
  if (!newKb.value.name) return
  await kbApi.create(newKb.value)
  newKb.value.name = ''
  newKb.value.type = 'COMPANY'
  showCreateKb.value = false
  await loadKnowledgeBases()
}

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value
}

function closeDrawer() {
  drawerOpen.value = false
}

function switchSection(key) {
  activeSection.value = key
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function toggleFavGroup(kbId) {
  if (expandedFavGroups.value.has(kbId)) {
    expandedFavGroups.value.delete(kbId)
  } else {
    expandedFavGroups.value.add(kbId)
  }
}

function goToKb(kbId) {
  router.push({ path: '/', query: { kbId } })
  closeDrawer()
}

function goToSettings(kbId) {
  router.push({ path: `/settings/${kbId}` })
  closeDrawer()
}

function openDoc(kbId, docId) {
  router.push({ path: `/editor/${kbId}/${docId}`, query: { kbId } })
  closeDrawer()
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
/* 顶部导航 */
.top-nav {
  height: 56px;
  background: var(--panel);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.top-nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.menu-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.menu-btn svg {
  width: 20px;
  height: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--brand);
  font-size: 18px;
  font-weight: 600;
}

.logo-text {
  color: var(--text);
}

.primary-nav {
  display: flex;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}

.nav-link:hover {
  background: var(--line-light);
  color: var(--text);
}

.nav-link.active {
  background: var(--brand-light);
  color: var(--brand);
}

.nav-link .nav-icon {
  width: 18px;
  height: 18px;
}

.top-nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.icon-btn svg {
  width: 20px;
  height: 20px;
}

.user-menu {
  position: relative;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--brand-light);
  color: var(--brand);
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.user-avatar:hover {
  background: var(--brand);
  color: white;
}

.user-avatar svg {
  width: 20px;
  height: 20px;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 200px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 8px;
  z-index: 1001;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  width: 100%;
  border: none;
  background: transparent;
  text-align: left;
}

.dropdown-item:hover {
  background: var(--line-light);
}

.dropdown-item svg {
  width: 18px;
  height: 18px;
  color: var(--text-secondary);
}

.dropdown-divider {
  height: 1px;
  background: var(--line);
  margin: 8px 0;
}

/* 遮罩层 */
.drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1100;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 左侧抽屉导航 */
.side-drawer {
  width: 280px;
  background: var(--panel);
  border-right: 1px solid var(--line);
  position: fixed;
  left: -280px;
  top: 0;
  bottom: 0;
  overflow-y: auto;
  padding: 80px 16px 24px 16px;
  z-index: 1200;
  transition: left 0.3s ease-out;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.side-drawer.open {
  left: 0;
}

.nav-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 8px 12px;
  margin-bottom: 8px;
}

.section-header h3 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.add-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.add-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.add-btn svg {
  width: 16px;
  height: 16px;
}

.create-form {
  padding: 12px;
  background: var(--line-light);
  border-radius: 8px;
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-input,
.form-select {
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  font-size: 14px;
  background: var(--panel);
  color: var(--text);
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-select:focus {
  border-color: var(--brand);
}

.form-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 12px;
  font-size: 13px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s;
  flex: 1;
}

.btn:hover {
  background: var(--line-light);
}

.btn-primary {
  background: var(--brand);
  color: white;
  border-color: var(--brand);
}

.btn-primary:hover {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
}

.nav-item:hover {
  background: var(--line-light);
}

.nav-item.router-link-active {
  background: var(--brand-light);
  color: var(--brand);
}

.nav-item svg {
  width: 18px;
  height: 18px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.nav-item.router-link-active svg {
  color: var(--brand);
}

.kb-item {
  position: relative;
}

.kb-avatar {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.kb-avatar.kb-company {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.kb-avatar.kb-department {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.kb-avatar.kb-private {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
}

.kb-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-settings {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  display: none;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.kb-item:hover .kb-settings {
  display: flex;
}

.kb-settings:hover {
  background: var(--line);
  color: var(--brand);
}

.kb-settings svg {
  width: 16px;
  height: 16px;
}

.fav-group {
  margin-bottom: 8px;
}

.fav-kb-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.fav-kb-header:hover {
  background: var(--line-light);
}

.kb-avatar-small {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.kb-avatar-small.kb-company {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.fav-kb-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--text);
}

.expand-icon {
  width: 16px;
  height: 16px;
  color: var(--text-secondary);
  transition: transform 0.2s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.fav-docs {
  margin-left: 32px;
  margin-top: 4px;
}

.fav-doc {
  font-size: 13px;
  padding: 8px 12px;
}

.empty-state {
  padding: 24px 12px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>
