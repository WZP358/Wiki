<template>
  <div class="dashboard">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1>工作台</h1>
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
      </div>

      <!-- 右侧边栏 -->
      <div class="side-column">
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
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { kbApi, docApi } from '../api/modules'

const route = useRoute()
const router = useRouter()
const kbs = ref([])
const activeKbId = ref('')
const latest = ref([])
const hot = ref([])

const totalDocs = computed(() => latest.value.length + hot.value.length)

onMounted(async () => {
  await loadKbs()
  const kbFromQuery = route.query.kbId
  if (kbFromQuery) {
    await pickKb(kbFromQuery)
  } else if (kbs.value[0]) {
    await pickKb(kbs.value[0].id)
  }
})

async function loadKbs() {
  kbs.value = await kbApi.mine()
}

async function pickKb(kbId) {
  activeKbId.value = kbId
  latest.value = await docApi.latest(kbId)
  hot.value = await docApi.hot(kbId)
}

function openDoc(docId) {
  router.push(`/editor/${activeKbId.value}/${docId}`)
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
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
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

/* 文档行的样式已迁移到 DocTreeItem 组件中，避免重复与冲突 */

.btn-new-doc {
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  background: var(--brand);
  color: #fff;
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
