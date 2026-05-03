<template>
  <section class="panel page">
    <h2>后台总览</h2>
    <p class="sub">用户管理 / 团队管理 / 权限分配 / 内容审计 / 操作日志。</p>

    <div class="stats">
      <div class="stat">
        <div class="k">活跃用户</div>
        <div class="v">{{ stats.activeUsers ?? '-' }}</div>
      </div>
      <div class="stat">
        <div class="k">文档（正常）</div>
        <div class="v">{{ stats.totalDocs ?? '-' }}</div>
      </div>
      <div class="stat">
        <div class="k">文档（已删除）</div>
        <div class="v">{{ stats.deletedDocs ?? '-' }}</div>
      </div>
      <div class="stat">
        <div class="k">近24h 操作日志</div>
        <div class="v">{{ stats.operationLogs24h ?? '-' }}</div>
      </div>
      <div class="stat">
        <div class="k">近24h 查看</div>
        <div class="v">{{ stats.docViews24h ?? '-' }}</div>
      </div>
      <div class="stat">
        <div class="k">近24h 修改</div>
        <div class="v">{{ stats.docEdits24h ?? '-' }}</div>
      </div>
    </div>

    <div class="trend panel">
      <div class="trend-title">近7天操作日志趋势</div>
      <div class="bars">
        <div v-for="d in (stats.operationLogs7d || [])" :key="d.date" class="bar">
          <div class="h" :style="{ height: barHeight(d.count) }"></div>
          <div class="t">{{ shortDate(d.date) }}</div>
          <div class="c">{{ d.count }}</div>
        </div>
      </div>
    </div>

    <div class="grid">
      <button class="card" @click="go('/admin/users')">
        <div class="title">用户管理</div>
        <div class="desc">账号、角色、团队、启用状态管理。</div>
      </button>
      <button class="card" @click="go('/admin/departments')">
        <div class="title">团队管理</div>
        <div class="desc">团队树、负责人、启用/禁用与描述维护。</div>
      </button>
      <button class="card" @click="go('/admin/permissions')">
        <div class="title">权限分配</div>
        <div class="desc">全局角色与知识库成员权限配置。</div>
      </button>
      <button class="card" @click="go('/admin/audit')">
        <div class="title">内容审计</div>
        <div class="desc">文档检索、查看/修改记录追溯。</div>
      </button>
      <button class="card" @click="go('/admin/logs')">
        <div class="title">操作日志</div>
        <div class="desc">系统操作审计（管理员专用）。</div>
      </button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api/modules'
const router = useRouter()
function go(path) {
  router.push(path)
}

const stats = ref({})

onMounted(async () => {
  stats.value = await adminApi.statsOverview()
})

function barHeight(count) {
  const c = Number(count || 0)
  // map to 8..72px
  const h = Math.min(72, 8 + c * 2)
  return `${h}px`
}

function shortDate(v) {
  if (!v) return '-'
  return String(v).slice(5)
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.sub {
  margin: 6px 0 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.stats {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  border: 1px solid var(--line);
  background: var(--panel);
  border-radius: 12px;
  padding: 12px;
}

.k {
  font-size: 12px;
  color: var(--text-secondary);
}

.v {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 800;
}

.trend {
  margin-top: 12px;
  padding: 12px;
  box-shadow: var(--shadow-sm);
}

.trend-title {
  font-weight: 800;
  margin-bottom: 10px;
}

.bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.h {
  width: 100%;
  max-width: 42px;
  background: var(--brand-light);
  border: 1px solid var(--brand);
  border-radius: 10px;
}

.t {
  font-size: 12px;
  color: var(--text-secondary);
}

.c {
  font-size: 12px;
  color: var(--text-secondary);
}

.grid {
  margin-top: 14px;
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
  font-weight: 800;
  margin-bottom: 6px;
}

.desc {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: 1fr;
  }
  .bars {
    grid-template-columns: repeat(7, minmax(0, 1fr));
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

