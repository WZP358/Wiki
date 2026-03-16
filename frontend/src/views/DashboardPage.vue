<template>
  <div class="dashboard">
    <div class="page-header">
      <h1>工作台</h1>
    </div>

    <!-- 欢迎横幅 -->
    <section class="welcome-banner">
      <div class="welcome-content">
        <h2 class="greeting">下午好，{{ userName }}</h2>
        <p class="welcome-text">今天共有 {{ latest.length }} 份文档等待您的查阅，部门知识库已新增 {{ kbs.length }} 个更新。</p>
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-value">{{ kbs.length }}</div>
            <div class="stat-label">知识库数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ totalDocs }}</div>
            <div class="stat-label">文档总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ viewRate }}%</div>
            <div class="stat-label">完成度</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 快捷操作 -->
    <section class="quick-actions">
      <h3 class="section-title">快捷操作</h3>
      <div class="actions-grid">
        <div class="action-card" @click="createDoc">
          <div class="action-icon upload">📤</div>
          <div class="action-label">导入文件</div>
        </div>
        <div class="action-card">
          <div class="action-icon backup">📋</div>
          <div class="action-label">备份归档</div>
        </div>
      </div>
    </section>

    <!-- 最近访问 -->
    <section class="recent-section">
      <div class="section-header">
        <h3 class="section-title">🔥 最近访问</h3>
        <a href="#" class="view-all">查看全部</a>
      </div>
      <div class="recent-grid">
        <div
          v-for="doc in latest.slice(0, 4)"
          :key="doc.id"
          class="recent-card"
          @click="openDoc(doc.id)"
        >
          <div class="recent-icon">📄</div>
          <div class="recent-info">
            <h4 class="recent-title">{{ doc.title }}</h4>
            <p class="recent-desc">{{ doc.summary || '暂无描述' }}</p>
            <div class="recent-meta">
              <span class="recent-author">来自 {{ doc.author || '未知' }}</span>
              <span class="recent-date">{{ formatDate(doc.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div v-if="latest.length === 0" class="empty-state">
          <p>暂无最近访问的文档</p>
        </div>
      </div>
    </section>

    <!-- 参与的知识库 -->
    <section class="kb-section">
      <div class="section-header">
        <h3 class="section-title">📚 参与的知识库</h3>
        <button class="btn btn-primary" @click="showCreateKb = !showCreateKb">
          + 新建知识库
        </button>
      </div>

      <div v-if="showCreateKb" class="create-form">
        <input v-model="newKb.name" class="input" placeholder="输入知识库名称..." />
        <select v-model="newKb.type" class="input select-type">
          <option value="COMPANY">🏢 公司公开（全公司可见）</option>
          <option value="DEPARTMENT">🏛️ 部门（部门内可见）</option>
          <option value="PRIVATE">🔒 私有（仅自己可见）</option>
        </select>
        <button class="btn btn-primary" @click="createKb">
          <span>创建</span>
        </button>
      </div>

      <div class="kb-grid">
        <div
          v-for="kb in kbs"
          :key="kb.id"
          class="kb-card"
          :class="getKbClass(kb.type)"
          @click="pickKb(kb.id)"
        >
          <div class="kb-avatar" :class="getKbClass(kb.type)">
            {{ kb.name.charAt(0).toUpperCase() }}
          </div>
          <div class="kb-info">
            <h4 class="kb-name">{{ kb.name }}</h4>
            <p class="kb-meta">{{ getKbMeta(kb) }}</p>
          </div>
        </div>
      </div>
    </section>
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

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text);
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #4169E1 0%, #5B7FFF 100%);
  border-radius: 16px;
  padding: 32px 40px;
  margin-bottom: 32px;
  color: white;
  box-shadow: 0 4px 20px rgba(65, 105, 225, 0.3);
}

.welcome-content {
  max-width: 100%;
}

.greeting {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 12px;
}

.welcome-text {
  font-size: 15px;
  opacity: 0.95;
  margin-bottom: 28px;
  line-height: 1.6;
}

.stats-row {
  display: flex;
  gap: 48px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  opacity: 0.85;
}

/* 快捷操作 */
.quick-actions {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.action-icon.upload {
  background: linear-gradient(135deg, #E8F0FE 0%, #D2E3FC 100%);
}

.action-icon.share {
  background: linear-gradient(135deg, #E6F7ED 0%, #C8E6C9 100%);
}

.action-icon.member {
  background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%);
}

.action-icon.backup {
  background: linear-gradient(135deg, #F3E5F5 0%, #E1BEE7 100%);
}

.action-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

/* 最近访问 */
.recent-section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.view-all {
  font-size: 14px;
  color: var(--brand);
  text-decoration: none;
  transition: color 0.2s ease;
}

.view-all:hover {
  color: var(--brand-hover);
}

.recent-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.recent-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.recent-icon {
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--line-light);
  border-radius: 8px;
}

.recent-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.recent-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--muted);
}

.recent-author,
.recent-date {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 知识库 */
.kb-section {
  margin-bottom: 32px;
}

.create-form {
  display: grid;
  grid-template-columns: 1fr 160px auto;
  gap: 12px;
  margin-bottom: 24px;
  padding: 20px;
  background: var(--line-light);
  border-radius: 12px;
}

.select-type {
  width: 160px;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.kb-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.kb-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.kb-avatar {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.kb-card.kb-company .kb-avatar {
  background: linear-gradient(135deg, #5B7FFF 0%, #4169E1 100%);
}

.kb-card.kb-department .kb-avatar {
  background: linear-gradient(135deg, #4DB6AC 0%, #00897B 100%);
}

.kb-card.kb-private .kb-avatar {
  background: linear-gradient(135deg, #FF8A65 0%, #FF6F00 100%);
}


.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-meta {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 20px;
  text-align: center;
  color: var(--muted);
  background: var(--line-light);
  border-radius: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .recent-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .kb-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .stats-row {
    gap: 32px;
  }

  .actions-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .recent-grid {
    grid-template-columns: 1fr;
  }

  .create-form {
    grid-template-columns: 1fr;
  }

  .select-type {
    width: 100%;
  }

  .kb-grid {
    grid-template-columns: 1fr;
  }
}
</style>
