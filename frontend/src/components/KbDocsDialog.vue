<template>
  <div v-if="visible" class="kb-dialog-overlay" @click.self="$emit('close')">
    <div class="kb-dialog">
      <header class="kb-dialog-header">
        <div class="title">
          <div class="kb-avatar-mini kb-company">
            {{ kb?.name?.charAt(0).toUpperCase() }}
          </div>
          <div class="text">
            <h3>{{ kb?.name || '知识库' }}</h3>
            <p class="desc">{{ kb?.description || '点击左侧文档开始阅读' }}</p>
          </div>
        </div>
        <button class="btn-icon" @click="$emit('close')">×</button>
      </header>
      <div class="kb-dialog-body">
        <aside class="doc-tree">
          <h4>文档目录</h4>
          <div v-if="loading" class="empty">加载中...</div>
          <div v-else-if="tree.length === 0" class="empty">暂无文档</div>
          <div v-else class="tree-list">
            <DocTreeItem
              v-for="node in tree"
              :key="node.id"
              :node="node"
              :level="0"
              :active-id="activeDocId"
              :expanded-ids="expandedIds"
              @toggle="toggleNode"
              @open="handleOpen"
            />
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { docApi } from '../api/modules'
import DocTreeItem from './DocTreeItem.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kb: { type: Object, default: null },
  activeDocId: { type: [String, Number], default: '' }
})

const emit = defineEmits(['close', 'open-doc'])

const tree = ref([])
const loading = ref(false)
const expandedIds = ref(new Set())

function storageKey() {
  return `kb-doc-tree-expanded:${String(props.kb?.id || '')}`
}

function loadExpandedState() {
  expandedIds.value = new Set()
  if (!props.kb?.id) return
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return
    const arr = JSON.parse(raw)
    if (Array.isArray(arr)) {
      expandedIds.value = new Set(arr.map(String))
    }
  } catch (e) {
    // ignore
  }
}

function saveExpandedState() {
  if (!props.kb?.id) return
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

async function loadTree() {
  if (!props.kb?.id) {
    tree.value = []
    return
  }
  loading.value = true
  try {
    tree.value = await docApi.tree(props.kb.id)
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

function handleOpen(node) {
  emit('open-doc', { kbId: props.kb.id, docId: node.id })
}

watch(
  () => [props.visible, props.kb?.id],
  ([visible]) => {
    if (props.kb?.id) loadExpandedState()
    if (visible) loadTree()
  }
)

onMounted(() => {
  if (props.visible) {
    loadExpandedState()
    loadTree()
  }
})
</script>

<style scoped>
.kb-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.kb-dialog {
  width: 720px;
  max-height: 80vh;
  background: var(--panel);
  border-radius: 12px;
  border: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.kb-dialog-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kb-avatar-mini {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.kb-dialog-header h3 {
  margin: 0;
  font-size: 16px;
}

.desc {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.kb-dialog-body {
  display: flex;
  min-height: 260px;
}

.doc-tree {
  flex: 1;
  padding: 16px 20px 20px;
  overflow-y: auto;
}

.doc-tree h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
}

.empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.tree-list {
  margin: 0;
  padding: 0;
}

.btn-icon {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 18px;
}
</style>

