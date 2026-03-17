<template>
  <div>
    <div
      class="doc-item"
      :class="{ child: level > 0, active: isActive }"
      :style="{ paddingLeft: `${12 + level * 16}px` }"
      @click="$emit('open', node)"
      @mouseenter="hover = true"
      @mouseleave="hover = false"
    >
      <button
        v-if="hasChildren"
        class="tree-toggle-btn"
        :title="isExpanded ? '收起' : '展开'"
        @click.stop="emitToggle"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path :d="isExpanded ? 'M9 18l6-6-6-6' : 'M6 9l6 6 6-6'" />
        </svg>
      </button>
      <span v-else class="tree-toggle-placeholder"></span>
      <div class="doc-content">
        <h4 class="doc-title">{{ node.title }}</h4>
      </div>

      <button
        v-show="hover"
        class="doc-menu-btn"
        @click.stop="toggleMenu"
        title="更多操作"
      >
        ···
      </button>

      <div v-if="menuOpen" class="doc-menu" @click.stop>
        <button class="doc-menu-item" @click="emitNewChild">新建子文档</button>
        <button class="doc-menu-item" @click="emitNewSibling">新建同级文档</button>
        <div class="doc-menu-divider"></div>
        <button class="doc-menu-item" @click="emitRename">重命名</button>
        <button class="doc-menu-item danger" @click="emitDelete">删除</button>
      </div>
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
          @new-child="$emit('new-child', $event)"
          @new-sibling="$emit('new-sibling', $event)"
          @request-close-menus="$emit('request-close-menus')"
        />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

defineOptions({ name: 'DocTreeItem' })

const props = defineProps({
  node: { type: Object, required: true },
  level: { type: Number, default: 0 },
  closeSignal: { type: Number, default: 0 },
  activeId: { type: [String, Number], default: '' },
  expandedIds: { type: Object, default: null } // Set<string>
})

const emit = defineEmits(['toggle', 'open', 'rename', 'delete', 'new-child', 'new-sibling', 'request-close-menus'])

const hover = ref(false)
const menuOpen = ref(false)

const hasChildren = computed(() => (props.node?.children || []).length > 0)
const isExpanded = computed(() => {
  if (!props.expandedIds) return true
  return props.expandedIds.has(String(props.node.id))
})
const isActive = computed(() => String(props.node.id) === String(props.activeId || ''))

watch(hover, (h) => {
  if (!h) menuOpen.value = false
})

watch(
  () => props.closeSignal,
  () => {
    menuOpen.value = false
  }
)

function closeMenu() {
  menuOpen.value = false
  hover.value = false
}

function toggleMenu() {
  emit('request-close-menus')
  menuOpen.value = !menuOpen.value
}

function emitToggle() {
  emit('toggle', props.node.id)
}

function emitRename() {
  closeMenu()
  emit('rename', props.node)
}

function emitDelete() {
  closeMenu()
  emit('delete', props.node)
}

function emitNewChild() {
  closeMenu()
  emit('new-child', props.node)
}

function emitNewSibling() {
  closeMenu()
  emit('new-sibling', props.node)
}
</script>

<style scoped>
.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  margin: 0 -10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
  position: relative;
}

.doc-item:hover {
  background: var(--line-light);
}

.doc-item.active {
  background: var(--brand-light);
  color: var(--brand);
}

.tree-toggle-btn {
  width: 16px;
  height: 16px;
  border: none;
  padding: 0;
  margin-right: 2px;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}

.tree-toggle-btn svg {
  width: 14px;
  height: 14px;
}

.tree-toggle-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.tree-toggle-placeholder {
  width: 16px;
  height: 16px;
  margin-right: 2px;
  flex-shrink: 0;
}

.doc-content {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-menu-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
}

.doc-menu-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.doc-menu {
  position: absolute;
  right: 8px;
  top: 34px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: var(--shadow);
  padding: 6px;
  z-index: 10;
  min-width: 120px;
}

.doc-menu-item {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text);
  cursor: pointer;
}

.doc-menu-item:hover {
  background: var(--line-light);
}

.doc-menu-item.danger {
  color: #ff7875;
}

.doc-menu-divider {
  height: 1px;
  background: var(--line);
  margin: 6px 4px;
  opacity: 0.85;
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

