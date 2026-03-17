<template>
  <div class="kb-home">
    <header class="kb-header">
      <button class="back-btn" @click="$router.back()">←</button>
      <div class="kb-info" v-if="kb">
        <div class="kb-avatar-mini" :class="getKbClass(kb.type)">
          {{ kb.name.charAt(0).toUpperCase() }}
        </div>
        <div class="meta">
          <h2>{{ kb.name }}</h2>
          <p class="sub">
            <span>{{ typeText }}</span>
            <span v-if="kb.description" class="dot">·</span>
            <span v-if="kb.description">{{ kb.description }}</span>
          </p>
        </div>
      </div>
    </header>

    <main class="kb-content" v-if="kb">
      <section class="section">
        <div class="section-header">
          <h3>空间概览</h3>
          <div class="actions">
            <button class="btn-link" @click="renameKb">重命名</button>
            <button class="btn-link danger" @click="deleteKb">删除</button>
          </div>
        </div>
        <div class="stats">
          <div class="stat-item">
            <div class="label">最近更新</div>
            <div class="value">{{ latestDocs.length }} 篇</div>
          </div>
          <div class="stat-item">
            <div class="label">热门文档</div>
            <div class="value">{{ hotDocs.length }} 篇</div>
          </div>
          <div class="stat-item">
            <div class="label">成员</div>
            <div class="value">{{ members.length }}</div>
          </div>
        </div>
      </section>

      <section class="section two-column">
        <div class="col">
          <div class="section-header">
            <h3>最近更新</h3>
          </div>
          <div class="doc-list">
            <div
              v-for="doc in latestDocs"
              :key="doc.id"
              class="doc-row"
              @click="openDoc(doc.id)"
            >
              <div class="title">{{ doc.title }}</div>
            </div>
            <div v-if="latestDocs.length === 0" class="empty-tip">暂无最近更新</div>
          </div>
        </div>
        <div class="col">
          <div class="section-header">
            <h3>热门文档</h3>
          </div>
          <div class="doc-list">
            <div
              v-for="doc in hotDocs"
              :key="doc.id"
              class="doc-row"
              @click="openDoc(doc.id)"
            >
              <div class="title">{{ doc.title }}</div>
            </div>
            <div v-if="hotDocs.length === 0" class="empty-tip">暂无热门文档</div>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h3>成员</h3>
        </div>
        <div class="member-list">
          <div
            v-for="m in members"
            :key="m.userId"
            class="member-item"
          >
            <div class="avatar">{{ (m.nickname || m.username || '?').slice(0,1) }}</div>
            <div class="info">
              <div class="name">{{ m.nickname || m.username }}</div>
              <div class="role">{{ m.role }}</div>
            </div>
          </div>
          <div v-if="members.length === 0" class="empty-tip">暂无成员信息</div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { kbApi, docApi } from '../api/modules'

const route = useRoute()
const router = useRouter()

const kbId = route.params.kbId

const kb = ref(null)
const latestDocs = ref([])
const hotDocs = ref([])
const members = ref([])

const typeText = computed(() => {
  if (!kb.value) return ''
  if (kb.value.type === 'COMPANY') return '公司公开'
  if (kb.value.type === 'DEPARTMENT') return '部门空间'
  if (kb.value.type === 'PRIVATE') return '私有空间'
  return ''
})

function getKbClass(type) {
  const map = {
    COMPANY: 'kb-company',
    DEPARTMENT: 'kb-department',
    PRIVATE: 'kb-private'
  }
  return map[type] || 'kb-company'
}

function openDoc(docId) {
  router.push(`/editor/${kbId}/${docId}`)
}

onMounted(async () => {
  kb.value = await kbApi.get(kbId)
  latestDocs.value = await docApi.latest(kbId)
  hotDocs.value = await docApi.hot(kbId)
  members.value = await kbApi.members(kbId)
})

async function renameKb() {
  if (!kb.value) return
  const name = window.prompt('请输入新的知识库名称', kb.value.name)
  if (!name) return
  const updated = await kbApi.update(kbId, {
    name,
    type: kb.value.type,
    description: kb.value.description
  })
  kb.value = updated
}

async function deleteKb() {
  if (!kb.value) return
  if (!window.confirm('删除知识库会将其中的文档一并移入回收站，确定删除吗？')) return
  await kbApi.remove(kbId)
  router.push('/')
}
</script>

<style scoped>
.kb-home {
  min-height: 100vh;
  background: var(--bg);
}

.kb-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kb-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.kb-company {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}
.kb-department {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}
.kb-private {
  background: linear-gradient(135deg, #faad14, #ffc53d);
}

.meta h2 {
  margin: 0;
  font-size: 18px;
}

.sub {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.dot::before {
  content: '·';
  margin: 0 4px;
}

.kb-content {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 18px 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-link {
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 13px;
  cursor: pointer;
}

.btn-link.danger {
  color: #ff7875;
}

.section-header h3 {
  margin: 0;
  font-size: 15px;
}

.stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  min-width: 120px;
}

.label {
  font-size: 12px;
  color: var(--text-secondary);
}

.value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.col {
  min-width: 0;
}

.doc-list {
  max-height: 260px;
  overflow-y: auto;
}

.doc-row {
  padding: 6px 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text);
}

.doc-row:hover {
  background: var(--line-light);
}

.title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-tip {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--bg);
}

.member-item .avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--brand-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.member-item .name {
  font-size: 13px;
}

.member-item .role {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>

