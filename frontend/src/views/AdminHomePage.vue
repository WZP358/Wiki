<template>
  <section class="panel page">
    <div class="header">
      <h2>后台管理</h2>
      <p class="sub">仅管理员可见。这里汇总管理功能入口。</p>
    </div>

    <div class="grid">
      <button class="card" @click="go('/admin/logs')">
        <div class="title">操作日志</div>
        <div class="desc">查看系统操作记录、审计管理员与用户行为。</div>
      </button>

      <button class="card" @click="go('/')">
        <div class="title">返回工作台</div>
        <div class="desc">回到普通使用界面（概览/知识库/编辑等）。</div>
      </button>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function go(path) {
  router.push(path)
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.header h2 {
  margin: 0;
}

.sub {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.card {
  border: 1px solid var(--line);
  background: var(--panel);
  border-radius: 12px;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.card:hover {
  background: var(--line-light);
  border-color: var(--brand);
}

.title {
  font-weight: 700;
  margin-bottom: 6px;
}

.desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

