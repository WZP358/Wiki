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
          placeholder="搜索文档..."
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
          <h3 class="filter-title">时间范围</h3>
          <select v-model="filters.timeRange" class="filter-select">
            <option value="">不限时间</option>
            <option value="today">今天</option>
            <option value="week">本周</option>
            <option value="month">本月</option>
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

const router = useRouter()
const searchQuery = ref('2026 技术路线')
const sortBy = ref('relevance')

const filters = ref({
  document: true,
  knowledge: false,
  member: false,
  timeRange: ''
})

const results = ref([
  {
    id: 1,
    type: 'document',
    title: '2026 技术路线图：通往 AIGC 原生之路',
    excerpt: '...这文档详细描述了 2026 年我对核心路的重点计划，特别是关于 技术路线 中的 Rust 迁移方案与 AI 模型集成...',
    author: '王经理',
    date: '2026年3月16日',
    views: 1200
  },
  {
    id: 2,
    type: 'knowledge',
    title: '2026 研发规范资产库',
    excerpt: '包含所有 2026 年发布的最新代码规范、技术栈选择标准和全流程审计规则...',
    author: '由研发部 管理',
    date: '45 篇文档',
    views: 0
  },
  {
    id: 3,
    type: 'document',
    title: '品牌新视觉应用：2026 视觉技术路线',
    excerpt: '本文讨论了如何在 3D 层叠感与温暖中性色应用在市场部所有有的数字资产中，作为 2026 年品牌的主力...',
    author: '赵市场',
    date: '2026年2月28日',
    views: 856
  }
])

const docCount = computed(() => results.value.filter(r => r.type === 'document').length)
const kbCount = computed(() => results.value.filter(r => r.type === 'knowledge').length)
const memberCount = computed(() => results.value.filter(r => r.type === 'member').length)

function getTypeLabel(type) {
  const labels = {
    document: '研发部 / 核心路线',
    knowledge: '知识库',
    member: '市场部 / 宣传'
  }
  return labels[type] || type
}

function performSearch() {
  console.log('Searching for:', searchQuery.value)
}

function openResult(item) {
  if (item.type === 'document') {
    router.push(`/editor/1/${item.id}`)
  }
}

onMounted(() => {
  performSearch()
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
