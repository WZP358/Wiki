<template>
  <aside class="left-sidebar" @click="$emit('request-close-menus')">
    <div class="sidebar-top">
      <div class="brand" @click="$emit('go-dashboard')">
        <div class="logo">W</div>
        <div class="brand-text">
          <div class="name">Wiki</div>
          <div class="sub">工作台</div>
        </div>
      </div>

      <nav class="primary">
        <button class="nav-btn" :class="{ active: activeKey === 'dashboard' }" @click="$emit('go-dashboard')">
          <span class="dot"></span>
          <span>概览</span>
        </button>
        <button class="nav-btn" :class="{ active: activeKey === 'search' }" @click="$emit('go-search')">
          <span class="dot"></span>
          <span>搜索</span>
        </button>
        <button class="nav-btn" :class="{ active: activeKey === 'favorites' }" @click="$emit('go-favorites')">
          <span class="dot"></span>
          <span>收藏</span>
        </button>
        <button class="nav-btn" :class="{ active: activeKey === 'recycle' }" @click="$emit('go-recycle')">
          <span class="dot"></span>
          <span>回收站</span>
        </button>
      </nav>
    </div>

    <div class="sidebar-section">
      <div class="section-header">
        <div class="title">知识库</div>
        <button class="icon-btn" title="刷新" @click="$emit('refresh-kbs')">↻</button>
      </div>

      <div v-if="loading" class="empty">加载中...</div>
      <div v-else-if="kbs.length === 0" class="empty">暂无知识库</div>

      <div v-else class="kb-list">
        <button
          v-for="kb in kbs"
          :key="kb.id"
          class="kb-row"
          :class="{ active: String(kb.id) === String(activeKbId) }"
          @click="$emit('select-kb', kb)"
        >
          <div class="kb-avatar" :class="getKbClass(kb.type)">
            {{ (kb.name || 'K').charAt(0).toUpperCase() }}
          </div>
          <div class="kb-name" :title="kb.name">{{ kb.name }}</div>
        </button>
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  kbs: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  activeKbId: { type: [String, Number], default: '' },
  activeKey: { type: String, default: 'dashboard' }
})

defineEmits([
  'select-kb',
  'refresh-kbs',
  'go-dashboard',
  'go-search',
  'go-favorites',
  'go-recycle',
  'request-close-menus'
])

function getKbClass(type) {
  const map = {
    COMPANY: 'kb-company',
    DEPARTMENT: 'kb-department',
    PRIVATE: 'kb-private'
  }
  return map[type] || 'kb-company'
}
</script>

<style scoped>
.left-sidebar {
  width: var(--sidebar-w, 240px);
  background: var(--panel);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
}

.sidebar-top {
  padding: 14px 12px 10px;
  border-bottom: 1px solid var(--line);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.brand:hover {
  background: var(--line-light);
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--brand);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.brand-text {
  min-width: 0;
}

.brand-text .name {
  font-weight: 700;
  color: var(--text);
  line-height: 1.1;
}

.brand-text .sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.primary {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-btn {
  border: none;
  background: transparent;
  color: var(--text);
  cursor: pointer;
  padding: 10px 10px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: background 0.2s, color 0.2s;
  text-align: left;
}

.nav-btn:hover {
  background: var(--line-light);
}

.nav-btn.active {
  background: var(--brand-light);
  color: var(--brand);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.35;
}

.nav-btn.active .dot {
  opacity: 1;
}

.sidebar-section {
  padding: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  margin-bottom: 6px;
}

.section-header .title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.icon-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s;
}

.icon-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.kb-list {
  overflow-y: auto;
  padding-right: 2px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb-row {
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 10px;
  text-align: left;
  transition: background 0.2s, color 0.2s;
}

.kb-row:hover {
  background: var(--line-light);
}

.kb-row.active {
  background: var(--brand-light);
  color: var(--brand);
}

.kb-avatar {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}

.kb-avatar.kb-company {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.kb-avatar.kb-department {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.kb-avatar.kb-private {
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
}

.kb-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text);
}

.kb-row.active .kb-name {
  color: var(--brand);
}

.empty {
  padding: 18px 8px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}
</style>

