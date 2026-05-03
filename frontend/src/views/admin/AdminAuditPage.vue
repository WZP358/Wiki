<template>
  <div class="audit-page">
    <section class="page-panel">
      <div class="toolbar">
        <input v-model="keyword" class="input" placeholder="搜索标题或内容" @keyup.enter="reload" />
        <input v-model="kbId" class="input mini" placeholder="kbId" />
        <input v-model="ownerId" class="input mini" placeholder="ownerId" />
        <select v-model="visibility" class="input select">
          <option value="">全部可见性</option>
          <option value="PUBLIC">公开</option>
          <option value="TEAM">团队</option>
          <option value="PRIVATE">私有</option>
        </select>
        <select v-model="deleted" class="input select">
          <option value="">全部状态</option>
          <option value="false">正常</option>
          <option value="true">已删除</option>
        </select>
        <button class="btn primary" type="button" @click="reload">查询</button>
        <button class="btn" type="button" @click="downloadDocsCsv">导出 CSV</button>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>kbId</th>
              <th>ownerId</th>
              <th>可见性</th>
              <th>发布</th>
              <th>删除</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in docs" :key="doc.id">
              <td class="mono">{{ doc.id }}</td>
              <td class="title">{{ doc.title }}</td>
              <td class="mono">{{ doc.kbId }}</td>
              <td class="mono">{{ doc.ownerId }}</td>
              <td><span class="tag">{{ visibilityLabel(doc.visibility) }}</span></td>
              <td>{{ doc.published ? '是' : '否' }}</td>
              <td><span :class="['pill', doc.deleted ? 'bad' : 'ok']">{{ doc.deleted ? '已删除' : '正常' }}</span></td>
              <td class="mono">{{ fmt(doc.updatedAt) }}</td>
              <td class="ops">
                <button class="btn" type="button" @click="openTrace(doc)">查看记录</button>
                <button class="btn" type="button" @click="toggleDeleted(doc)">{{ doc.deleted ? '恢复' : '软删' }}</button>
                <button class="btn" type="button" @click="togglePublished(doc)">{{ doc.published ? '下架' : '发布' }}</button>
              </td>
            </tr>
            <tr v-if="docs.length === 0">
              <td colspan="9" class="empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager">
        <button class="btn" type="button" :disabled="page <= 0" @click="prev">上一页</button>
        <div class="meta">第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页，{{ total }} 条</div>
        <button class="btn" type="button" :disabled="page + 1 >= totalPages" @click="next">下一页</button>
      </div>
    </section>

    <section v-if="traceDoc" class="page-panel">
      <div class="trace-head">
        <div>
          <h2>操作轨迹：{{ traceDoc.title }}</h2>
          <p class="meta mono">docId: {{ traceDoc.id }} · kbId: {{ traceDoc.kbId }} · ownerId: {{ traceDoc.ownerId }}</p>
        </div>
        <button class="btn" type="button" @click="closeTrace">关闭</button>
      </div>

      <div class="trace-grid">
        <section>
          <h3>修改记录</h3>
          <table class="mini-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>用户</th>
                <th>动作</th>
                <th>标题变更</th>
                <th>说明</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in editLogs" :key="log.id">
                <td class="mono">{{ fmt(log.createdAt) }}</td>
                <td class="mono">{{ log.userId }} / {{ log.username }}</td>
                <td><span class="tag">{{ log.action }}</span></td>
                <td>{{ log.titleBefore || '-' }} -> {{ log.titleAfter || '-' }}</td>
                <td>{{ log.commitMessage || '-' }}</td>
              </tr>
              <tr v-if="editLogs.length === 0">
                <td colspan="5" class="empty">暂无记录</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section>
          <h3>查看记录</h3>
          <table class="mini-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>用户</th>
                <th>IP</th>
                <th>User-Agent</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in viewLogs" :key="log.id">
                <td class="mono">{{ fmt(log.createdAt) }}</td>
                <td class="mono">{{ log.userId }} / {{ log.username }}</td>
                <td class="mono">{{ log.ip || '-' }}</td>
                <td class="ua">{{ log.userAgent || '-' }}</td>
              </tr>
              <tr v-if="viewLogs.length === 0">
                <td colspan="4" class="empty">暂无记录</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { adminApi } from '../../api/modules'
import { showToast } from '../../utils/errorBus'

