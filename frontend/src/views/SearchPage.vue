<template>
  <main class="app-container ry-page">
    <el-card shadow="never" class="ry-filter-card">
      <template #header>
        <div class="ry-card-header">
          <div>
            <span>全文检索</span>
            <div class="ry-muted">按标题、正文、知识库或成员查找内容。</div>
          </div>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="queryForm" class="search-form">
        <el-form-item label="类型">
          <el-select v-model="searchType" style="width: 140px">
            <el-option label="文档" value="document" />
            <el-option label="知识库" value="knowledge" />
            <el-option label="成员账号" value="username" />
            <el-option label="成员 ID" value="userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchQuery" :placeholder="searchPlaceholder" clearable style="width: 320px" @keyup.enter="performSearch" />
        </el-form-item>
        <el-form-item v-if="searchType === 'document'" label="知识库">
          <el-select v-model="selectedKbId" placeholder="请选择知识库" filterable style="width: 220px">
            <el-option v-for="kb in kbs" :key="kb.id" :label="kb.name" :value="String(kb.id)" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="searchType === 'knowledge'" label="团队">
          <el-select v-model="selectedDeptId" placeholder="全部团队" clearable filterable style="width: 220px">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="String(dept.id)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="performSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="ry-card-header">
          <span>搜索结果</span>
          <span class="ry-muted">共 {{ results.length }} 条</span>
        </div>
      </template>

      <el-empty v-if="!loading && results.length === 0" description="输入关键词后开始检索。文档检索使用数据库 LIKE 查询标题和正文。" />
      <el-table v-else v-loading="loading" :data="results" @row-click="openResult">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="excerpt" label="摘要" min-width="360" show-overflow-tooltip />
      </el-table>
    </el-card>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi, kbApi, deptApi, docApi } from '../api/modules'
import { showToast } from '../utils/errorBus'

const route = useRoute()
const router = useRouter()
const queryForm = reactive({})
const searchQuery = ref('')
const searchType = ref('document')
const selectedKbId = ref('')
const selectedDeptId = ref('')
const results = ref([])
const departments = ref([])
const kbs = ref([])
const loading = ref(false)

const searchPlaceholder = computed(() => ({
  document: '输入文档标题或正文关键词',
  knowledge: '输入知识库名称或描述关键词',
  username: '输入成员账号',
  userId: '输入成员 ID'
}[searchType.value]))

onMounted(async () => {
  selectedKbId.value = String(route.query.kbId || '')
  await Promise.all([loadKbs(), loadDepartments()])
  if (!selectedKbId.value && kbs.value[0]) selectedKbId.value = String(kbs.value[0].id)
})

watch(searchType, () => {
  results.value = []
})

async function loadKbs() {
  try {
    const data = await kbApi.mine()
    kbs.value = Array.isArray(data) ? data : []
  } catch {
    kbs.value = []
  }
}

async function loadDepartments() {
  try {
    const data = await deptApi.list()
    departments.value = Array.isArray(data) ? data : []
  } catch {
    departments.value = []
  }
}

function resetSearch() {
  searchQuery.value = ''
  selectedDeptId.value = ''
  results.value = []
}

async function performSearch() {
  const keyword = searchQuery.value.trim()
  if (!keyword && !(searchType.value === 'knowledge' && selectedDeptId.value)) {
    showToast({ message: '请输入关键词', type: 'warning' })
    return
  }

  loading.value = true
  results.value = []
  try {
    if (searchType.value === 'document') {
      if (!selectedKbId.value) {
        showToast({ message: '请先选择知识库', type: 'warning' })
        return
      }
      const docs = await docApi.search(selectedKbId.value, keyword)
      results.value = (Array.isArray(docs) ? docs : []).map(doc => ({
        id: doc.id,
        kbId: doc.kbId,
        type: 'document',
        title: doc.title,
        excerpt: doc.searchHighlight || doc.markdownContent || '文档'
      }))
      return
    }

    if (searchType.value === 'knowledge') {
      const data = selectedDeptId.value ? await kbApi.byDepartment(selectedDeptId.value) : await kbApi.search(keyword)
      results.value = (Array.isArray(data) ? data : []).map(kb => ({
        id: kb.id,
        type: 'knowledge',
        title: kb.name,
        excerpt: kb.description || '知识库'
      }))
      return
    }

    if (searchType.value === 'userId') {
      if (!/^[0-9]+$/.test(keyword)) {
        showToast({ message: '用户 ID 必须是数字', type: 'warning' })
        return
      }
      router.push(`/user/${keyword}`)
      return
    }

    const user = await authApi.publicUserByUsername(keyword)
    results.value = [{
      id: user.id,
      type: 'member',
      title: user.nickname || user.displayName || user.username,
      excerpt: [user.departmentName, user.positionName, user.username].filter(Boolean).join(' / ')
    }]
  } finally {
    loading.value = false
  }
}

function openResult(item) {
  if (item.type === 'document') router.push(`/editor/${item.kbId || selectedKbId.value}/${item.id}`)
  else if (item.type === 'knowledge') router.push(`/kb/${item.id}`)
  else if (item.type === 'member') router.push(`/user/${item.id}`)
}

function getTypeLabel(type) {
  return { document: '文档', knowledge: '知识库', member: '成员' }[type] || type
}
</script>

<style scoped>
.search-form { margin-bottom: -18px; }
</style>
