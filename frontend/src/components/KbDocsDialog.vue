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
          <ul v-else class="tree-list">
            <TreeNode
              v-for="node in tree"
              :key="node.id"
              :node="node"
              :level="0"
              @open="handleOpen"
            />
          </ul>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, watch, ref } from 'vue'
import { docApi } from '../api/modules'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kb: { type: Object, default: null }
})

const emit = defineEmits(['close', 'open-doc'])

const tree = ref([])
const loading = ref(false)

async function loadTree() {
  if (!props.kb?.id) {
    tree.value = []
    return
  }
  loading.value = true
  try {
    tree.value = await docApi.tree(props.kb.id)
  } finally {
    loading.value = false
  }
}

function handleOpen(node) {
  emit('open-doc', { kbId: props.kb.id, docId: node.id })
}

watch(
  () => props.visible,
  (v) => {
    if (v) {
      loadTree()
    }
  }
)

onMounted(() => {
  if (props.visible) {
    loadTree()
  }
})
</script>

<script>
// 内联递归组件
export default {
  name: 'KbDocsDialog',
  components: {
    TreeNode: {
      name: 'TreeNode',
      props: {
        node: { type: Object, required: true },
        level: { type: Number, default: 0 }
      },
      emits: ['open'],
      methods: {
        open() {
          this.$emit('open', this.node)
        }
      },
      template: `
        <li>
          <div
            class="tree-item"
            :style="{ paddingLeft: 12 + level * 16 + 'px' }"
            @click="open"
          >
            <span class="tree-title">{{ node.title }}</span>
          </div>
          <ul v-if="node.children && node.children.length" class="tree-children">
            <TreeNode
              v-for="child in node.children"
              :key="child.id"
              :node="child"
              :level="level + 1"
              @open="$emit('open', $event)"
            />
          </ul>
        </li>
      `
    }
  }
}
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
  list-style: none;
  margin: 0;
  padding: 0;
}

.tree-item {
  height: 30px;
  display: flex;
  align-items: center;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
}

.tree-item:hover {
  background: var(--line-light);
}

.tree-title {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-children {
  list-style: none;
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