const route = useRoute()
const keyword = ref('')
const kbId = ref('')
const ownerId = ref('')
const visibility = ref('')
const deleted = ref('')
const docs = ref([])
const page = ref(0)
const size = ref(20)
const totalPages = ref(1)
const total = ref(0)
const traceDoc = ref(null)
const viewLogs = ref([])
const editLogs = ref([])

onMounted(async () => {
  if (route.query?.kbId) {
    kbId.value = String(route.query.kbId)
  }
  await reload()
})

function fmt(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString('zh-CN')
}

function visibilityLabel(value) {
  return {
    PUBLIC: '公开',
    TEAM: '团队',
    PRIVATE: '私有'
  }[value] || value
}

function buildParams(includePage = true) {
  const params = includePage ? { page: page.value, size: size.value } : {}
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (kbId.value) params.kbId = Number(kbId.value)
  if (ownerId.value) params.ownerId = Number(ownerId.value)
  if (visibility.value) params.visibility = visibility.value
  if (deleted.value) params.deleted = deleted.value === 'true'
  return params
}

async function reload() {
  const res = await adminApi.auditDocs(buildParams())
  docs.value = res.content || []
  totalPages.value = res.totalPages ?? 1
  total.value = res.totalElements ?? docs.value.length
}

function downloadDocsCsv() {
  window.open(adminApi.exportDocsUrl(buildParams(false)), '_blank')
}

async function prev() {
  if (page.value <= 0) return
  page.value -= 1
  await reload()
}

async function next() {
  if (page.value + 1 >= totalPages.value) return
  page.value += 1
  await reload()
}

async function openTrace(doc) {
  traceDoc.value = doc
  const [views, edits] = await Promise.all([
    adminApi.auditDocViewLogs({ docId: doc.id, page: 0, size: 20 }),
    adminApi.auditDocEditLogs({ docId: doc.id, page: 0, size: 20 })
  ])
  viewLogs.value = views.content || []
  editLogs.value = edits.content || []
}

function closeTrace() {
  traceDoc.value = null
  viewLogs.value = []
  editLogs.value = []
}

async function toggleDeleted(doc) {
  await adminApi.docAction({
    docId: doc.id,
    deleted: !doc.deleted,
    reason: doc.deleted ? 'Admin restore (audit)' : 'Admin soft delete (audit)'
  })
  await reload()
  showToast({
    title: doc.deleted ? '已恢复' : '已删除',
    message: doc.deleted ? '文档已恢复。' : '文档已移入回收站。',
    type: 'success'
  })
}

async function togglePublished(doc) {
  await adminApi.docAction({
    docId: doc.id,
    published: !doc.published,
    reason: doc.published ? 'Admin unpublish (audit)' : 'Admin publish (audit)'
  })
  await reload()
  showToast({
    title: doc.published ? '已下架' : '已发布',
    message: doc.published ? '文档已下架。' : '文档已发布。',
    type: 'success'
  })
}

</script>

<style scoped>
.audit-page {
  display: grid;
  gap: 12px;
}

.page-panel {
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.mini {
  width: 120px;
}

.select {
  width: auto;
  min-width: 150px;
}

.btn.primary {
  background: var(--brand);
  border-color: var(--brand);
  color: #fff;
}

.table-wrap {
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th,
td {
  border-bottom: 1px solid var(--line);
  padding: 10px;
  text-align: left;
  background: var(--panel);
  vertical-align: middle;
}

th {
  background: var(--line-light);
  font-weight: 700;
  position: sticky;
  top: 0;
  z-index: 1;
}

.title {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}

.pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid var(--line);
}

.pill.ok {
  background: var(--brand-soft);
  color: var(--brand);
  border-color: var(--brand-light);
}

.pill.bad {
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
  border-color: rgba(239, 68, 68, 0.25);
}

.ops {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.danger {
  border-color: rgba(239, 68, 68, 0.35);
  color: var(--danger);
}

.empty {
  text-align: center;
  padding: 18px;
  color: var(--text-secondary);
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta {
  color: var(--text-secondary);
  font-size: 13px;
}

.trace-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.trace-head h2,
h3 {
  margin: 0 0 8px;
}

.trace-grid {
  display: grid;
  gap: 14px;
}

.mini-table {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
}

.mini-table th,
.mini-table td {
  padding: 8px;
}

.ua {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
