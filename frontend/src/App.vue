<template>
  <div class="app-shell">
    <div
      v-if="!isPublicRoute && !isAdminLayout"
      class="app-frame"
      :style="{ '--sidebar-w': leftSidebarCollapsed ? '56px' : '240px' }"
    >
      <LeftSidebar
        :kbs="kbs"
        :loading="kbsLoading"
        :active-kb-id="currentKbId"
        :active-key="activePrimaryKey"
        :collapsed="leftSidebarCollapsed"
        @select-kb="handleSelectKb"
        @create-kb="handleCreateKb"
        @refresh-kbs="loadKbs"
        @go-dashboard="goDashboard"
        @go-search="goToSearch"
        @toggle-collapsed="toggleLeftSidebar"
        @request-close-menus="closeSignal++"
      />

      <KbDrawer
        :visible="drawerVisible"
        :kb-id="currentKbId"
        :active-doc-id="currentDocId"
        :close-signal="closeSignal"
        :refresh-signal="drawerRefreshSignal"
        @close="drawerVisible = false"
        @open-doc="openDocFromDrawer"
        @create-doc="goCreateDoc"
        @go-home="goKbHome"
        @rename-doc="renameDocFromDrawer"
        @delete-doc="deleteDocFromDrawer"
        @request-close-menus="closeSignal++"
      />

      <main class="main-content" @click="drawerVisible = false">
        <router-view />
      </main>
    </div>

    <main v-else class="main-content full">
      <router-view />
    </main>

    <LoadingMask />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadingMask from './components/LoadingMask.vue'
import LeftSidebar from './components/LeftSidebar.vue'
import KbDrawer from './components/KbDrawer.vue'
import { authApi, docApi, kbApi } from './api/modules'
import { useAuthStore } from './store/auth'
import { confirmDialog, promptDialog, showToast } from './utils/errorBus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isPublicRoute = computed(() => route.meta.public)
const isAdminLayout = computed(() => route.meta.layout === 'admin')

const kbs = ref([])
const kbsLoading = ref(false)
const drawerVisible = ref(false)
const closeSignal = ref(0)
const drawerRefreshSignal = ref(0)
const leftSidebarCollapsed = ref(false)

const LEFT_SIDEBAR_COLLAPSED_KEY = 'wiki.leftSidebarCollapsed'

const currentKbId = computed(() => String(route.params.kbId || route.query.kbId || ''))
const currentDocId = computed(() => String(route.params.docId || ''))

const activePrimaryKey = computed(() => {
  const path = route.path || '/'
  if (path.startsWith('/search')) return 'search'
  return 'dashboard'
})

const strongKbContext = computed(() => {
  const path = route.path || '/'
  return path.startsWith('/editor/') || path.startsWith('/kb/')
})

onMounted(() => {
  if (shouldLoadPrivateShell()) {
    leftSidebarCollapsed.value = readLeftSidebarCollapsed()
    refreshCurrentUser()
    loadKbs()
  }
})

watch(
  () => [isPublicRoute.value, isAdminLayout.value],
  () => {
    if (shouldLoadPrivateShell()) {
      leftSidebarCollapsed.value = readLeftSidebarCollapsed()
      refreshCurrentUser()
      loadKbs()
    }
  }
)

watch(
  () => [currentKbId.value, strongKbContext.value],
  ([kbId, strong]) => {
    if (!strong) {
      drawerVisible.value = false
      return
    }
    drawerVisible.value = Boolean(kbId)
  },
  { immediate: true }
)

async function loadKbs() {
  if (!localStorage.getItem('wiki-token')) {
    kbs.value = []
    return
  }
  kbsLoading.value = true
  try {
    kbs.value = await kbApi.mine()
  } finally {
    kbsLoading.value = false
  }
}

async function refreshCurrentUser() {
  try {
    const user = await authApi.me()
    auth.user = user
    localStorage.setItem('wiki-user', JSON.stringify(user))
  } catch {
    // Token handling remains in the HTTP interceptor.
  }
}

