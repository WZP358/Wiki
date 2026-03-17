<template>
  <div class="app-shell">
    <div v-if="!isPublicRoute" class="app-frame">
      <LeftSidebar
        :kbs="kbs"
        :loading="kbsLoading"
        :active-kb-id="currentKbId"
        :active-key="activePrimaryKey"
        @select-kb="handleSelectKb"
        @refresh-kbs="loadKbs"
        @go-dashboard="goDashboard"
        @go-search="goToSearch"
        @go-favorites="goFavorites"
        @go-recycle="goRecycle"
        @request-close-menus="closeSignal++"
      />

      <KbDrawer
        :visible="drawerVisible"
        :kb-id="currentKbId"
        :active-doc-id="currentDocId"
        :close-signal="closeSignal"
        @close="drawerVisible = false"
        @open-doc="openDocFromDrawer"
        @create-doc="goCreateDoc"
        @go-home="goKbHome"
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
    <ErrorDialog />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from './store/theme'
import ErrorDialog from './components/ErrorDialog.vue'
import LoadingMask from './components/LoadingMask.vue'
import LeftSidebar from './components/LeftSidebar.vue'
import KbDrawer from './components/KbDrawer.vue'
import { kbApi } from './api/modules'

const route = useRoute()
const router = useRouter()
const theme = useThemeStore()

const isPublicRoute = computed(() => route.meta.public)

theme.apply()

const kbs = ref([])
const kbsLoading = ref(false)
const drawerVisible = ref(false)
const closeSignal = ref(0)

const currentKbId = computed(() => {
  return String(route.params.kbId || route.query.kbId || '')
})

const currentDocId = computed(() => String(route.params.docId || ''))

const activePrimaryKey = computed(() => {
  const p = route.path || '/'
  if (p.startsWith('/search')) return 'search'
  if (p.startsWith('/favorites')) return 'favorites'
  if (p.startsWith('/recycle')) return 'recycle'
  return 'dashboard'
})

const strongKbContext = computed(() => {
  const p = route.path || '/'
  return p.startsWith('/editor/') || p.startsWith('/kb/')
})

onMounted(() => {
  if (!isPublicRoute.value) {
    loadKbs()
  }
})

watch(
  () => isPublicRoute.value,
  (v) => {
    if (!v) loadKbs()
  }
)

watch(
  () => [currentKbId.value, strongKbContext.value],
  ([kbId, strong]) => {
    // 规则：一级页面（概览/搜索/回收站等）强制关闭二级导航；
    // 仅在 editor / kb 页面，且存在 kbId 时自动打开/切换。
    if (!strong) {
      drawerVisible.value = false
      return
    }
    if (kbId) {
      drawerVisible.value = true
    }
  },
  { immediate: true }
)

async function loadKbs() {
  kbsLoading.value = true
  try {
    kbs.value = await kbApi.mine()
  } finally {
    kbsLoading.value = false
  }
}

function goToSearch() {
  drawerVisible.value = false
  router.push('/search')
}

function goRecycle() {
  drawerVisible.value = false
  router.push('/recycle')
}

function goFavorites() {
  drawerVisible.value = false
  router.push('/favorites')
}

function goDashboard() {
  drawerVisible.value = false
  router.push('/')
}

function handleSelectKb(kb) {
  // 保持当前页面不丢失，同时用 query 记住选择（工作台/搜索页也能复用）
  const kbId = kb?.id
  if (!kbId) return
  drawerVisible.value = true
  router.push({ path: route.path, params: route.params, query: { ...route.query, kbId } })
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
  if (!currentKbId.value) return
  router.push(`/editor/${currentKbId.value}`)
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-frame {
  display: flex;
  min-height: 100vh;
  /* 给 KbDrawer 用的定位基准 */
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
  .main-content { padding: 16px; }
}
</style>
