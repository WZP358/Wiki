<template>
  <section class="panel page">
    <div class="toolbar">
      <div class="left">
        <input v-model="keyword" class="input" placeholder="搜索：用户名/昵称/邮箱/手机号" @keyup.enter="reload" />
        <select v-model="role" class="input select">
          <option value="">全部角色</option>
          <option value="ADMIN">ADMIN</option>
          <option value="USER">USER</option>
        </select>
        <select v-model="active" class="input select">
          <option value="">全部状态</option>
          <option value="true">启用</option>
          <option value="false">禁用</option>
        </select>
        <select v-model="departmentId" class="input select">
          <option value="">全部团队</option>
          <option v-for="d in depts" :key="d.id" :value="String(d.id)">{{ d.name }}</option>
        </select>
      </div>
      <div class="right">
        <button class="btn" @click="reload">查询</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>团队</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td class="mono">{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.nickname || '-' }}</td>
            <td><span class="tag">{{ u.role }}</span></td>
            <td>{{ deptName(u.departmentId) }}</td>
            <td>
              <span :class="['pill', u.active ? 'ok' : 'bad']">{{ u.active ? '启用' : '禁用' }}</span>
            </td>
            <td class="mono">{{ fmt(u.updatedAt) }}</td>
            <td>
              <button class="btn" @click="openEdit(u)">编辑</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <button class="btn" :disabled="page <= 0" @click="prev">上一页</button>
      <div class="meta">第 {{ page + 1 }} 页 / 共 {{ totalPages }} 页（{{ total }} 条）</div>
      <button class="btn" :disabled="page + 1 >= totalPages" @click="next">下一页</button>
    </div>

    <div v-if="editVisible" class="modal-mask" @click="closeEdit">
      <div class="modal panel" @click.stop>
        <div class="modal-title">编辑用户</div>
        <div class="form">
          <div class="row">
            <div class="label">用户</div>
            <div class="value mono">{{ editing?.id }} / {{ editing?.username }}</div>
          </div>
          <div class="row">
            <div class="label">角色</div>
            <select v-model="editRole" class="input select">
              <option value="ADMIN">ADMIN</option>
              <option value="USER">USER</option>
            </select>
          </div>
          <div class="row">
            <div class="label">团队</div>
            <select v-model="editDeptId" class="input select">
              <option value="">未分配</option>
              <option v-for="d in depts" :key="d.id" :value="String(d.id)">{{ d.name }}</option>
            </select>
          </div>
          <div class="row">
            <div class="label">状态</div>
            <select v-model="editActive" class="input select">
              <option value="true">启用</option>
              <option value="false">禁用</option>
            </select>
          </div>
        </div>
        <div class="actions">
          <button class="btn" @click="closeEdit">取消</button>
          <button class="btn btn-primary" @click="saveEdit" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi, deptApi } from '../../api/modules'

const keyword = ref('')
const role = ref('')
const active = ref('')
const departmentId = ref('')

const depts = ref([])

const users = ref([])
const page = ref(0)
const size = ref(20)
const totalPages = ref(1)
const total = ref(0)

const editVisible = ref(false)
const editing = ref(null)
const editRole = ref('USER')
const editDeptId = ref('')
const editActive = ref('true')
const saving = ref(false)

onMounted(async () => {
  depts.value = await deptApi.list()
  await reload()
})

function deptName(id) {
  if (!id) return '-'
  return depts.value.find(d => String(d.id) === String(id))?.name || String(id)
}

function fmt(v) {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString('zh-CN')
  } catch {
    return String(v)
  }
}

async function reload() {
  const params = {
    page: page.value,
    size: size.value
  }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  if (role.value) params.role = role.value
  if (departmentId.value) params.departmentId = Number(departmentId.value)
  if (active.value) params.active = active.value === 'true'

  const res = await adminApi.users(params)
  users.value = res.content || []
  totalPages.value = res.totalPages ?? 1
  total.value = res.totalElements ?? users.value.length
}

async function prev() {
  if (page.value <= 0) return
  page.value -= 1
  await reload()
}

async function next() {
  if (page.value + 1 >= totalPages.value) return
  page.value += 1
  await reload()
}

function openEdit(u) {
  editing.value = u
  editRole.value = u.role || 'USER'
  editDeptId.value = u.departmentId ? String(u.departmentId) : ''
  editActive.value = u.active ? 'true' : 'false'
  editVisible.value = true
}

function closeEdit() {
  editVisible.value = false
  editing.value = null
}

async function saveEdit() {
  if (!editing.value?.id) return
  saving.value = true
  try {
    await adminApi.updateUser({
      userId: editing.value.id,
      role: editRole.value,
      departmentId: editDeptId.value ? Number(editDeptId.value) : null,
      active: editActive.value === 'true'
    })
    await reload()
    closeEdit()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  flex: 1;
}

.select {
  width: auto;
  min-width: 140px;
}

.table-wrap {
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
  padding: 10px 10px;
  text-align: left;
  vertical-align: middle;
  background: var(--panel);
}

th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--line-light);
  font-weight: 700;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}

.pill {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid var(--line);
}

.pill.ok {
  background: var(--brand-soft);
  color: var(--brand);
  border-color: var(--brand-light);
}

.pill.bad {
  background: rgba(239, 68, 68, 0.08);
  color: var(--danger);
  border-color: rgba(239, 68, 68, 0.25);
}

.empty {
  text-align: center;
  padding: 22px;
  color: var(--text-secondary);
}

.pager {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta {
  color: var(--text-secondary);
  font-size: 13px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: grid;
  place-items: center;
  padding: 18px;
  z-index: 999;
}

.modal {
  width: min(520px, 100%);
  padding: 14px;
}

.modal-title {
  font-weight: 800;
  margin-bottom: 10px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.row {
  display: grid;
  grid-template-columns: 70px 1fr;
  gap: 10px;
  align-items: center;
}

.label {
  color: var(--text-secondary);
  font-size: 13px;
}

.value {
  font-size: 13px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