function shouldLoadPrivateShell() {
  return !isPublicRoute.value && !isAdminLayout.value && Boolean(localStorage.getItem('wiki-token'))
}

async function handleCreateKb(payload) {
  const name = String(payload?.name || '').trim()
  const type = payload?.type || 'COMPANY'
  if (!name) return
  const created = await kbApi.create({
    name,
    type,
    teamId: type === 'DEPARTMENT'
      ? (payload?.teamId || auth.user?.teamIds?.[0] || auth.user?.departmentId || null)
      : null
  })
  await loadKbs()
  if (created?.id) {
    router.push(`/kb/${created.id}`)
  }
}

function goToSearch() {
  drawerVisible.value = false
  router.push(currentKbId.value ? { path: '/search', query: { kbId: currentKbId.value } } : '/search')
}

function goDashboard() {
  drawerVisible.value = false
  router.push('/')
}

function handleSelectKb(kb) {
  const kbId = kb?.id
  if (!kbId) return
  drawerVisible.value = true
  if (route.path.startsWith('/editor') || route.path.startsWith('/kb/')) {
    router.push(`/kb/${kbId}`)
    return
  }
  router.push({ path: route.path, query: { ...route.query, kbId } })
}

function openDocFromDrawer(node) {
  if (!node?.id || !currentKbId.value) return
  router.push(`/editor/${currentKbId.value}/${node.id}`)
}

function goKbHome() {
  if (!currentKbId.value) return
  router.push(`/kb/${currentKbId.value}`)
}

function goCreateDoc() {
  if (!currentKbId.value) {
    showToast({ title: '请选择知识库', message: '先从左侧选择一个知识库，再创建文档。', type: 'warning' })
    return
  }
  router.push(`/editor/${currentKbId.value}`)
}

async function renameDocFromDrawer(node) {
  if (!node?.id) return
  const value = await promptDialog({
    title: '重命名文档',
    message: '请输入新的文档名称。',
    inputValue: node.title || '',
    inputPlaceholder: '文档名称',
    confirmText: '保存'
  })
  const title = String(value || '').trim()
  if (!title || title === node.title) return
  await docApi.update(node.id, {
    title,
    commitMessage: '重命名文档'
  })
  drawerRefreshSignal.value++
  showToast({ title: '已保存', message: '文档名称已更新。', type: 'success' })
}

async function deleteDocFromDrawer(node) {
  if (!node?.id) return
  const ok = await confirmDialog({
    title: '删除文档',
    message: `确定删除“${node.title || '未命名文档'}”吗？删除后仅系统管理员可以在后台恢复。`,
    tone: 'danger',
    confirmText: '删除',
    cancelText: '取消'
  })
  if (!ok) return
  await docApi.delete(node.id)
  drawerRefreshSignal.value++
  showToast({ title: '已删除', message: '文档已删除。', type: 'success' })
  if (String(node.id) === String(currentDocId.value)) {
    goKbHome()
  }
}

function readLeftSidebarCollapsed() {
  try {
    return localStorage.getItem(LEFT_SIDEBAR_COLLAPSED_KEY) === '1'
  } catch {
    return false
  }
}

function toggleLeftSidebar() {
  leftSidebarCollapsed.value = !leftSidebarCollapsed.value
  try {
    localStorage.setItem(LEFT_SIDEBAR_COLLAPSED_KEY, leftSidebarCollapsed.value ? '1' : '0')
  } catch {
    // 浏览器禁用存储时不影响主流程。
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-frame {
  display: flex;
  min-height: 100vh;
  --sidebar-w: 240px;
}

.main-content {
  min-width: 0;
  padding: 24px;
  flex: 1;
}

.main-content.full {
  margin-left: 0;
  margin-top: 0;
}

@media (max-width: 900px) {
  .main-content {
    padding: 16px;
  }
}
</style>
