<template>
  <div class="search-page">
    <!-- 顶部导航 -->
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">
        <span class="icon">←</span>
      </button>
      <div class="search-bar">
        <span class="search-icon">🔍</span>
        <input
          v-model="searchQuery"
          class="search-input"
          placeholder="搜索文档 / 知识库 / 用户ID / 用户名..."
          @keyup.enter="performSearch"
        />
      </div>
    </header>

    <!-- 主内容区 -->
    <div class="search-content">
      <!-- 左侧筛选器 -->
      <aside class="filter-sidebar">
        <div class="filter-section">
          <h3 class="filter-title">内容类型</h3>
          <div class="filter-options">
            <label class="filter-option">
              <input type="checkbox" v-model="filters.document" />
              <span>文档 ({{ docCount }})</span>
            </label>
            <label class="filter-option">
              <input type="checkbox" v-model="filters.knowledge" />
              <span>知识库 ({{ kbCount }})</span>
            </label>
            <label class="filter-option">
              <input type="checkbox" v-model="filters.member" />
              <span>成员 ({{ memberCount }})</span>
            </label>
          </div>
        </div>

        <div class="filter-section">
          <h3 class="filter-title">部门</h3>
          <select v-model="selectedDeptId" class="filter-select">
            <option value="">全部部门</option>
            <option
              v-for="dept in departments"
              :key="dept.id"
              :value="String(dept.id)"
            >
              {{ dept.name }}
            </option>
          </select>
        </div>
      </aside>

      <!-- 右侧结果区 -->
      <main class="results-area">
        <div class="results-header">
          <p class="results-info">找到约 {{ results.length }} 个结果</p>
          <div class="sort-controls">
            <span class="sort-label">排序：</span>
            <select v-model="sortBy" class="sort-select">
              <option value="relevance">相关度</option>
              <option value="date">时间</option>
              <option value="views">浏览量</option>
            </select>
          </div>
        </div>

        <!-- 搜索结果列表 -->
        <div class="results-list">
          <article
            v-for="item in results"
            :key="item.id"
            class="result-card"
            @click="openResult(item)"
          >
            <div class="result-header">
              <span class="result-type" :class="`type-${item.type}`">
                {{ getTypeLabel(item.type) }}
              </span>
              <h3 class="result-title">{{ item.title }}</h3>
            </div>
            <p class="result-excerpt">{{ item.excerpt }}</p>
            <div class="result-meta">
              <span class="meta-item">
                <span class="meta-icon">👤</span>
                {{ item.author }}
              </span>
              <span class="meta-item">
                <span class="meta-icon">📅</span>
                {{ item.date }}
              </span>
              <span class="meta-item">
                <span class="meta-icon">👁️</span>
                {{ item.views }} 次阅读
              </span>
            </div>
          </article>

          <div v-if="results.length === 0" class="empty-results">
            <div class="empty-icon">🔍</div>
            <h3>未找到相关结果</h3>
            <p>尝试使用其他关键词或调整筛选条件</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, kbApi, deptApi } from '../api/modules'

const router = useRouter()
const searchQuery = ref('')
const sortBy = ref('relevance')

const filters = ref({
  document: true,
  knowledge: true,
  member: true
})

const results = ref([])
const departments = ref([])
const selectedDeptId = ref('')

const docCount = computed(() => results.value.filter(r => r.type === 'document').length)
const kbCount = computed(() => results.value.filter(r => r.type === 'knowledge').length)
const memberCount = computed(() => results.value.filter(r => r.type === 'member').length)

function getTypeLabel(type) {
  const labels = {
    document: '文档',
    knowledge: '知识库',
    member: '成员'
  }
  return labels[type] || type
}

