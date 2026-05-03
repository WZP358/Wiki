<template>
  <main class="app-container kb-home">
    <el-skeleton v-if="loading" :rows="8" animated />

    <el-card v-else-if="kb" shadow="never" class="kb-card">
      <template #header>
        <div class="kb-header">
          <div class="kb-heading">
            <el-avatar shape="square" :class="getKbClass(kb.type)">{{ kb.name.slice(0, 1).toUpperCase() }}</el-avatar>
            <div class="kb-title">
              <h1>{{ kb.name }}</h1>
              <div class="meta-line">
                <el-tag size="small" effect="plain">{{ typeText }}</el-tag>
                <el-tag size="small" :type="roleTagType(kb.myRole)" effect="plain">{{ myRoleText }}</el-tag>
                <span>{{ kb.description || '暂无描述' }}</span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button @click="router.push('/')">返回首页</el-button>
            <el-button type="primary" :disabled="!canEdit" @click="createDoc">新建文档</el-button>
            <el-button v-if="canManage" @click="createChildKb">新建子知识库</el-button>
            <el-button v-if="canManage" @click="router.push(`/settings/${kbId}`)">成员与设置</el-button>
            <el-button v-if="canManage" @click="renameKb">重命名</el-button>
            <el-button v-if="canManage" type="danger" plain @click="deleteKb">停用</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="!canEdit"
        class="readonly-alert"
        title="你当前对该知识库只有查看权限。可见只代表能阅读；创建和编辑必须由知识库管理员授予编辑或管理角色。"
        type="info"
        :closable="false"
        show-icon
      />

      <el-card shadow="never" class="inner-card">
        <template #header>
          <div class="ry-card-header">
            <span>子知识库</span>
            <span class="ry-muted">用于表达项目下的前端、后端、任务组等独立协作空间，点击后直接进入对应知识库。</span>
          </div>
        </template>
        <el-empty v-if="childKbs.length === 0" description="暂无子知识库" :image-size="80" />
        <div v-else class="child-grid">
          <button v-for="child in childKbs" :key="child.id" type="button" class="child-kb" @click="router.push(`/kb/${child.id}`)">
            <span class="kb-mini" :class="getKbClass(child.type)">{{ (child.name || 'K').slice(0, 1).toUpperCase() }}</span>
            <span>
              <strong>{{ child.name }}</strong>
              <small>{{ child.description || kbTypeLabel(child.type) }}</small>
            </span>
            <el-tag size="small" :type="roleTagType(child.myRole)" effect="plain">{{ roleLabel(child.myRole) }}</el-tag>
          </button>
        </div>
      </el-card>

      <el-row :gutter="16">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="inner-card">
            <template #header><span>最近更新</span></template>
            <el-empty v-if="latestDocs.length === 0" description="暂无最近更新文档" :image-size="80" />
            <el-table v-else :data="latestDocs.slice(0, 5)" size="small" @row-click="row => openDoc(row.id)">
              <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="versionNo" label="版本" width="80">
                <template #default="{ row }">v{{ row.versionNo || 1 }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="12">
          <el-card shadow="never" class="inner-card">
            <template #header><span>热门文档</span></template>
            <el-empty v-if="hotDocs.length === 0" description="暂无热门文档" :image-size="80" />
            <el-table v-else :data="hotDocs.slice(0, 5)" size="small" @row-click="row => openDoc(row.id)">
              <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="viewCount" label="阅读" width="90">
                <template #default="{ row }">{{ row.viewCount || 0 }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="inner-card members-card">
        <template #header>
          <div class="ry-card-header">
            <span>协作成员</span>
            <span class="ry-muted">只展示已加入协作名单的普通用户。</span>
          </div>
        </template>
        <el-empty v-if="members.length === 0" description="暂无协作成员，请知识库管理员邀请普通用户。" :image-size="80" />
        <el-table v-else :data="members" size="small">
          <el-table-column label="姓名" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="member-cell">
                <el-avatar :size="30" :src="row.avatarUrl">{{ memberInitial(row) }}</el-avatar>
                <div>
                  <strong>{{ row.displayName || row.nickname || row.username || row.userId }}</strong>
                  <span>{{ row.username || '-' }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="departmentName" label="团队" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.departmentName || '未分配团队' }}</template>
          </el-table-column>
          <el-table-column prop="positionName" label="职位" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.positionName || '普通成员' }}</template>
          </el-table-column>
          <el-table-column label="权限" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="roleTagType(row.role)" effect="plain">{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-card>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { kbApi, docApi } from '../api/modules'
import { showToast } from '../utils/errorBus'

const route = useRoute()
const router = useRouter()
const kbId = computed(() => route.params.kbId)

const loading = ref(false)
const kb = ref(null)
const childKbs = ref([])
const latestDocs = ref([])
const hotDocs = ref([])
const members = ref([])

const canManage = computed(() => kb.value?.myRole === 'ADMIN')
const canEdit = computed(() => ['ADMIN', 'EDITOR'].includes(kb.value?.myRole))
const typeText = computed(() => kbTypeLabel(kb.value?.type))
const myRoleText = computed(() => roleLabel(kb.value?.myRole || 'READER'))

onMounted(load)
watch(kbId, load)

async function load() {
  if (!kbId.value) return
  loading.value = true
  try {
    kb.value = await kbApi.get(kbId.value)
    const [children, latest, hot, memberList] = await Promise.all([
      kbApi.children(kbId.value),
      docApi.latest(kbId.value),
      docApi.hot(kbId.value),
      kbApi.members(kbId.value)
    ])
    childKbs.value = Array.isArray(children) ? children : []
    latestDocs.value = Array.isArray(latest) ? latest : []
    hotDocs.value = Array.isArray(hot) ? hot : []
    members.value = Array.isArray(memberList) ? memberList : []
  } finally {
    loading.value = false
  }
}

function getKbClass(type) {
  return { COMPANY: 'company', DEPARTMENT: 'department', PRIVATE: 'private' }[type] || 'company'
}

function kbTypeLabel(type) {
  return {
    COMPANY: '公开知识库',
    DEPARTMENT: '团队知识库',
    PRIVATE: '私有知识库'
  }[type] || '知识库'
}

function roleLabel(role) {
  return {
    READER: '查看',
    VIEWER: '查看',
    EDITOR: '编辑',
    ADMIN: '管理'
  }[role] || role || '查看'
}

function roleTagType(role) {
  return {
    ADMIN: 'warning',
    EDITOR: 'success',
    READER: 'info',
    VIEWER: 'info'
  }[role] || 'info'
}

function memberInitial(member) {
  return String(member.displayName || member.nickname || member.username || '?').slice(0, 1)
}

function openDoc(docId) {
  router.push(`/editor/${kbId.value}/${docId}`)
}

function createDoc() {
  if (!canEdit.value) {
    showToast({ title: '只读权限', message: '你需要加入协作名单并拥有编辑或管理权限后才能新建文档。', type: 'warning' })
    return
  }
  router.push(`/editor/${kbId.value}`)
}

async function createChildKb() {
  if (!kb.value) return
  const { value } = await ElMessageBox.prompt('请输入子知识库名称。', '新建子知识库', {
    confirmButtonText: '创建',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '知识库名称不能为空'
  }).catch(() => ({}))
  if (!value) return
  const child = await kbApi.create({
    name: value,
    type: kb.value.type,
    description: `隶属于 ${kb.value.name}`,
    parentId: kb.value.id,
    teamId: kb.value.type === 'DEPARTMENT' ? kb.value.teamId : null
  })
  showToast({ title: '已创建', message: '子知识库已创建，可独立邀请成员和维护权限。', type: 'success' })
  router.push(`/kb/${child.id}`)
}

async function renameKb() {
  if (!kb.value) return
  const { value } = await ElMessageBox.prompt('请输入新的知识库名称。', '重命名知识库', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: kb.value.name,
    inputPattern: /\S+/,
    inputErrorMessage: '知识库名称不能为空'
  }).catch(() => ({}))
  if (!value) return
  kb.value = await kbApi.update(kbId.value, {
    name: value,
    type: kb.value.type,
    description: kb.value.description,
    parentId: kb.value.parentId
  })
  showToast({ title: '已保存', message: '知识库名称已更新。', type: 'success' })
}

