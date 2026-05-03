<template>
  <section class="page-panel">
    <div class="toolbar">
      <input v-model="keyword" class="input" placeholder="搜索名称或描述" @keyup.enter="reload" />
      <select v-model="type" class="input select">
        <option value="">全部类型</option>
        <option value="COMPANY">公司公开</option>
        <option value="DEPARTMENT">团队知识库</option>
        <option value="PRIVATE">私有空间</option>
      </select>
      <input v-model="ownerId" class="input mini" placeholder="ownerId" />
      <select v-model="deleted" class="input select">
        <option value="">全部状态</option>
        <option value="false">正常</option>
        <option value="true">已删除</option>
      </select>
      <button class="btn primary" type="button" @click="reload">查询</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>类型</th>
            <th>ownerId</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="kb in kbs" :key="kb.id">
            <td class="mono">{{ kb.id }}</td>
            <td>{{ kb.name }}</td>
            <td><span class="tag">{{ typeLabel(kb.type) }}</span></td>
            <td class="mono">{{ kb.ownerId }}</td>
            <td><span :class="['pill', kb.deleted ? 'bad' : 'ok']">{{ kb.deleted ? '已删除' : '正常' }}</span></td>
            <td class="mono">{{ fmt(kb.updatedAt) }}</td>
            <td class="ops">
              <button class="btn" type="button" @click="goAuditDocs(kb)">查看文档</button>
              <button class="btn" type="button" @click="toggleDeleted(kb)">{{ kb.deleted ? '恢复' : '删除' }}</button>
              <button class="btn danger" type="button" @click="purge(kb)">彻底删除</button>
            </td>
          </tr>
          <tr v-if="kbs.length === 0">
            <td colspan="7" class="empty">暂无数据</td>
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
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api/modules'
import { confirmDialog, showToast } from '../../utils/errorBus'

const router = useRouter()
const keyword = ref('')
const type = ref('')
const ownerId = ref('')
const deleted = ref('')
const kbs = ref([])
const page = ref(0)
const size = ref(20)
const totalPages = ref(1)
const total = ref(0)

onMounted(reload)

function fmt(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString('zh-CN')
}

function typeLabel(value) {
  return {
    COMPANY: '公司公开',
    DEPARTMENT: '团队知识库',
    PRIVATE: '私有空间'
  }[value] || value
}

function buildParams() {
  const params = { page: page.value, size: size.value }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (type.value) params.type = type.value
  if (ownerId.value) params.ownerId = Number(ownerId.value)
  if (deleted.value) params.deleted = deleted.value === 'true'
  return params
}

async function reload() {
  const res = await adminApi.kbs(buildParams())
  kbs.value = res.content || []
  totalPages.value = res.totalPages ?? 1
  total.value = res.totalElements ?? kbs.value.length
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

function goAuditDocs(kb) {
  router.push({ path: '/admin/audit', query: { kbId: kb.id } })
}

async function toggleDeleted(kb) {
  await adminApi.kbAction({
    kbId: kb.id,
    deleted: !kb.deleted,
    reason: kb.deleted ? 'Admin restore KB' : 'Admin soft delete KB'
  })
  await reload()
  showToast({
    title: kb.deleted ? '已恢复' : '已删除',
    message: kb.deleted ? '知识库已恢复。' : '知识库已移入回收站。',
    type: 'success'
  })
}

async function purge(kb) {
  const ok = await confirmDialog({
    title: '彻底删除知识库',
    message: '该操作会级联删除知识库下所有文档，且不可恢复。',
    tone: 'danger',
    confirmText: '彻底删除'
  })
  if (!ok) return
  await adminApi.kbAction({
    kbId: kb.id,
    purge: true,
    confirmed: true,
    reason: 'Admin purge KB'
  })
  await reload()
  showToast({ title: '已删除', message: '知识库已彻底删除。', type: 'success' })
}
</script>

<style scoped>
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

.select {
  width: auto;
  min-width: 150px;
}

.mini {
  width: 130px;
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
</style>
