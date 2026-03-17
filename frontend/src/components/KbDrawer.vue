<template>
  <div class="kb-drawer" :class="{ open: visible }" @click="$emit('close')">
    <div class="kb-drawer-inner" @click.stop>
      <header class="kb-drawer-header">
        <div class="kb-title">
          <div class="kb-avatar-mini" :class="getKbClass(kb?.type)">
            {{ (kb?.name || 'K').charAt(0).toUpperCase() }}
          </div>
          <div class="kb-meta">
            <div class="name">{{ kb?.name || '知识库' }}</div>
            <div class="desc">{{ kb?.description || '文档目录' }}</div>
          </div>
        </div>
        <button class="btn-close" @click="$emit('close')" title="关闭">×</button>
      </header>

      <div class="kb-drawer-actions">
        <button class="btn btn-primary" @click="$emit('create-doc')">新建文档</button>
        <button class="btn-text btn-link" @click="$emit('go-home')">空间主页</button>
      </div>

      <div class="kb-drawer-body" @click="$emit('request-close-menus')">
        <div v-if="loading" class="empty">加载中...</div>
        <div v-else-if="tree.length === 0" class="empty">暂无文档</div>
        <DocTreeItem
          v-else
          v-for="node in tree"
          :key="node.id"
          :node="node"
          :level="0"
          :close-signal="closeSignal"
          :active-id="activeDocId"
          :expanded-ids="expandedIds"
          @toggle="toggleNode"
          @open="$emit('open-doc', $event)"
          @rename="$emit('rename-doc', $event)"
          @delete="$emit('delete-doc', $event)"
          @new-child="$emit('new-child', $event)"
          @new-sibling="$emit('new-sibling', $event)"
          @request-close-menus="$emit('request-close-menus')"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { kbApi, docApi } from '../api/modules'
import DocTreeItem from './DocTreeItem.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kbId: { type: [String, Number], default: '' },
  closeSignal: { type: Number, default: 0 },
  activeDocId: { type: [String, Number], default: '' }
})

defineEmits([
  'close',
  'open-doc',
  'create-doc',
  'go-home',
  'rename-doc',
  'delete-doc',
  'new-child',
  'new-sibling',
  'request-close-menus'
])

const kb = ref(null)
const tree = ref([])
const loading = ref(false)
const expandedIds = ref(new Set())

function storageKey() {
  return `kb-doc-tree-expanded:${String(props.kbId || '')}`
}

function loadExpandedState() {
  expandedIds.value = new Set()
  if (!props.kbId) return
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return
    const arr = JSON.parse(raw)
    if (Array.isArray(arr)) {
      expandedIds.value = new Set(arr.map(String))
    }
  } catch (e) {
    // ignore corrupted storage
  }
}

function saveExpandedState() {
  if (!props.kbId) return
  try {
    localStorage.setItem(storageKey(), JSON.stringify(Array.from(expandedIds.value)))
  } catch (e) {
    // ignore
  }
}

function toggleNode(id) {
  const key = String(id)
  const next = new Set(expandedIds.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedIds.value = next
  saveExpandedState()
}

function expandToActive() {
  const target = String(props.activeDocId || '')
  if (!target || !Array.isArray(tree.value) || tree.value.length === 0) return

  const next = new Set(expandedIds.value)

  const dfs = (node) => {
    if (!node) return false
    if (String(node.id) === target) return true
    const children = node.children || []
    for (const child of children) {
      if (dfs(child)) {
        next.add(String(node.id))
        return true
      }
    }
    return false
  }

  for (const root of tree.value) {
    dfs(root)
  }

  expandedIds.value = next
  saveExpandedState()
}

function getKbClass(type) {
  const map = {
    COMPANY: 'kb-company',
    DEPARTMENT: 'kb-department',
    PRIVATE: 'kb-private'
  }
  return map[type] || 'kb-company'
}

async function load() {
  if (!props.kbId) return
  loading.value = true
  try {
    kb.value = await kbApi.get(props.kbId)
    tree.value = await docApi.tree(props.kbId)
    // 默认展开第一层（仅在没有存储状态时）
    if (expandedIds.value.size === 0 && Array.isArray(tree.value)) {
      tree.value.forEach(n => expandedIds.value.add(String(n.id)))
      saveExpandedState()
    }
    expandToActive()
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.kbId],
  ([v]) => {
    if (props.kbId) {
      loadExpandedState()
    }
    if (v && props.kbId) load()
  }
)

onMounted(() => {
  if (props.visible && props.kbId) load()
})
</script>

<style scoped>
.kb-drawer {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100vw;
  background: transparent;
  opacity: 0;
  transition: opacity 0.16s ease;
  z-index: 900;
  pointer-events: none;
}

.kb-drawer.open {
  opacity: 1;
  pointer-events: auto;
}

.kb-drawer-inner {
  height: 100%;
  width: var(--drawer-w, 320px);
  margin-left: var(--sidebar-w, 240px);
  background: var(--panel);
  border-right: 1px solid var(--line);
  box-shadow: 12px 0 24px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
}

.kb-drawer-header {
  padding: 14px 14px 10px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.kb-title {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.kb-avatar-mini {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
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

.kb-meta {
  min-width: 0;
}

.name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-close {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 18px;
  line-height: 18px;
  padding: 4px 6px;
  border-radius: 6px;
}

.btn-close:hover {
  background: var(--line-light);
  color: var(--text);
}

.kb-drawer-actions {
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-link {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
}

.btn-link:hover {
  background: var(--line-light);
  color: var(--brand);
}

.kb-drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px 14px;
}

.empty {
  padding: 20px 0;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}
</style>

