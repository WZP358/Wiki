<template>
  <div class="reactions-bar">
    <el-button
      v-for="type in reactionTypes"
      :key="type.value"
      size="small"
      :type="stats?.userReacted?.[type.value] ? 'primary' : 'default'"
      plain
      @click="toggleReaction(type.value)"
    >
      {{ type.label }}
      <el-tag v-if="getCount(type.value) > 0" size="small" effect="plain" class="count-tag">{{ getCount(type.value) }}</el-tag>
    </el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { reactionApi } from '../api/modules'

const props = defineProps({
  docId: { type: [String, Number], required: true }
})

const stats = ref(null)

const reactionTypes = [
  { value: 'LIKE', label: '赞' },
  { value: 'LOVE', label: '喜欢' },
  { value: 'THUMBS_UP', label: '认可' },
  { value: 'CLAP', label: '鼓励' },
  { value: 'FIRE', label: '推荐' }
]

onMounted(async () => {
  await loadStats()
})

async function loadStats() {
  try {
    stats.value = await reactionApi.getStats(props.docId)
  } catch (err) {
    console.error('加载反应统计失败:', err)
  }
}

async function toggleReaction(type) {
  try {
    await reactionApi.toggle(props.docId, type)
    await loadStats()
  } catch (err) {
    console.error('切换反应失败:', err)
  }
}

function getCount(type) {
  return stats.value?.counts?.[type] || 0
}
</script>

<style scoped>
.reactions-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 16px 0;
}

.count-tag {
  margin-left: 6px;
}
</style>
