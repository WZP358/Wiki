<template>
  <div class="settings-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">
        <span class="icon">←</span>
      </button>
      <div class="header-content">
        <h1 class="page-title">研发部知识库 管理</h1>
        <p class="page-subtitle">配置空间权限、成员及基本信息</p>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="stopKnowledgeBase">停用知识库</button>
        <button class="btn-primary" @click="saveSettings">保存更改</button>
      </div>
    </header>

    <!-- 标签页导航 -->
    <div class="tabs-container">
      <nav class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="tab"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- 内容区域 -->
    <main class="settings-content">
      <!-- 成员管理 -->
      <div v-if="activeTab === 'members'" class="tab-content">
        <div class="content-header">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              v-model="memberSearch"
              class="search-input"
              placeholder="搜索成员、邮件..."
            />
          </div>
          <select v-model="roleFilter" class="filter-select">
            <option value="">所有角色</option>
            <option value="admin">管理员</option>
            <option value="editor">编辑者</option>
            <option value="viewer">阅读者</option>
          </select>
          <button class="btn-primary" @click="showAddMember = true">
            <span class="btn-icon">+</span>
            添加成员
          </button>
        </div>

        <!-- 成员列表表格 -->
        <div class="table-container">
          <table class="members-table">
            <thead>
              <tr>
                <th>成员</th>
                <th>角色</th>
                <th>加入日期</th>
                <th>最后活跃</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in filteredMembers" :key="member.id">
                <td>
                  <div class="member-info">
                    <div class="member-avatar">{{ member.name.charAt(0) }}</div>
                    <div class="member-details">
                      <div class="member-name">{{ member.name }}</div>
                      <div class="member-email">{{ member.email }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="role-badge" :class="`role-${member.role}`">
                    {{ getRoleLabel(member.role) }}
                  </span>
                </td>
                <td class="text-secondary">{{ member.joinDate }}</td>
                <td class="text-secondary">{{ member.lastActive }}</td>
                <td>
                  <button class="btn-icon-small" @click="editMember(member)" title="编辑">
                    ⋯
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <span class="pagination-info">显示 1 - 3 / 共 45 名成员</span>
          <div class="pagination-controls">
            <button class="pagination-btn" :disabled="currentPage === 1">‹</button>
            <button
              v-for="page in totalPages"
              :key="page"
              class="pagination-btn"
              :class="{ active: currentPage === page }"
              @click="currentPage = page"
            >
              {{ page }}
            </button>
            <button class="pagination-btn" :disabled="currentPage === totalPages">›</button>
          </div>
        </div>
      </div>

      <!-- 通用设置 -->
      <div v-if="activeTab === 'general'" class="tab-content">
        <div class="settings-section">
          <h3 class="section-title">基本信息</h3>
          <div class="form-group">
            <label class="form-label">知识库名称</label>
            <input v-model="settings.name" class="form-input" placeholder="输入知识库名称" />
          </div>
          <div class="form-group">
            <label class="form-label">描述</label>
            <textarea
              v-model="settings.description"
              class="form-textarea"
              placeholder="简要描述这个知识库的用途..."
              rows="4"
            ></textarea>
          </div>
        </div>

        <div class="settings-section">
          <h3 class="section-title">访问权限</h3>
          <div class="form-group">
            <label class="form-label">可见性</label>
            <select v-model="settings.visibility" class="form-select">
              <option value="PUBLIC">📖 公开 - 所有注册用户可见</option>
              <option value="TEAM">👥 团队 - 仅团队成员可见</option>
              <option value="PRIVATE">🔒 私有 - 仅自己可见</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 权限模板 -->
      <div v-if="activeTab === 'permissions'" class="tab-content">
        <div class="settings-section">
          <h3 class="section-title">权限模板</h3>
          <p class="section-description">定义不同角色的权限范围</p>

          <div class="permission-grid">
            <div v-for="role in roles" :key="role.id" class="permission-card">
              <div class="permission-header">
                <h4 class="permission-title">{{ role.name }}</h4>
                <span class="permission-count">{{ role.memberCount }} 人</span>
              </div>
              <ul class="permission-list">
                <li v-for="perm in role.permissions" :key="perm" class="permission-item">
                  <span class="permission-icon">✓</span>
                  {{ perm }}
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- 安全审计 -->
      <div v-if="activeTab === 'audit'" class="tab-content">
        <div class="settings-section">
          <h3 class="section-title">操作日志</h3>
          <div class="audit-list">
            <div v-for="log in auditLogs" :key="log.id" class="audit-item">
              <div class="audit-icon">📋</div>
              <div class="audit-details">
                <div class="audit-action">{{ log.action }}</div>
                <div class="audit-meta">
                  <span>{{ log.user }}</span>
                  <span class="audit-sep">·</span>
                  <span>{{ log.time }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 添加成员弹窗 -->
    <div v-if="showAddMember" class="modal-overlay" @click.self="showAddMember = false">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3>添加成员</h3>
          <button class="btn-icon" @click="showAddMember = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">邮箱地址</label>
            <input
              v-model="newMember.email"
              class="form-input"
              placeholder="user@company.com"
              type="email"
            />
          </div>
          <div class="form-group">
            <label class="form-label">角色</label>
            <select v-model="newMember.role" class="form-select">
              <option value="viewer">阅读者</option>
              <option value="editor">编辑者</option>
              <option value="admin">管理员</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showAddMember = false">取消</button>
          <button class="btn-primary" @click="addMember">添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('members')
const memberSearch = ref('')
const roleFilter = ref('')
const currentPage = ref(1)
const totalPages = ref(3)
const showAddMember = ref(false)

const tabs = [
  { id: 'members', label: '成员管理' },
  { id: 'general', label: '通用设置' },
  { id: 'permissions', label: '权限模板' },
  { id: 'audit', label: '安全审计' }
]

const members = ref([
  {
    id: 1,
    name: '张小新',
    email: 'zhangxiaoxin@company.com',
    role: 'admin',
    joinDate: '2024-05-12',
    lastActive: '刚刚'
  },
  {
    id: 2,
    name: '李思',
    email: 'lisi_v@company.com',
    role: 'editor',
    joinDate: '2025-01-20',
    lastActive: '2小时前'
  },
  {
    id: 3,
    name: '王武',
    email: 'wangwu@company.com',
    role: 'viewer',
    joinDate: '2026-03-01',
    lastActive: '昨天'
  }
])

const settings = ref({
  name: '研发部知识库',
  description: '包含所有研发相关的技术文档、规范和最佳实践',
  visibility: 'TEAM'
})

const roles = ref([
  {
    id: 'admin',
    name: '管理员',
    memberCount: 5,
    permissions: ['完全控制', '成员管理', '权限配置', '删除知识库']
  },
  {
    id: 'editor',
    name: '编辑者',
    memberCount: 23,
    permissions: ['创建文档', '编辑文档', '删除自己的文档', '评论']
  },
  {
    id: 'viewer',
    name: '阅读者',
    memberCount: 17,
    permissions: ['查看文档', '搜索', '评论']
  }
])

const auditLogs = ref([
  { id: 1, action: '张小新 修改了知识库权限设置', user: '张小新', time: '5分钟前' },
  { id: 2, action: '李思 添加了新成员 王武', user: '李思', time: '1小时前' },
  { id: 3, action: '张小新 创建了文档《2026技术路线》', user: '张小新', time: '3小时前' }
])

const newMember = ref({
  email: '',
  role: 'viewer'
})

const filteredMembers = computed(() => {
  let result = members.value

  if (memberSearch.value) {
    const search = memberSearch.value.toLowerCase()
    result = result.filter(
      m =>
        m.name.toLowerCase().includes(search) || m.email.toLowerCase().includes(search)
    )
  }

  if (roleFilter.value) {
    result = result.filter(m => m.role === roleFilter.value)
  }

  return result
})

function getRoleLabel(role) {
  const labels = {
    admin: '管理员',
    editor: '编辑者',
    viewer: '阅读者'
  }
  return labels[role] || role
}

function editMember(member) {
  console.log('Edit member:', member)
}

function addMember() {
  console.log('Add member:', newMember.value)
  showAddMember.value = false
  newMember.value = { email: '', role: 'viewer' }
}

function saveSettings() {
  console.log('Save settings:', settings.value)
}

function stopKnowledgeBase() {
  if (confirm('确定要停用此知识库吗？')) {
    console.log('Stop knowledge base')
  }
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: var(--bg);
}

/* 页面头部 */
.page-header {
  background: var(--panel);
  border-bottom: 1px solid var(--line);
  padding: 20px 32px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.header-content {
  flex: 1;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 标签页 */
.tabs-container {
  background: var(--panel);
  border-bottom: 1px solid var(--line);
  padding: 0 32px;
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab {
  padding: 12px 20px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab:hover {
  color: var(--text);
}

.tab.active {
  color: var(--brand);
  border-bottom-color: var(--brand);
}

/* 内容区域 */
.settings-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.content-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.search-box {
  flex: 1;
  max-width: 400px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  transition: all 0.2s;
}

.search-box:focus-within {
  border-color: var(--brand);
}

.search-icon {
  font-size: 16px;
  color: var(--muted);
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 14px;
  outline: none;
}

.search-input::placeholder {
  color: var(--muted);
}

.filter-select {
  padding: 8px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.filter-select:hover {
  border-color: var(--brand);
}

.btn-primary,
.btn-secondary {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  outline: none;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background: var(--brand);
  color: white;
}

.btn-primary:hover {
  background: var(--brand-hover);
}

.btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--line);
}

.btn-secondary:hover {
  background: var(--line-light);
  color: var(--text);
  border-color: var(--brand);
}

.btn-icon {
  font-size: 18px;
}

/* 表格 */
.table-container {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.members-table {
  width: 100%;
  border-collapse: collapse;
}

.members-table thead {
  background: var(--line-light);
}

.members-table th {
  padding: 14px 20px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--line);
}

.members-table td {
  padding: 16px 20px;
  font-size: 14px;
  color: var(--text);
  border-bottom: 1px solid var(--line);
}

.members-table tbody tr:last-child td {
  border-bottom: none;
}

.members-table tbody tr:hover {
  background: var(--line-light);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.member-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.member-name {
  font-weight: 500;
  color: var(--text);
}

.member-email {
  font-size: 13px;
  color: var(--text-secondary);
}

.role-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.role-badge.role-admin {
  background: #FFF3E0;
  color: #F57C00;
}

.role-badge.role-editor {
  background: #E8F0FE;
  color: #1967D2;
}

.role-badge.role-viewer {
  background: #F3E5F5;
  color: #8E24AA;
}

.text-secondary {
  color: var(--text-secondary);
}

.btn-icon-small {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 16px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon-small:hover {
  background: var(--line-light);
  color: var(--text);
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
}

.pagination-info {
  font-size: 13px;
  color: var(--text-secondary);
}

.pagination-controls {
  display: flex;
  gap: 6px;
}

.pagination-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--line);
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  border-color: var(--brand);
  color: var(--brand);
}

.pagination-btn.active {
  background: var(--brand);
  border-color: var(--brand);
  color: white;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 设置表单 */
.settings-section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
  margin: 0 0 16px 0;
}

.section-description {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 20px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 8px;
}

.form-input,
.form-textarea,
.form-select {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--panel);
  color: var(--text);
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.form-input:hover,
.form-textarea:hover,
.form-select:hover {
  border-color: var(--brand);
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.form-textarea {
  resize: vertical;
  font-family: inherit;
}

/* 权限卡片 */
.permission-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.permission-card {
  background: var(--line-light);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 20px;
}

.permission-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.permission-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.permission-count {
  font-size: 13px;
  color: var(--text-secondary);
}

.permission-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text);
  margin-bottom: 8px;
}

.permission-item:last-child {
  margin-bottom: 0;
}

.permission-icon {
  color: var(--success);
  font-size: 14px;
}

/* 审计日志 */
.audit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--line-light);
  border-radius: 8px;
}

.audit-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.audit-details {
  flex: 1;
}

.audit-action {
  font-size: 14px;
  color: var(--text);
  margin-bottom: 4px;
}

.audit-meta {
  font-size: 13px;
  color: var(--text-secondary);
}

.audit-sep {
  margin: 0 6px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.modal-dialog {
  background: var(--panel);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  width: 90%;
  max-width: 500px;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--line);
}

@media (max-width: 900px) {
  .page-header {
    flex-wrap: wrap;
  }

  .header-content {
    width: 100%;
    order: -1;
  }

  .permission-grid {
    grid-template-columns: 1fr;
  }

  .content-header {
    flex-wrap: wrap;
  }

  .search-box {
    max-width: 100%;
  }
}
</style>
