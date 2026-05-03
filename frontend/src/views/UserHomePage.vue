<template>
  <main class="app-container ry-page">
    <el-card shadow="never" class="user-card">
      <template #header>
        <div class="ry-card-header">
          <div class="identity">
            <el-avatar :size="48" :src="user?.avatarUrl">{{ avatarText }}</el-avatar>
            <div>
              <h1>{{ displayName }}</h1>
              <div class="ry-muted">
                <span>ID: {{ userId }}</span>
                <span v-if="user?.username">账号：{{ user.username }}</span>
                <span v-if="orgText">{{ orgText }}</span>
              </div>
            </div>
          </div>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-card shadow="never">
        <template #header>
          <div class="ry-card-header">
            <span>公开知识库</span>
            <span class="ry-muted">{{ kbs.length }} 个</span>
          </div>
        </template>

        <el-empty v-if="!loading && kbs.length === 0" description="该用户暂无公开知识库。" />
        <el-table v-else v-loading="loading" :data="kbs" @row-click="row => $router.push(`/kb/${row.id}`)">
          <el-table-column label="名称" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="kb-name">
                <el-avatar shape="square" :size="30">{{ (row.name || 'K').charAt(0).toUpperCase() }}</el-avatar>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ kbTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-card>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { authApi, kbApi } from '../api/modules'

const route = useRoute()
const userId = route.params.userId
const user = ref(null)
const kbs = ref([])
const loading = ref(false)

const displayName = computed(() => user.value?.nickname || user.value?.displayName || user.value?.username || '用户主页')
const avatarText = computed(() => (displayName.value || 'U').charAt(0).toUpperCase())
const orgText = computed(() => [user.value?.departmentName, user.value?.positionName].filter(Boolean).join(' / '))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [userInfo, publicKbs] = await Promise.all([
      authApi.publicUserById(userId),
      kbApi.publicByUser(userId)
    ])
    user.value = userInfo
    kbs.value = Array.isArray(publicKbs) ? publicKbs : []
  } finally {
    loading.value = false
  }
}

function kbTypeLabel(type) {
  return {
    COMPANY: '公开',
    DEPARTMENT: '团队',
    PRIVATE: '私有'
  }[type] || '知识库'
}
</script>

<style scoped>
.identity,
.kb-name {
  display: flex;
  align-items: center;
  gap: 12px;
}

.identity h1 {
  margin-bottom: 4px;
  font-size: 20px;
}

.ry-muted {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.kb-name span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
