<template>
  <div class="favorites-page">
    <header class="page-header">
      <h2 class="title">收藏</h2>
      <div class="sub">快速访问你收藏的文档</div>
    </header>

    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="groups.length === 0" class="empty">暂无收藏</div>

    <div v-else class="groups">
      <section v-for="g in groups" :key="g.kbId" class="group">
        <div class="group-header">
          <div class="kb-avatar">{{ (g.kbName || 'K').charAt(0).toUpperCase() }}</div>
          <div class="kb-name">{{ g.kbName }}</div>
          <div class="count">{{ g.docs.length }}</div>
        </div>
        <div class="docs">
          <button
            v-for="d in g.docs"
            :key="d.docId"
            class="doc-row"
            @click="openDoc(g.kbId, d.docId)"
          >
            <span class="doc-title">{{ d.docTitle }}</span>
            <span class="doc-time">{{ formatTime(d.createdAt) }}</span>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '../api/modules'

const router = useRouter()
const loading = ref(false)
const favorites = ref([])

const groups = computed(() => {
  const map = new Map()
  for (const fav of favorites.value || []) {
    if (!fav.kbId) continue
    if (!map.has(fav.kbId)) {
      map.set(fav.kbId, { kbId: fav.kbId, kbName: fav.kbName || '知识库', docs: [] })
    }
    map.get(fav.kbId).docs.push({
      docId: fav.docId,
      docTitle: fav.docTitle || '未命名',
      createdAt: fav.createdAt
    })
  }
  return Array.from(map.values())
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    favorites.value = await favoriteApi.mine()
  } finally {
    loading.value = false
  }
}

function openDoc(kbId, docId) {
  if (!kbId || !docId) return
  router.push(`/editor/${kbId}/${docId}`)
}

function formatTime(iso) {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch (e) {
    return ''
  }
}
</script>

<style scoped>
.favorites-page {
  max-width: 980px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 14px;
}

.title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text);
}

.sub {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
}

.empty {
  padding: 24px 8px;
  text-align: center;
  color: var(--muted);
}

.groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group {
  border: 1px solid var(--line);
  background: var(--panel);
  border-radius: 12px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
}

.kb-avatar {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.kb-name {
  font-weight: 700;
  color: var(--text);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.count {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--line-light);
  padding: 2px 8px;
  border-radius: 999px;
}

.docs {
  display: flex;
  flex-direction: column;
}

.doc-row {
  border: none;
  background: transparent;
  text-align: left;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
  border-top: 1px solid var(--line);
}

.doc-row:first-child {
  border-top: none;
}

.doc-row:hover {
  background: var(--line-light);
}

.doc-title {
  color: var(--text);
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-time {
  color: var(--text-secondary);
  font-size: 12px;
  flex-shrink: 0;
}
</style>

