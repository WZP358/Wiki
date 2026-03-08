<template>
  <section class="panel page">
    <h2>管理员日志</h2>
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

onMounted(load)

async function load() {
  const res = await adminApi.logs(page.value, 20)
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
</script>

<style scoped>
.page {
  padding: 16px;
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
