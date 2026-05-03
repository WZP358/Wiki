<template>
  <div class="admin-shell">
    <aside class="admin-nav panel">
      <div class="brand" @click="go('/admin')">
        <div class="logo">W</div>
        <div class="text">
          <div class="name">Wiki</div>
          <div class="sub">后台管理</div>
        </div>
      </div>

      <nav class="menu">
        <button class="item" :class="{ active: isActive('/admin') }" @click="go('/admin')">总览</button>
        <button class="item" :class="{ active: isActive('/admin/users') }" @click="go('/admin/users')">用户管理</button>
        <button class="item" :class="{ active: isActive('/admin/kbs') }" @click="go('/admin/kbs')">知识库管理</button>
        <button class="item" :class="{ active: isActive('/admin/departments') }" @click="go('/admin/departments')">团队管理</button>
        <button class="item" :class="{ active: isActive('/admin/permissions') }" @click="go('/admin/permissions')">权限分配</button>
        <button class="item" :class="{ active: isActive('/admin/audit') }" @click="go('/admin/audit')">内容审计</button>
        <button class="item" :class="{ active: isActive('/admin/logs') }" @click="go('/admin/logs')">操作日志</button>
      </nav>

      <div class="bottom">
        <button class="btn" @click="go('/')">返回工作台</button>
      </div>
    </aside>

    <section class="admin-main">
      <header class="admin-top">
        <div class="title">{{ title }}</div>
        <div class="spacer"></div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function go(path) {
  router.push(path)
}

function isActive(prefix) {
  return (route.path || '').startsWith(prefix)
}

const title = computed(() => {
  const p = route.path || '/admin'
  if (p.startsWith('/admin/users')) return '用户管理'
  if (p.startsWith('/admin/kbs')) return '知识库管理'
  if (p.startsWith('/admin/departments')) return '团队管理'
  if (p.startsWith('/admin/permissions')) return '权限分配'
  if (p.startsWith('/admin/audit')) return '内容审计'
  if (p.startsWith('/admin/logs')) return '操作日志'
  return '后台总览'
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  padding: 16px;
  min-height: 100vh;
}

.admin-nav {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.brand:hover {
  background: var(--line-light);
}

.logo {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: var(--brand);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
}

.text .name {
  font-weight: 800;
}

.text .sub {
  font-size: 12px;
  color: var(--text-secondary);
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text);
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.item:hover {
  background: var(--line-light);
  border-color: var(--brand);
  color: var(--brand);
}

.item.active {
  background: var(--brand-light);
  border-color: var(--brand);
  color: var(--brand);
}

.bottom {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

.admin-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--panel);
  box-shadow: var(--shadow-sm);
}

.title {
  font-weight: 800;
}

.spacer {
  flex: 1;
}

.admin-content {
  min-width: 0;
}

@media (max-width: 980px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }
}
</style>

