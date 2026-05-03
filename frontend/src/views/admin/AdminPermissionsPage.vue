<template>
  <div class="wrap">
    <section class="panel page">
      <h2>权限分配</h2>
      <p class="sub">
        这里同时提供：全局用户角色（ADMIN/USER）与知识库成员角色管理（KB Members）。
      </p>
    </section>

    <section class="panel page">
      <h3>全局角色（用户）</h3>
      <div class="row">
        <input v-model="userId" class="input" placeholder="用户ID（数字）" />
        <select v-model="userRole" class="input select">
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
        <button class="btn btn-primary" @click="applyGlobalRole" :disabled="savingUser">
          {{ savingUser ? '提交中…' : '更新角色' }}
        </button>
      </div>
      <p class="hint">建议在“用户管理”里通过搜索定位用户并编辑；这里适合快速处理已知用户ID。</p>
    </section>

    <section class="panel page">
      <h3>知识库成员权限（KB）</h3>
      <div class="row">
        <input v-model="kbId" class="input" placeholder="知识库ID（kbId）" />
        <button class="btn" @click="loadMembers" :disabled="loadingMembers">{{ loadingMembers ? '加载中…' : '加载成员' }}</button>
      </div>

      <div v-if="members.length" class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>用户ID</th>
              <th>用户名</th>
              <th>当前角色</th>
              <th>设置角色</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in members" :key="m.userId">
              <td class="mono">{{ m.userId }}</td>
              <td>{{ m.username || '-' }}</td>
              <td><span class="tag">{{ m.role }}</span></td>
              <td>
                <select v-model="m._nextRole" class="input select">
                  <option value="READER">READER</option>
                  <option value="EDITOR">EDITOR</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </td>
              <td>
                <button class="btn btn-primary" @click="saveMember(m)" :disabled="m._saving">
                  {{ m._saving ? '保存中…' : '保存' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="hint">输入 kbId 并加载成员后可在此修改成员角色。</div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { adminApi, kbApi } from '../../api/modules'

const userId = ref('')
const userRole = ref('USER')
const savingUser = ref(false)

const kbId = ref('')
const members = ref([])
const loadingMembers = ref(false)

async function applyGlobalRole() {
  const uid = Number(userId.value)
  if (!uid) return
  savingUser.value = true
  try {
    await adminApi.updateUser({ userId: uid, role: userRole.value })
  } finally {
    savingUser.value = false
  }
}

async function loadMembers() {
  const id = Number(kbId.value)
  if (!id) return
  loadingMembers.value = true
  try {
    const res = await kbApi.members(id)
    members.value = (res || []).map(m => ({ ...m, _nextRole: m.role, _saving: false }))
  } finally {
    loadingMembers.value = false
  }
}

async function saveMember(m) {
  if (!kbId.value || !m?.userId) return
  m._saving = true
  try {
    await kbApi.updateMember(Number(kbId.value), { userId: m.userId, role: m._nextRole })
    await loadMembers()
  } finally {
    m._saving = false
  }
}
</script>

<style scoped>
.wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page {
  padding: 16px;
}

.sub {
  margin: 6px 0 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 10px;
}

.select {
  width: auto;
  min-width: 180px;
}

.hint {
  margin: 10px 0 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.table-wrap {
  margin-top: 12px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th, td {
  border-bottom: 1px solid var(--line);
  padding: 10px;
  text-align: left;
  background: var(--panel);
}

th {
  background: var(--line-light);
  font-weight: 700;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}
</style>

