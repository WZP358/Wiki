<template>
  <main class="app-container ry-page">
    <el-card shadow="never">
      <template #header>
        <div class="ry-card-header">
          <div>
            <span>{{ form.name || '知识库设置' }}</span>
            <div class="ry-muted">维护知识库资料，邀请普通用户加入协作名单。</div>
          </div>
          <div class="header-actions">
            <el-button @click="router.back()">返回</el-button>
            <el-button type="danger" plain :disabled="!kbId || saving" @click="disableKnowledgeBase">停用</el-button>
            <el-button type="primary" :loading="saving" :disabled="!kbId" @click="saveSettings">保存</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!kbId" description="请先选择一个知识库" />

      <el-row v-else :gutter="16">
        <el-col :xs="24" :md="8">
          <el-card shadow="never">
            <template #header>基本信息</template>
            <el-form :model="form" label-width="92px">
              <el-form-item label="名称">
                <el-input v-model="form.name" maxlength="128" show-word-limit />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="4" maxlength="512" show-word-limit />
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="form.type" style="width: 100%">
                  <el-option label="公开知识库" value="COMPANY" />
                  <el-option label="团队知识库" value="DEPARTMENT" />
                  <el-option label="私有知识库" value="PRIVATE" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.type === 'DEPARTMENT'" label="所属团队">
                <el-select v-model="form.teamId" filterable placeholder="请选择团队" style="width: 100%">
                  <el-option v-for="team in teams" :key="team.id" :label="team.name" :value="team.id" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="16">
          <el-card shadow="never">
            <template #header>
              <div class="ry-card-header">
                <span>成员权限</span>
                <el-button type="primary" plain @click="showAddMember = true">邀请成员</el-button>
              </div>
            </template>

            <el-alert
              class="member-alert"
              title="公开或团队可见只代表可阅读；只有加入名单且拥有编辑或管理权限的成员才能创建或修改文档。每个知识库至少保留一名管理员。"
              type="info"
              :closable="false"
              show-icon
            />

            <el-table v-loading="membersLoading" :data="members" size="small">
              <el-table-column prop="departmentName" label="团队" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ row.departmentName || '未分配' }}</template>
              </el-table-column>
              <el-table-column prop="positionName" label="职位" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">{{ row.positionName || '-' }}</template>
              </el-table-column>
              <el-table-column label="姓名" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                  <strong>{{ row.displayName || row.nickname || row.username || row.userId }}</strong>
                  <div class="ry-muted">{{ row.username || '-' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="权限" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="roleTagType(row.role)" effect="plain">{{ roleLabel(row.role) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="调整" width="150">
                <template #default="{ row }">
                  <el-select :model-value="row.role" size="small" @change="role => updateMemberRole(row, role)">
                    <el-option label="查看" value="READER" />
                    <el-option label="编辑" value="EDITOR" />
                    <el-option label="管理" value="ADMIN" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="showAddMember" title="邀请成员" width="480px">
      <el-form :model="newMember" label-width="90px">
        <el-form-item label="账号或邮箱">
          <el-input v-model="newMember.usernameOrEmail" placeholder="请输入普通用户账号或邮箱" clearable />
        </el-form-item>
        <el-form-item label="权限">
          <el-select v-model="newMember.role" style="width: 100%">
            <el-option label="查看" value="READER" />
            <el-option label="编辑" value="EDITOR" />
            <el-option label="管理" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAddMember">取消</el-button>
        <el-button type="primary" @click="addMember">邀请</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { kbApi, deptApi } from '../api/modules'
import { showToast } from '../utils/errorBus'

const route = useRoute()
const router = useRouter()
const kbId = computed(() => route.params.kbId || route.query.kbId || '')
const kb = ref(null)
const teams = ref([])
const members = ref([])
const saving = ref(false)
const membersLoading = ref(false)
const showAddMember = ref(false)
const form = reactive({ name: '', description: '', type: 'COMPANY', parentId: null, teamId: null })
const newMember = reactive({ usernameOrEmail: '', role: 'READER' })

onMounted(load)
watch(kbId, load)

async function load() {
  await loadTeams()
  if (!kbId.value) return
  const currentKb = await kbApi.get(kbId.value)
  kb.value = currentKb
  form.name = currentKb.name || ''
  form.description = currentKb.description || ''
  form.type = normalizeKbType(currentKb.type)
  form.parentId = currentKb.parentId || null
  form.teamId = currentKb.teamId || null
  await loadMembers()
}

async function loadTeams() {
  try {
    const data = await deptApi.list()
    teams.value = Array.isArray(data) ? data : []
  } catch {
    teams.value = []
  }
}

async function loadMembers() {
  if (!kbId.value) return
  membersLoading.value = true
  try {
    const result = await kbApi.members(kbId.value)
    members.value = Array.isArray(result) ? result : []
  } finally {
    membersLoading.value = false
  }
}

function normalizeKbType(type) {
  return type === 'PUBLIC' ? 'COMPANY' : (type || 'COMPANY')
}

function roleLabel(role) {
  return { READER: '查看', EDITOR: '编辑', ADMIN: '管理' }[role] || role
}

function roleTagType(role) {
  return { ADMIN: 'warning', EDITOR: 'success', READER: 'info' }[role] || 'info'
}

async function saveSettings() {
  if (!form.name) {
    showToast({ title: '缺少名称', message: '知识库名称不能为空。', type: 'warning' })
    return
  }
  if (form.type === 'DEPARTMENT' && !form.teamId) {
    showToast({ title: '缺少团队', message: '团队知识库必须选择所属团队。', type: 'warning' })
    return
  }
  saving.value = true
  try {
    const updated = await kbApi.update(kbId.value, {
      name: form.name,
      description: form.description,
      type: form.type,
      parentId: form.parentId,
      teamId: form.type === 'DEPARTMENT' ? form.teamId : null
    })
    kb.value = updated
    form.type = normalizeKbType(updated.type)
    form.teamId = updated.teamId || null
    showToast({ title: '已保存', message: '知识库设置已更新。', type: 'success' })
  } finally {
    saving.value = false
  }
}

async function updateMemberRole(member, role) {
  await kbApi.updateMember(kbId.value, { userId: member.userId, role })
  await loadMembers()
  showToast({ title: '已更新', message: '成员权限已调整。', type: 'success' })
}

async function addMember() {
  if (!newMember.usernameOrEmail) {
    showToast({ title: '缺少成员', message: '请输入账号或邮箱。', type: 'warning' })
    return
  }
  await kbApi.updateMember(kbId.value, {
    usernameOrEmail: newMember.usernameOrEmail,
    role: newMember.role
  })
  closeAddMember()
  await loadMembers()
  showToast({ title: '已邀请', message: '成员已加入知识库。', type: 'success' })
}

function closeAddMember() {
  showAddMember.value = false
  newMember.usernameOrEmail = ''
  newMember.role = 'READER'
}

async function disableKnowledgeBase() {
  await ElMessageBox.confirm('停用后，该知识库不会出现在普通列表中；系统管理员仍可在后台查看和恢复。', '停用知识库', {
    confirmButtonText: '停用',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => Promise.reject(new Error('cancel')))
  await kbApi.remove(kbId.value)
  showToast({ title: '已停用', message: '知识库已停用。', type: 'success' })
  await router.push('/')
}
</script>

<style scoped>
.header-actions { display: flex; align-items: center; gap: 8px; }
.member-alert { margin-bottom: 12px; }
@media (max-width: 760px) { .header-actions { flex-wrap: wrap; } }
</style>
