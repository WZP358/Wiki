<template>
  <main class="app-container user-dashboard">
    <el-card shadow="never" class="hero-card">
      <el-row :gutter="22" align="middle">
        <el-col :xs="24" :lg="15">
          <div class="hero-copy">
            <p class="eyebrow">企业内部知识库 Wiki 系统</p>
            <h1>把团队经验沉淀成可检索、可追溯、可协作的内部文档。</h1>
            <p class="intro-text">
              新用户注册后先进入待分配状态。系统管理员分配团队、职位和基础可见范围；知识库管理员再邀请成员并授予查看、编辑或管理权限。
              公开或团队可见只代表能阅读，编辑必须来自知识库成员角色。
            </p>
            <div class="intro-actions">
              <el-button type="primary" @click="openFirstKb">进入知识库</el-button>
              <el-button @click="goSearch">全文检索</el-button>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :lg="9">
          <div class="stat-grid">
            <el-statistic title="可见知识库" :value="kbs.length" />
            <el-statistic title="最近更新" :value="latest.length" />
            <el-statistic title="热门文档" :value="hot.length" />
          </div>
          <el-divider />
          <div v-if="activeKb" class="current-kb">
            <span class="ry-muted">当前知识库</span>
            <strong>{{ activeKb.name }}</strong>
            <div>
              <el-tag size="small" effect="plain">{{ kbTypeText(activeKb.type) }}</el-tag>
              <el-tag size="small" :type="roleTagType(activeKb.myRole)" effect="plain">{{ roleText(activeKb.myRole) }}</el-tag>
            </div>
          </div>
          <el-alert
            v-else
            title="暂无可用知识库，请等待管理员分配团队或知识库权限。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="workflow-row">
      <el-col v-for="item in workflows" :key="item.title" :xs="24" :md="8">
        <el-card shadow="never" class="workflow-card">
          <div class="workflow-index">{{ item.index }}</div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.text }}</p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="ry-card-header">
              <span>我的知识库</span>
              <el-button link type="primary" @click="refresh">刷新</el-button>
            </div>
          </template>
          <el-empty v-if="kbs.length === 0" description="暂无可见知识库，请联系管理员分配团队或知识库权限。" />
          <div v-else class="kb-grid">
            <button
              v-for="kb in kbs"
              :key="kb.id"
              type="button"
              :class="['kb-card-btn', { active: String(kb.id) === String(activeKbId) }]"
              @click="pickKb(kb.id)"
            >
              <span class="kb-mark" :class="kb.type">{{ (kb.name || 'K').slice(0, 1).toUpperCase() }}</span>
              <span class="kb-info">
                <strong>{{ kb.name }}</strong>
                <small>{{ kb.description || '暂无描述' }}</small>
              </span>
              <el-tag size="small" :type="roleTagType(kb.myRole)" effect="plain">{{ roleText(kb.myRole) }}</el-tag>
            </button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="section-card">
          <template #header><span>业务规则</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item v-for="item in features" :key="item.label" :label="item.label">
              {{ item.text }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="doc-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="ry-card-header">
              <span>最近更新</span>
              <el-button link type="primary" @click="createDoc">新建文档</el-button>
            </div>
          </template>
          <el-empty v-if="latest.length === 0" description="当前知识库暂无最近更新文档。" :image-size="88" />
          <el-table v-else :data="latest.slice(0, 5)" size="small" @row-click="row => openDoc(row.id)">
            <el-table-column prop="title" label="文档标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="versionNo" label="版本" width="90">
              <template #default="{ row }">v{{ row.versionNo || 1 }}</template>
            </el-table-column>
            <el-table-column label="更新时间" width="120">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="section-card">
          <template #header><span>最多阅读</span></template>
          <el-empty v-if="hot.length === 0" description="当前知识库暂无热门文档。" :image-size="88" />
          <el-table v-else :data="hot.slice(0, 5)" size="small" @row-click="row => openDoc(row.id)">
            <el-table-column prop="title" label="文档标题" min-width="220" show-overflow-tooltip />
            <el-table-column label="阅读" width="90">
              <template #default="{ row }">{{ row.viewCount || 0 }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { kbApi, docApi } from '../api/modules'
import { showToast } from '../utils/errorBus'

const route = useRoute()
const router = useRouter()
const kbs = ref([])
const activeKbId = ref('')
const latest = ref([])
const hot = ref([])

const activeKb = computed(() => kbs.value.find(kb => String(kb.id) === String(activeKbId.value)))
const canEditActiveKb = computed(() => ['ADMIN', 'EDITOR'].includes(activeKb.value?.myRole))

const workflows = [
  { index: '01', title: '注册后待分配', text: '新用户默认没有业务权限，系统管理员先分配团队、职位和基础可见范围。' },
  { index: '02', title: '知识库管理员邀请', text: '知识库管理员维护协作名单，并授予查看、编辑或管理权限。' },
  { index: '03', title: '成员按权限协作', text: '公开或团队可见只提供阅读入口，编辑和管理必须依赖成员角色。' }
]

const features = [
  { label: '查看', text: '可阅读公开、同团队或被邀请可见的内容，不能创建或修改。' },
  { label: '编辑', text: '可创建、编辑、保存草稿、插入图片，并触发版本历史。' },
  { label: '管理', text: '可邀请成员、调整权限、创建子知识库、停用知识库和删除文档。' },
  { label: '底线', text: '每个知识库至少保留一名管理员，避免空间无人维护。' }
]

onMounted(refresh)
watch(() => route.query.kbId, async kbId => {
  if (kbId && String(kbId) !== String(activeKbId.value)) await pickKb(kbId)
})

async function refresh() {
  await loadKbs()
  const kbFromQuery = route.query.kbId
  if (kbFromQuery) await pickKb(kbFromQuery)
  else if (kbs.value[0]) await pickKb(kbs.value[0].id)
  else {
    activeKbId.value = ''
    latest.value = []
    hot.value = []
  }
}

async function loadKbs() {
  try {
    const result = await kbApi.mine()
    kbs.value = Array.isArray(result) ? result : []
  } catch {
    kbs.value = []
  }
}

async function pickKb(kbId) {
  activeKbId.value = kbId
  router.replace({ path: '/', query: { ...route.query, kbId } })
  try {
    const [latestDocs, hotDocs] = await Promise.all([docApi.latest(kbId), docApi.hot(kbId)])
    latest.value = Array.isArray(latestDocs) ? latestDocs : []
    hot.value = Array.isArray(hotDocs) ? hotDocs : []
  } catch {
    latest.value = []
    hot.value = []
  }
}

function openFirstKb() {
  if (!activeKbId.value && kbs.value[0]) {
    pickKb(kbs.value[0].id)
    return
  }
  if (!activeKbId.value) {
    showToast({ title: '暂无知识库', message: '请等待管理员分配团队或知识库权限。', type: 'warning' })
    return
  }
  router.push(`/kb/${activeKbId.value}`)
}

function createDoc() {
  if (!activeKbId.value) {
    showToast({ title: '请选择知识库', message: '需要先选择一个知识库。', type: 'warning' })
    return
  }
  if (!canEditActiveKb.value) {
    showToast({ title: '只读权限', message: '你需要拥有编辑或管理权限后才能新建文档。', type: 'warning' })
    return
  }
  router.push(`/editor/${activeKbId.value}`)
}

function openDoc(docId) {
  if (!activeKbId.value || !docId) return
  router.push(`/editor/${activeKbId.value}/${docId}`)
}

function goSearch() {
  router.push(activeKbId.value ? { path: '/search', query: { kbId: activeKbId.value } } : '/search')
}

function kbTypeText(type) {
  return { COMPANY: '公开知识库', DEPARTMENT: '团队知识库', PRIVATE: '私有知识库' }[type] || '知识库'
}

function roleText(role) {
  return { ADMIN: '可管理', EDITOR: '可编辑', READER: '可查看', VIEWER: '可查看' }[role] || '可查看'
}

function roleTagType(role) {
  return { ADMIN: 'warning', EDITOR: 'success', READER: 'info', VIEWER: 'info' }[role] || 'info'
}

function formatDate(date) {
  if (!date) return '今天'
  const d = new Date(date)
  const now = new Date()
  const diff = Math.floor((now - d) / (1000 * 60 * 60 * 24))
  if (Number.isNaN(diff) || diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff < 7) return `${diff}天前`
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.user-dashboard { max-width: 1280px; margin: 0 auto; }
.hero-card { margin-bottom: 16px; border-color: #dfe6f1; }
.hero-copy { padding: 4px 0; }
.eyebrow { margin: 0 0 10px; color: #409eff; font-weight: 700; }
h1 { max-width: 760px; margin: 0; color: #1f2d3d; font-size: 28px; line-height: 1.35; }
.intro-text { max-width: 820px; margin: 14px 0 0; color: #606266; line-height: 1.8; }
.intro-actions { margin-top: 18px; display: flex; gap: 10px; flex-wrap: wrap; }
.stat-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
.stat-grid :deep(.el-statistic) { padding: 12px; border: 1px solid #edf0f6; border-radius: 6px; background: #f8fbff; }
.current-kb { display: grid; gap: 8px; }
.current-kb strong { font-size: 16px; }
.current-kb div { display: flex; gap: 8px; flex-wrap: wrap; }
.workflow-row, .doc-row { margin-top: 16px; }
.workflow-card { height: 100%; }
.workflow-index { color: #c0c4cc; font-size: 22px; font-weight: 700; line-height: 1; }
.workflow-card strong { display: block; margin-top: 8px; color: #1f2d3d; }
.workflow-card p { margin: 8px 0 0; color: #606266; line-height: 1.7; }
.section-card { height: 100%; }
.kb-grid { display: grid; gap: 10px; }
.kb-card-btn { width: 100%; min-height: 68px; border: 1px solid #ebeef5; border-radius: 6px; display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 12px; padding: 12px; background: #fff; text-align: left; cursor: pointer; }
.kb-card-btn:hover, .kb-card-btn.active { border-color: #409eff; background: #f5faff; }
.kb-mark { width: 38px; height: 38px; border-radius: 6px; display: grid; place-items: center; color: #fff; font-weight: 700; }
.kb-mark.COMPANY { background: #409eff; }
.kb-mark.DEPARTMENT { background: #67c23a; }
.kb-mark.PRIVATE { background: #e6a23c; }
.kb-info { min-width: 0; display: grid; gap: 3px; }
.kb-info strong, .kb-info small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-info small { color: #909399; }
:deep(.el-table__row) { cursor: pointer; }
@media (max-width: 768px) {
  h1 { font-size: 24px; }
  .stat-grid { grid-template-columns: 1fr; margin-top: 18px; }
  .kb-card-btn { grid-template-columns: 38px minmax(0, 1fr); }
  .kb-card-btn .el-tag { grid-column: 2; width: fit-content; }
}
</style>
