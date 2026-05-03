<template>
  <div v-if="visible" class="template-modal-overlay" @click.self="$emit('close')">
    <div class="template-modal">
      <div class="modal-header">
        <h2>选择模板</h2>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>

      <div class="modal-body">
        <div class="category-tabs">
          <button
            v-for="cat in categories"
            :key="cat"
            class="tab-btn"
            :class="{ active: activeCategory === cat }"
            @click="activeCategory = cat"
          >
            {{ cat }}
          </button>
        </div>

        <div class="templates-grid">
          <div
            v-for="template in filteredTemplates"
            :key="template.id"
            class="template-card"
            @click="selectTemplate(template)"
          >
            <div v-if="template.coverUrl" class="template-cover">
              <img :src="template.coverUrl" :alt="template.name" />
            </div>
            <div class="template-info">
              <h3>{{ template.name }}</h3>
              <p class="desc">{{ template.description }}</p>
              <div class="meta">
                <span class="author">{{ template.creatorName }}</span>
                <span class="dot">·</span>
                <span class="uses">使用 {{ template.useCount }} 次</span>
              </div>
            </div>
          </div>

          <div v-if="filteredTemplates.length === 0" class="empty-state">
            <p>暂无模板</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { templateApi } from '../api/modules'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kbId: { type: [String, Number], default: null }
})

const emit = defineEmits(['close', 'select'])

const templates = ref([])
const activeCategory = ref('全部')

const categories = computed(() => {
  const cats = new Set(['全部'])
  templates.value.forEach(t => {
    if (t.category) cats.add(t.category)
  })
  return Array.from(cats)
})

const filteredTemplates = computed(() => {
  if (activeCategory.value === '全部') return templates.value
  return templates.value.filter(t => t.category === activeCategory.value)
})

onMounted(async () => {
  if (props.visible) {
    await loadTemplates()
  }
})

async function loadTemplates() {
  try {
    templates.value = await templateApi.list(props.kbId)
  } catch (err) {
    console.error('加载模板失败:', err)
  }
}

async function selectTemplate(template) {
  try {
    await templateApi.incrementUse(template.id)
    emit('select', template)
    emit('close')
  } catch (err) {
    console.error('选择模板失败:', err)
  }
}
</script>

<style scoped>
.template-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.template-modal {
  background: var(--panel);
  border-radius: 12px;
  width: 90%;
  max-width: 900px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 28px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover {
  background: var(--line-light);
}

.modal-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.category-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
}

.tab-btn {
  padding: 6px 16px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  white-space: nowrap;
  transition: all 0.2s;
}

.tab-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.tab-btn.active {
  background: var(--brand-light);
  color: var(--brand);
  font-weight: 500;
}

.templates-grid {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  align-content: start;
}

.template-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg);
}

.template-card:hover {
  border-color: var(--brand);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.template-cover {
  width: 100%;
  height: 140px;
  overflow: hidden;
  background: var(--line-light);
}

.template-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.template-info {
  padding: 16px;
}

.template-info h3 {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.desc {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--muted);
}

.dot {
  opacity: 0.5;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 20px;
  text-align: center;
  color: var(--muted);
}
</style>
