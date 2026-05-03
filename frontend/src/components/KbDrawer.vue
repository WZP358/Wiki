<template>
  <div class="kb-drawer" :class="{ open: visible }" @click="$emit('close')">
    <div class="kb-drawer-inner" @click.stop>
      <header class="kb-drawer-header">
        <div class="kb-title">
          <el-avatar shape="square" :class="getKbClass(kb?.type)">{{ (kb?.name || 'K').charAt(0).toUpperCase() }}</el-avatar>
          <div class="kb-meta">
            <div class="name">{{ kb?.name || '知识库' }}</div>
            <div class="desc">{{ kb?.description || '文档目录' }}</div>
          </div>
        </div>
        <el-button text circle title="关闭" @click="$emit('close')">
          <el-icon><Close /></el-icon>
        </el-button>
      </header>

      <div class="kb-drawer-actions">
        <el-button type="primary" size="small" @click="$emit('create-doc')">新建文档</el-button>
        <el-button link type="primary" size="small" @click="$emit('go-home')">知识库主页</el-button>
      </div>

      <el-scrollbar class="kb-drawer-body" @click="$emit('request-close-menus')">
        <el-empty v-if="loading" description="正在加载文档目录..." :image-size="72" />
        <el-empty v-else-if="tree.length === 0" description="暂无文档，点击上方“新建文档”开始。" :image-size="72" />
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
          @request-close-menus="$emit('request-close-menus')"
        />
      </el-scrollbar>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { kbApi, docApi } from '../api/modules'
import DocTreeItem from './DocTreeItem.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kbId: { type: [String, Number], default: '' },
  closeSignal: { type: Number, default: 0 },
  refreshSignal: { type: Number, default: 0 },
  activeDocId: { type: [String, Number], default: '' }
})

defineEmits([
  'close',
  'open-doc',
  'create-doc',
  'go-home',
  'rename-doc',
  'delete-doc',
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
  } catch {
    // 忽略损坏的本地展开状态。
  }
}

function saveExpandedState() {
  if (!props.kbId) return
  try {
    localStorage.setItem(storageKey(), JSON.stringify(Array.from(expandedIds.value)))
  } catch {
    // 浏览器禁用存储时不影响目录使用。
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

  const dfs = node => {
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
    COMPANY: 'company',
    DEPARTMENT: 'department',
    PRIVATE: 'private'
  }
  return map[type] || 'company'
}

async function load() {
  if (!props.kbId) return
  loading.value = true
  try {
    kb.value = await kbApi.get(props.kbId)
    const docs = await docApi.tree(props.kbId)
    tree.value = Array.isArray(docs) ? docs : []
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

watch(
  () => props.activeDocId,
  () => expandToActive()
)

watch(
  () => props.refreshSignal,
  () => {
    if (props.visible && props.kbId) load()
  }
)

onMounted(() => {
  if (props.visible && props.kbId) load()
})
</script>

<style scoped>
.kb-drawer {
  position: fixed;
  inset: 0;
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

.el-avatar.company {
  background: #409eff;
}

.el-avatar.department {
  background: #67c23a;
}

.el-avatar.private {
  background: #e6a23c;
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

.kb-drawer-actions {
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.kb-drawer-body {
  flex: 1;
  padding: 10px 14px 14px;
}
</style>
