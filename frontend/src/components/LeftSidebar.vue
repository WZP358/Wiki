<template>
  <aside :class="['left-sidebar', { collapsed }]" @click="handleSidebarClick">
    <button class="brand-row" type="button" @click="$emit('go-dashboard')">
      <span class="logo">W</span>
      <span v-show="!collapsed" class="brand-text">
        <strong>企业知识库</strong>
        <small>内部文档协作平台</small>
      </span>
    </button>

    <div class="side-tools">
      <el-tooltip :content="collapsed ? '展开侧栏' : '收起侧栏'" placement="right">
        <el-button text circle @click.stop="$emit('toggle-collapsed')">
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <el-menu
      class="side-menu"
      background-color="transparent"
      text-color="#c6d0dc"
      active-text-color="#ffffff"
      :collapse="collapsed"
      :collapse-transition="false"
      :default-active="activeKey"
      @select="handleMenuSelect"
    >
      <el-menu-item index="dashboard">
        <el-icon><House /></el-icon>
        <template #title>首页</template>
      </el-menu-item>
      <el-menu-item index="search">
        <el-icon><Search /></el-icon>
        <template #title>全文检索</template>
      </el-menu-item>
    </el-menu>

    <section class="kb-section">
      <div class="section-title">
        <span v-show="!collapsed">知识库</span>
        <div class="section-actions">
          <el-tooltip content="新建知识库" placement="right">
            <el-button v-show="!collapsed" text circle @click.stop="toggleCreate">
              <el-icon><Plus /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="刷新" placement="right">
            <el-button text circle @click.stop="$emit('refresh-kbs')">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <el-card v-if="showCreateKb && !collapsed" class="create-box" shadow="never" @click.stop>
        <el-input v-model="newKb.name" size="small" placeholder="知识库名称" @keyup.enter="submitCreate" />
        <el-select v-model="newKb.type" size="small" style="width: 100%">
          <el-option label="公开知识库" value="COMPANY" />
          <el-option label="团队知识库" value="DEPARTMENT" />
          <el-option label="私有知识库" value="PRIVATE" />
        </el-select>
        <el-select
          v-if="newKb.type === 'DEPARTMENT'"
          v-model="newKb.teamId"
          size="small"
          style="width: 100%"
          placeholder="选择归属团队"
        >
          <el-option
            v-for="team in userTeamOptions"
            :key="team.id"
            :label="team.name"
            :value="team.id"
          />
        </el-select>
        <div class="create-actions">
          <el-button type="primary" size="small" @click="submitCreate">创建</el-button>
          <el-button size="small" @click="cancelCreate">取消</el-button>
        </div>
      </el-card>

      <div v-if="loading" class="empty">正在加载...</div>
      <div v-else-if="kbs.length === 0" class="empty">暂无可用知识库</div>
      <el-scrollbar v-else class="kb-scroll">
        <button
          v-for="kb in kbs"
          :key="kb.id"
          class="kb-row"
          :class="{ active: String(kb.id) === String(activeKbId) }"
          type="button"
          :title="kb.name"
          @click="$emit('select-kb', kb)"
        >
          <span class="kb-avatar" :class="getKbClass(kb.type)">{{ (kb.name || 'K').charAt(0).toUpperCase() }}</span>
          <span v-show="!collapsed" class="kb-name">{{ kb.name }}</span>
        </button>
      </el-scrollbar>
    </section>

    <div class="sidebar-user" @click.stop>
      <el-dropdown trigger="click" placement="top" @command="handleUserCommand">
        <button class="user-btn" type="button">
          <el-avatar :size="collapsed ? 32 : 30">{{ (auth.user?.username || 'U').charAt(0).toUpperCase() }}</el-avatar>
          <span v-show="!collapsed" class="user-meta">
            <strong>{{ auth.user?.nickname || auth.user?.username || '未命名用户' }}</strong>
            <small>{{ userSubtitle }}</small>
          </span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item v-if="auth.isAdmin" command="admin">后台管理</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Expand, Fold, House, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { deptApi } from '../api/modules'

defineProps({
  kbs: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  activeKbId: { type: [String, Number], default: '' },
  activeKey: { type: String, default: 'dashboard' },
  collapsed: { type: Boolean, default: false }
})

