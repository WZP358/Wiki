<template>
  <div>
    <div
      class="doc-item"
      :class="{ child: level > 0, active: isActive }"
      :style="{ paddingLeft: `${12 + level * 16}px` }"
      @click="$emit('open', node)"
    >
      <el-button v-if="hasChildren" text circle size="small" :title="isExpanded ? '收起' : '展开'" @click.stop="emitToggle">
        <el-icon><ArrowDown v-if="isExpanded" /><ArrowRight v-else /></el-icon>
      </el-button>
      <span v-else class="tree-toggle-placeholder"></span>

      <el-icon class="doc-icon"><Document /></el-icon>
      <span class="doc-title">{{ node.title }}</span>

      <el-dropdown trigger="click" placement="bottom-end" @command="handleCommand" @click.stop>
        <el-button text circle size="small" @click.stop>
          <el-icon><MoreFilled /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="rename">重命名</el-dropdown-item>
            <el-dropdown-item command="delete">删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <transition name="tree-collapse">
      <div v-if="hasChildren && isExpanded" class="children">
        <DocTreeItem
          v-for="child in node.children"
          :key="child.id"
          :node="child"
          :level="level + 1"
          :close-signal="closeSignal"
          :active-id="activeId"
          :expanded-ids="expandedIds"
          @toggle="$emit('toggle', $event)"
          @open="$emit('open', $event)"
          @rename="$emit('rename', $event)"
          @delete="$emit('delete', $event)"
          @request-close-menus="$emit('request-close-menus')"
        />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowDown, ArrowRight, Document, MoreFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'DocTreeItem' })

const props = defineProps({
  node: { type: Object, required: true },
  level: { type: Number, default: 0 },
  closeSignal: { type: Number, default: 0 },
  activeId: { type: [String, Number], default: '' },
  expandedIds: { type: Object, default: null }
})

const emit = defineEmits(['toggle', 'open', 'rename', 'delete', 'request-close-menus'])

const hasChildren = computed(() => (props.node?.children || []).length > 0)
const isExpanded = computed(() => {
  if (!props.expandedIds) return true
  return props.expandedIds.has(String(props.node.id))
})
const isActive = computed(() => String(props.node.id) === String(props.activeId || ''))

function emitToggle() {
  emit('toggle', props.node.id)
}

function handleCommand(command) {
  emit('request-close-menus')
  if (command === 'rename') emit('rename', props.node)
  if (command === 'delete') emit('delete', props.node)
}
</script>

<style scoped>
.doc-item {
  min-height: 34px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 -8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.doc-item:hover {
  background: var(--line-light);
}

.doc-item.active {
  background: var(--brand-light);
  color: var(--brand);
}

.tree-toggle-placeholder {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.doc-icon {
  flex-shrink: 0;
  color: #909399;
}

.doc-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text);
  font-size: 14px;
}

.doc-item.active .doc-title,
.doc-item.active .doc-icon {
  color: var(--brand);
}

.tree-collapse-enter-active,
.tree-collapse-leave-active {
  transition: all 0.18s ease-out;
}

.tree-collapse-enter-from,
.tree-collapse-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}
</style>