function performSearch() {
  const keyword = searchQuery.value.trim()
  const newResults = []

  // 如果有关键字
  if (keyword) {
    // 纯数字优先按用户ID跳主页
    if (/^[0-9]+$/.test(keyword)) {
      router.push(`/user/${keyword}`)
      return
    }

    // 用户名搜索
    authApi.publicUserByUsername(keyword)
      .then(user => {
        if (filters.value.member) {
          newResults.push({
            id: user.id,
            type: 'member',
            title: user.nickname || user.username,
            excerpt: user.departmentName || '成员',
            author: user.username,
            date: '',
            views: 0
          })
        }
      })
      .catch(() => {})

    // 关键词知识库搜索
    kbApi.search(keyword)
      .then(kbs => {
        if (filters.value.knowledge) {
          for (const kb of kbs) {
            newResults.push({
              id: kb.id,
              type: 'knowledge',
              title: kb.name,
              excerpt: kb.description || '知识库',
              author: '',
              date: '',
              views: 0
            })
          }
        }
      })
      .finally(() => {
        // 如果没有按部门筛选，则直接使用
        if (!selectedDeptId.value) {
          results.value = newResults
        }
      })
  }

  // 部门视角：选了部门就根据部门拉知识库
  if (selectedDeptId.value) {
    kbApi.byDepartment(selectedDeptId.value)
      .then(kbs => {
        if (filters.value.knowledge) {
          for (const kb of kbs) {
            newResults.push({
              id: kb.id,
              type: 'knowledge',
              title: kb.name,
              excerpt: kb.description || '部门知识库',
              author: '',
              date: '',
              views: 0
            })
          }
        }
      })
      .finally(() => {
        results.value = newResults
      })
  } else if (!keyword) {
    // 没关键词也没部门，清空
    results.value = []
  }
}

function openResult(item) {
  if (item.type === 'document') {
    router.push(`/editor/${item.kbId}/${item.id}`)
  } else if (item.type === 'knowledge') {
    router.push(`/kb/${item.id}`)
  } else if (item.type === 'member') {
    router.push(`/user/${item.id}`)
  }
}

onMounted(async () => {
  try {
    departments.value = await deptApi.list()
  } catch (e) {
    console.error('Failed to load departments', e)
  }
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: var(--bg);
}

.page-header {
  background: var(--panel);
  border-bottom: 1px solid var(--line);
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.search-bar {
  flex: 1;
  max-width: 600px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--bg);
  border: 1px solid var(--line);
  border-radius: 8px;
  transition: all 0.2s;
}

.search-bar:focus-within {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.search-icon {
  font-size: 18px;
  color: var(--muted);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 15px;
  outline: none;
}

.search-input::placeholder {
  color: var(--muted);
}

.search-content {
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  gap: 24px;
}

/* 左侧筛选器 */
.filter-sidebar {
  width: 240px;
  flex-shrink: 0;
}

.filter-section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 16px 0;
}

.filter-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-option {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
  transition: color 0.2s;
}

.filter-option:hover {
  color: var(--brand);
}

.filter-option input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--brand);
}

.filter-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.filter-select:hover {
  border-color: var(--brand);
}

/* 右侧结果区 */
.results-area {
  flex: 1;
  min-width: 0;
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.results-info {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.sort-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.sort-select {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.sort-select:hover {
  border-color: var(--brand);
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.result-type {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.result-type.type-document {
  background: #E8F0FE;
  color: #1967D2;
}

.result-type.type-knowledge {
  background: #F3E5F5;
  color: #8E24AA;
}

.result-type.type-member {
  background: #FFF3E0;
  color: #F57C00;
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--brand);
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-excerpt {
  font-size: 14px;
  color: var(--text);
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.meta-icon {
  font-size: 14px;
}

.empty-results {
  text-align: center;
  padding: 80px 20px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-results h3 {
  font-size: 18px;
  color: var(--text);
  margin: 0 0 8px 0;
}

.empty-results p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

@media (max-width: 900px) {
  .search-content {
    flex-direction: column;
  }

  .filter-sidebar {
    width: 100%;
  }

  .filter-section {
    display: inline-block;
    margin-right: 12px;
    margin-bottom: 12px;
  }
}
</style>
