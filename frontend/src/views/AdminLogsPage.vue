<template>
  <section class="panel page">
    <div class="toolbar">
      <h2>操作日志</h2>
      <div class="right">
        <button class="btn" @click="downloadCsv">导出CSV</button>
      </div>
    </div>

    <div class="filters">
      <input v-model="userId" class="input mini" placeholder="userId" />
      <input v-model="action" class="input" placeholder="action（如 ADMIN_PURGE_DOC）" />
      <input v-model="targetType" class="input mini" placeholder="targetType" />
      <input v-model="targetId" class="input mini" placeholder="targetId" />
      <input v-model="ip" class="input mini" placeholder="ip" />
      <input v-model="fromTime" class="input" placeholder="fromTime（ISO）" />
      <input v-model="toTime" class="input" placeholder="toTime（ISO）" />
      <button class="btn" @click="applyFilters">查询</button>
      <button class="btn" @click="resetFilters">重置</button>
    </div>
    <table>
      <thead>
        <tr>
          <th>时间</th>
          <th>用户</th>
          <th>IP</th>
          <th>操作</th>
          <th>对象</th>
          <th>详情</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="log in logs" :key="log.id">
          <td>{{ log.createdAt }}</td>
          <td>{{ log.username || '-' }}</td>
          <td>{{ log.ip || '-' }}</td>
          <td>{{ log.action }}</td>
          <td>{{ log.targetType }}:{{ log.targetId }}</td>
          <td>{{ log.detail }}</td>
        </tr>
      </tbody>
    </table>
    <div class="actions">
      <button class="btn" @click="prev" :disabled="page <= 0">上一页</button>
      <button class="btn" @click="next">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../api/modules'

const logs = ref([])
const page = ref(0)

const userId = ref('')
const action = ref('')
const targetType = ref('')
const targetId = ref('')
const ip = ref('')
const fromTime = ref('')
const toTime = ref('')

onMounted(load)

async function load() {
  const params = buildParams()
  const res = await adminApi.logs(page.value, 20, params)
  logs.value = res.content || []
}

async function prev() {
  if (page.value <= 0) {
    return
  }
  page.value -= 1
  await load()
}

async function next() {
  page.value += 1
  await load()
}

function buildParams() {
  const params = {}
  if (userId.value) params.userId = Number(userId.value)
  if (action.value.trim()) params.action = action.value.trim()
  if (targetType.value.trim()) params.targetType = targetType.value.trim()
  if (targetId.value.trim()) params.targetId = targetId.value.trim()
  if (ip.value.trim()) params.ip = ip.value.trim()
  if (fromTime.value.trim()) params.fromTime = fromTime.value.trim()
  if (toTime.value.trim()) params.toTime = toTime.value.trim()
  return params
}

async function applyFilters() {
  page.value = 0
  await load()
}

async function resetFilters() {
  userId.value = ''
  action.value = ''
  targetType.value = ''
  targetId.value = ''
  ip.value = ''
  fromTime.value = ''
  toTime.value = ''
  page.value = 0
  await load()
}

function downloadCsv() {
  const url = adminApi.exportOperationLogsUrl(buildParams())
  window.open(url, '_blank')
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.mini {
  width: 120px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th,
td {
  border: 1px solid var(--line);
  padding: 8px;
  text-align: left;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