async function deleteKb() {
  if (!kb.value) return
  await ElMessageBox.confirm('停用后知识库不会出现在普通列表中，系统管理员仍可在后台查看和恢复。', '停用知识库', {
    confirmButtonText: '停用',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => Promise.reject(new Error('cancel')))
  await kbApi.remove(kbId.value)
  showToast({ title: '已停用', message: '知识库已停用。', type: 'success' })
  router.push('/')
}
</script>

<style scoped>
.kb-home { max-width: 1280px; margin: 0 auto; }
.kb-card { min-height: 420px; }
.readonly-alert { margin-bottom: 16px; }
.kb-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.kb-heading { min-width: 0; display: flex; align-items: center; gap: 12px; }
.kb-title { min-width: 0; }
.kb-title h1 { margin-bottom: 6px; color: #1f2d3d; font-size: 21px; }
.meta-line { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; color: #606266; }
.header-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.el-avatar.company, .kb-mini.company { background: #409eff; }
.el-avatar.department, .kb-mini.department { background: #67c23a; }
.el-avatar.private, .kb-mini.private { background: #e6a23c; }
.inner-card { margin-top: 16px; }
.child-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 10px; }
.child-kb { min-height: 74px; border: 1px solid #ebeef5; border-radius: 6px; display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 12px; background: #fff; text-align: left; cursor: pointer; }
.child-kb:hover { border-color: #409eff; background: #f5faff; }
.kb-mini { width: 38px; height: 38px; border-radius: 6px; display: grid; place-items: center; color: #fff; font-weight: 700; }
.child-kb span:nth-child(2) { min-width: 0; display: grid; gap: 3px; }
.child-kb strong, .child-kb small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.child-kb small { color: #909399; }
.member-cell { display: flex; align-items: center; gap: 10px; }
.member-cell div { min-width: 0; display: grid; gap: 2px; }
.member-cell strong, .member-cell span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.member-cell span { color: #909399; font-size: 12px; }
:deep(.el-table__row) { cursor: pointer; }
@media (max-width: 900px) { .kb-header { align-items: flex-start; flex-direction: column; } }
</style>
