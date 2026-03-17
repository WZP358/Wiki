<template>
  <div>
    <div
      class="doc-item"
      :class="{ child: level > 0 }"
      :style="{ paddingLeft: `${12 + level * 16}px` }"
      @click="$emit('open', node)"
      @mouseenter="hover = true"
      @mouseleave="hover = false"
    >
      <svg class="doc-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <path d="M14 2v6h6"/>
      </svg>
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

    <div v-if="node.children && node.children.length" class="children">
      <DocTreeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :close-signal="closeSignal"
        @open="$emit('open', $event)"
        @rename="$emit('rename', $event)"
        @delete="$emit('delete', $event)"
        @new-child="$emit('new-child', $event)"
        @new-sibling="$emit('new-sibling', $event)"
        @request-close-menus="$emit('request-close-menus')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

defineOptions({ name: 'DocTreeItem' })

const props = defineProps({
  node: { type: Object, required: true },
  level: { type: Number, default: 0 },
  closeSignal: { type: Number, default: 0 }
})

const emit = defineEmits(['open', 'rename', 'delete', 'new-child', 'new-sibling', 'request-close-menus'])

const hover = ref(false)
const menuOpen = ref(false)

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

.doc-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--text-secondary);
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
</style>

