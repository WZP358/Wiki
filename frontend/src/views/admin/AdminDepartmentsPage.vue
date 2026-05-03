<template>
  <section class="panel page">
    <div class="toolbar">
      <div class="left">
        <button class="btn btn-primary" @click="openCreate">新建团队</button>
        <button class="btn" @click="load">刷新</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>上级团队</th>
            <th>负责人(managerId)</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in depts" :key="d.id">
            <td class="mono">{{ d.id }}</td>
            <td>{{ d.name }}</td>
            <td class="mono">{{ d.parentId || '-' }}</td>
            <td class="mono">{{ d.managerId || '-' }}</td>
            <td>
              <span :class="['pill', d.active ? 'ok' : 'bad']">{{ d.active ? '启用' : '禁用' }}</span>
            </td>
            <td class="mono">{{ fmt(d.updatedAt) }}</td>
            <td class="ops">
              <button class="btn" @click="openEdit(d)">编辑</button>
              <button class="btn" @click="toggleActive(d)">{{ d.active ? '禁用' : '启用' }}</button>
            </td>
          </tr>
          <tr v-if="depts.length === 0">
            <td colspan="7" class="empty">暂无团队</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="modalVisible" class="modal-mask" @click="closeModal">
      <div class="modal panel" @click.stop>
        <div class="modal-title">{{ editing ? '编辑团队' : '新建团队' }}</div>
        <div class="form">
          <div class="row">
            <div class="label">名称</div>
            <input v-model="form.name" class="input" placeholder="团队名称" />
          </div>
          <div class="row">
            <div class="label">上级团队</div>
            <select v-model="form.parentId" class="input select">
              <option value="">无</option>
              <option v-for="d in depts" :key="d.id" :value="String(d.id)">{{ d.name }} ({{ d.id }})</option>
            </select>
          </div>
          <div class="row">
            <div class="label">负责人</div>
            <input v-model="form.managerId" class="input" placeholder="managerId（用户ID，可空）" />
          </div>
          <div class="row">
            <div class="label">描述</div>
            <input v-model="form.description" class="input" placeholder="描述（可空）" />
          </div>
        </div>
        <div class="actions">
          <button class="btn" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="save" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '../../api/modules'

const depts = ref([])
const modalVisible = ref(false)
const editing = ref(null)
const saving = ref(false)

const form = ref({
  name: '',
  parentId: '',
  managerId: '',
  description: ''
})

onMounted(load)

function fmt(v) {
  if (!v) return '-'
  try {
    return new Date(v).toLocaleString('zh-CN')
  } catch {
    return String(v)
  }
}

async function load() {
  depts.value = await adminApi.adminDepts()
}

function openCreate() {
  editing.value = null
  form.value = { name: '', parentId: '', managerId: '', description: '' }
  modalVisible.value = true
}

function openEdit(d) {
  editing.value = d
  form.value = {
    name: d.name || '',
    parentId: d.parentId ? String(d.parentId) : '',
    managerId: d.managerId ? String(d.managerId) : '',
    description: d.description || ''
  }
  modalVisible.value = true
}

function closeModal() {
  modalVisible.value = false
  editing.value = null
}

async function save() {
  const payload = {
    name: String(form.value.name || '').trim(),
    parentId: form.value.parentId ? Number(form.value.parentId) : null,
    managerId: form.value.managerId ? Number(form.value.managerId) : null,
    description: String(form.value.description || '').trim() || null
  }
  if (!payload.name) return
  saving.value = true
  try {
    if (editing.value?.id) {
      await adminApi.updateDept(editing.value.id, payload)
    } else {
      await adminApi.createDept(payload)
    }
    await load()
    closeModal()
  } finally {
    saving.value = false
  }
}

async function toggleActive(d) {
  await adminApi.setDeptActive(d.id, !d.active)
  await load()
}
</script>

<style scoped>
.page { padding: 16px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 12px; }
.left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.table-wrap { overflow: auto; border: 1px solid var(--line); border-radius: 12px; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th, td { border-bottom: 1px solid var(--line); padding: 10px; text-align: left; background: var(--panel); }
th { background: var(--line-light); font-weight: 700; position: sticky; top: 0; z-index: 1; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; font-size: 12px; }
.pill { display: inline-flex; padding: 2px 8px; border-radius: 999px; font-size: 12px; border: 1px solid var(--line); }
.pill.ok { background: var(--brand-soft); color: var(--brand); border-color: var(--brand-light); }
.pill.bad { background: rgba(239, 68, 68, 0.08); color: var(--danger); border-color: rgba(239, 68, 68, 0.25); }
.ops { display: flex; gap: 8px; flex-wrap: wrap; }
.empty { text-align: center; padding: 18px; color: var(--text-secondary); }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: grid; place-items: center; padding: 18px; z-index: 999; }
.modal { width: min(560px, 100%); padding: 14px; }
.modal-title { font-weight: 800; margin-bottom: 10px; }
.form { display: flex; flex-direction: column; gap: 10px; }
.row { display: grid; grid-template-columns: 70px 1fr; gap: 10px; align-items: center; }
.label { color: var(--text-secondary); font-size: 13px; }
.select { width: auto; }
.actions { margin-top: 12px; display: flex; justify-content: flex-end; gap: 10px; }
</style>