const emit = defineEmits([
  'select-kb',
  'refresh-kbs',
  'create-kb',
  'go-dashboard',
  'go-search',
  'toggle-collapsed',
  'request-close-menus'
])

const router = useRouter()
const auth = useAuthStore()
const showCreateKb = ref(false)
const newKb = ref({ name: '', type: 'COMPANY', teamId: null })
const teams = ref([])

const userTeamOptions = computed(() => {
  const selected = new Map()
  const teamMap = new Map(teams.value.map(team => [String(team.id), team]))

  for (const teamId of assignedTeamIds.value) {
    let current = teamMap.get(String(teamId))
    let guard = 0
    while (current && guard < 32) {
      selected.set(String(current.id), current)
      current = current.parentId ? teamMap.get(String(current.parentId)) : null
      guard++
    }
  }

  if (selected.size === 0) {
    return fallbackTeamOptions.value
  }

  return Array.from(selected.values()).sort((a, b) => teamDepth(a, teamMap) - teamDepth(b, teamMap))
})

const assignedTeamIds = computed(() => {
  const ids = Array.isArray(auth.user?.teamIds) ? auth.user.teamIds : []
  if (ids.length > 0) return ids.filter(id => id !== null && id !== undefined)
  return auth.user?.departmentId ? [auth.user.departmentId] : []
})

const fallbackTeamOptions = computed(() => {
  const ids = Array.isArray(auth.user?.teamIds) ? auth.user.teamIds : []
  const names = Array.isArray(auth.user?.teamNames) ? auth.user.teamNames : []
  const options = ids
    .filter(id => id !== null && id !== undefined)
    .map((id, index) => ({ id, name: names[index] || `团队 ${id}` }))

  if (options.length === 0 && auth.user?.departmentId) {
    options.push({ id: auth.user.departmentId, name: auth.user.departmentName || `团队 ${auth.user.departmentId}` })
  }
  return options
})

const userSubtitle = computed(() => {
  if (auth.user?.departmentName || auth.user?.positionName) {
    return [auth.user.departmentName, auth.user.positionName].filter(Boolean).join(' / ')
  }
  return auth.user?.role === 'ADMIN' ? '系统管理员' : '待分配用户'
})

onMounted(loadTeams)

async function loadTeams() {
  try {
    const data = await deptApi.list()
    teams.value = Array.isArray(data) ? data : []
  } catch {
    teams.value = []
  }
}

function teamDepth(team, teamMap) {
  let depth = 0
  let current = team
  while (current?.parentId && depth < 32) {
    current = teamMap.get(String(current.parentId))
    depth++
  }
  return depth
}

function handleSidebarClick() {
  emit('request-close-menus')
}

function handleMenuSelect(index) {
  if (index === 'dashboard') emit('go-dashboard')
  if (index === 'search') emit('go-search')
}

function toggleCreate() {
  showCreateKb.value = !showCreateKb.value
}

function cancelCreate() {
  showCreateKb.value = false
  newKb.value = { name: '', type: 'COMPANY', teamId: null }
}

function submitCreate() {
  const name = String(newKb.value.name || '').trim()
  if (!name) return
  const teamId = newKb.value.type === 'DEPARTMENT'
    ? (newKb.value.teamId || userTeamOptions.value[0]?.id || null)
    : null
  emit('create-kb', { name, type: newKb.value.type, teamId })
  cancelCreate()
}

function handleUserCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'admin') {
    window.location.href = '/admin/'
  } else if (command === 'logout') {
    auth.logout()
    router.push('/auth')
  }
}

function getKbClass(type) {
  return {
    COMPANY: 'company',
    DEPARTMENT: 'department',
    PRIVATE: 'private'
  }[type] || 'company'
}
</script>

