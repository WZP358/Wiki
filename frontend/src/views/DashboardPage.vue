<template>
  <div class="dashboard">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1>工作台</h1>
      <button class="btn btn-primary" @click="createDoc">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        新建文档
      </button>
    </div>

    <!-- 主内容区 - 左右分栏 -->
    <div class="dashboard-content">
      <!-- 左侧主栏 -->
      <div class="main-column">
        <!-- 最近访问 -->
        <section class="section">
          <div class="section-header">
            <h3 class="section-title">最近访问</h3>
            <a href="#" class="link-more">查看全部</a>
          </div>
          <div class="doc-list">
            <div
              v-for="doc in latest.slice(0, 6)"
              :key="doc.id"
              class="doc-item"
              @click="openDoc(doc.id)"
            >
              <svg class="doc-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6"/>
              </svg>
              <div class="doc-content">
                <h4 class="doc-title">{{ doc.title }}</h4>
                <div class="doc-meta">
                  <span>{{ doc.author || '未知' }}</span>
                  <span class="dot">·</span>
                  <span>{{ formatDate(doc.updatedAt) }}</span>
                </div>
              </div>
            </div>
            <div v-if="latest.length === 0" class="empty-tip">
              暂无最近访问的文档
            </div>
          </div>
        </section>

        <!-- 参与的知识库 -->
        <section class="section">
          <div class="section-header">
            <h3 class="section-title">参与的知识库</h3>
            <button class="link-more" @click="showCreateKb = !showCreateKb">
              + 新建
            </button>
          </div>

          <div v-if="showCreateKb" class="create-form">
            <input v-model="newKb.name" class="input" placeholder="知识库名称" />
            <select v-model="newKb.type" class="input">
              <option value="COMPANY">公司公开</option>
              <option value="DEPARTMENT">部门</option>
              <option value="PRIVATE">私有</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="createKb">创建</button>
            <button class="btn btn-sm" @click="showCreateKb = false">取消</button>
          </div>

          <div class="kb-list">
            <div
              v-for="kb in kbs"
              :key="kb.id"
              class="kb-item"
              @click="pickKb(kb.id)"
            >
              <div class="kb-avatar" :class="getKbClass(kb.type)">
                {{ kb.name.charAt(0).toUpperCase() }}
              </div>
              <div class="kb-content">
                <h4 class="kb-name">{{ kb.name }}</h4>
                <p class="kb-meta">{{ getKbMeta(kb) }}</p>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 右侧边栏 -->
      <div class="side-column">
        <!-- 快捷操作 -->
        <section class="section">
          <h3 class="section-title">快捷操作</h3>
          <div class="quick-actions">
            <button class="quick-btn" @click="createDoc">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
              </svg>
              <span>导入文件</span>
            </button>
            <button class="quick-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
              </svg>
              <span>备份归档</span>
            </button>
          </div>
        </section>

        <!-- 统计信息 -->
        <section class="section">
          <h3 class="section-title">统计</h3>
          <div class="stats-list">
            <div class="stat-item">
              <div class="stat-label">知识库</div>
              <div class="stat-value">{{ kbs.length }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">文档</div>
              <div class="stat-value">{{ totalDocs }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">今日访问</div>
              <div class="stat-value">{{ latest.length }}</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { kbApi, docApi } from '../api/modules'

const router = useRouter()
const kbs = ref([])
const activeKbId = ref('')
const latest = ref([])
const hot = ref([])
const showCreateKb = ref(false)
const userName = ref('张小新')

const newKb = reactive({
  name: '',
  type: 'COMPANY'
})

const totalDocs = computed(() => latest.value.length + hot.value.length)
const viewRate = computed(() => latest.value.length > 0 ? 98 : 0)

onMounted(async () => {
  await loadKbs()
  if (kbs.value[0]) {
    await pickKb(kbs.value[0].id)
  }
})

async function loadKbs() {
  kbs.value = await kbApi.mine()
}

async function createKb() {
  if (!newKb.name) {
    return
  }
  await kbApi.create(newKb)
  newKb.name = ''
  showCreateKb.value = false
  await loadKbs()
}

async function pickKb(kbId) {
  activeKbId.value = kbId
  latest.value = await docApi.latest(kbId)
  hot.value = await docApi.hot(kbId)
}

async function createDoc() {
  if (!activeKbId.value && kbs.value[0]) {
    activeKbId.value = kbs.value[0].id
  }
  const created = await docApi.create({
    kbId: activeKbId.value,
    title: '未命名文档',
    markdownContent: '# 新文档\n',
    visibility: 'PUBLIC',
    published: true
  })
  router.push(`/editor/${activeKbId.value}/${created.id}`)
}

function openDoc(docId) {
  router.push(`/editor/${activeKbId.value}/${docId}`)
}

function getKbClass(type) {
  const typeMap = {
    'COMPANY': 'kb-company',
    'DEPARTMENT': 'kb-department',
    'PRIVATE': 'kb-private'
  }
  return typeMap[type] || 'kb-company'
}

function getKbMeta(kb) {
  const typeText = {
    'COMPANY': '公司公开',
    'DEPARTMENT': '部门',
    'PRIVATE': '私有'
  }
  return `${latest.value.length} 篇文档 · ${typeText[kb.type] || '公司公开'}`
}

function formatDate(date) {
  if (!date) return '今天'
  const d = new Date(date)
  const now = new Date()
  const diff = Math.floor((now - d) / (1000 * 60 * 60 * 24))
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff < 7) return `${diff}天前`
  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

/* 顶部标题栏 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.page-header .btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-header .btn svg {
  width: 16px;
  height: 16px;
}

/* 左右分栏布局 */
.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
}

.main-column {
  min-width: 0;
}

.side-column {
  min-width: 0;
}

/* 通用区块样式 */
.section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.link-more {
  font-size: 13px;
  color: var(--brand);
  text-decoration: none;
  cursor: pointer;
  background: none;
  border: none;
  padding: 0;
  transition: color 0.2s;
}

.link-more:hover {
  color: var(--brand-hover);
}

/* 文档列表 */
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin: 0 -12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.doc-item:hover {
  background: var(--line-light);
}

.doc-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--text-secondary);
}

.doc-content {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.doc-meta .dot {
  color: var(--muted);
}

/* 知识库列表 */
.kb-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin: 0 -12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.kb-item:hover {
  background: var(--line-light);
}

.kb-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
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

.kb-content {
  flex: 1;
  min-width: 0;
}

.kb-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-meta {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

/* 创建表单 */
.create-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--line-light);
  border-radius: 6px;
  margin-bottom: 16px;
}

.btn-sm {
  padding: 5px 12px;
  font-size: 13px;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: var(--text);
  width: 100%;
  text-align: left;
}

.quick-btn:hover {
  background: var(--line-light);
  border-color: var(--brand);
}

.quick-btn svg {
  width: 16px;
  height: 16px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

/* 统计信息 */
.stats-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
}

/* 空状态 */
.empty-tip {
  padding: 40px 20px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .dashboard-content {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .side-column .section {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }

  .side-column {
    grid-template-columns: 1fr;
  }
}
</style>