<style scoped>
.left-sidebar { width: var(--sidebar-w, 240px); min-width: var(--sidebar-w, 240px); height: 100vh; position: sticky; top: 0; display: flex; flex-direction: column; overflow: hidden; background: #2f4056; color: #c6d0dc; border-right: 1px solid #253447; }
.left-sidebar.collapsed { width: 56px; min-width: 56px; }
.brand-row { width: 100%; height: 58px; border: none; display: flex; align-items: center; gap: 10px; padding: 0 14px; cursor: pointer; color: inherit; background: #243245; text-align: left; }
.collapsed .brand-row { justify-content: center; padding: 0; }
.logo { width: 32px; height: 32px; border-radius: 6px; display: grid; place-items: center; flex-shrink: 0; color: #fff; background: #409eff; font-size: 18px; font-weight: 800; }
.brand-text { min-width: 0; }
.brand-text strong, .brand-text small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.brand-text strong { color: #fff; font-size: 15px; }
.brand-text small { margin-top: 2px; color: #8ea3bc; font-size: 12px; }
.side-tools { display: flex; justify-content: flex-end; padding: 8px 10px; }
.collapsed .side-tools { justify-content: center; padding: 8px 0; }
.side-tools :deep(.el-button), .section-actions :deep(.el-button) { width: 32px; height: 32px; color: #c6d0dc; }
.side-tools :deep(.el-button:hover), .section-actions :deep(.el-button:hover) { color: #fff; background: rgba(255, 255, 255, 0.08); }
.side-menu { width: 100%; border-right: none; background: transparent; }
.side-menu.el-menu--collapse { width: 56px; }
.side-menu :deep(.el-menu-item) { height: 46px; line-height: 46px; margin: 0; border-radius: 0; }
.side-menu :deep(.el-menu-item .el-icon) { margin-right: 10px; font-size: 18px; }
.collapsed .side-menu :deep(.el-menu-item) { width: 56px; padding: 0 !important; display: flex; justify-content: center; }
.collapsed .side-menu :deep(.el-menu-item .el-icon) { margin: 0; }
.side-menu :deep(.el-menu-item:hover), .side-menu :deep(.el-menu-item.is-active) { background: #26384f; }
.side-menu :deep(.el-menu-item.is-active) { box-shadow: inset 3px 0 0 #409eff; }
.kb-section { flex: 1; min-height: 0; display: flex; flex-direction: column; border-top: 1px solid #253447; }
.section-title { min-height: 42px; padding: 0 10px 0 16px; display: flex; align-items: center; justify-content: space-between; color: #91a6be; font-size: 12px; font-weight: 700; }
.collapsed .section-title { justify-content: center; padding: 0; }
.section-actions { display: flex; gap: 2px; }
.create-box { margin: 0 10px 10px; background: #26384f; border-color: #32465f; }
.create-box :deep(.el-card__body) { display: grid; gap: 8px; padding: 10px; }
.create-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.kb-scroll { flex: 1; }
.kb-scroll :deep(.el-scrollbar__view) { padding: 4px 0 8px; }
.kb-row { width: 100%; height: 42px; border: none; display: flex; align-items: center; gap: 10px; padding: 0 14px; color: #c6d0dc; background: transparent; cursor: pointer; text-align: left; }
.collapsed .kb-row { width: 56px; justify-content: center; padding: 0; }
.kb-row:hover, .kb-row.active { color: #fff; background: #26384f; }
.kb-row.active { box-shadow: inset 3px 0 0 #409eff; }
.kb-avatar { width: 30px; height: 30px; border-radius: 6px; display: grid; place-items: center; flex-shrink: 0; color: #fff; font-size: 13px; font-weight: 700; }
.collapsed .kb-avatar { width: 32px; height: 32px; }
.kb-avatar.company { background: #409eff; }
.kb-avatar.department { background: #67c23a; }
.kb-avatar.private { background: #e6a23c; }
.kb-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty { padding: 18px 12px; color: #91a6be; text-align: center; font-size: 13px; }
.sidebar-user { padding: 10px; border-top: 1px solid #253447; }
.collapsed .sidebar-user { padding: 10px 0; display: flex; justify-content: center; }
.user-btn { width: 100%; min-height: 44px; border: none; border-radius: 6px; display: flex; align-items: center; gap: 10px; padding: 7px 8px; color: #c6d0dc; background: transparent; cursor: pointer; }
.collapsed .user-btn { width: 44px; justify-content: center; padding: 0; }
.user-btn:hover { background: #26384f; color: #fff; }
.user-meta { min-width: 0; text-align: left; }
.user-meta strong, .user-meta small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-meta small { color: #91a6be; }
</style>
