import http, { buildUrl } from './http'

export const authApi = {
  sendCode: data => http.post('/auth/send-code', data),
  sendUpdateCode: data => http.post('/auth/send-update-code', data),
  uploadAvatar: file => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post('/auth/upload-avatar', formData)
  },
  register: data => http.post('/auth/register', data),
  login: data => http.post('/auth/login', data),
  me: () => http.get('/auth/me'),
  updateProfile: data => http.put('/auth/profile', data),
  publicUserById: userId => http.get(`/auth/public-user/by-id/${userId}`),
  publicUserByUsername: username => http.get('/auth/public-user/by-username', { params: { username } })
}

export const kbApi = {
  mine: () => http.get('/kbs/mine'),
  get: kbId => http.get(`/kbs/${kbId}`),
  create: data => http.post('/kbs', data),
  update: (kbId, data) => http.put(`/kbs/${kbId}`, data),
  remove: kbId => http.delete(`/kbs/${kbId}`),
  members: kbId => http.get(`/kbs/${kbId}/members`),
  children: kbId => http.get(`/kbs/${kbId}/children`),
  updateMember: (kbId, data) => http.post(`/kbs/${kbId}/members`, data),
  publicByUser: userId => http.get(`/kbs/user/${userId}/public`),
  search: keyword => http.get('/kbs/search', { params: { keyword } }),
  byDepartment: deptId => http.get('/kbs/by-department', { params: { deptId } })
}

export const deptApi = {
  list: () => http.get('/departments')
}

export const docApi = {
  create: data => http.post('/docs', data),
  uploadImage: file => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post('/docs/images', formData)
  },
  tree: kbId => http.get('/docs/tree', { params: { kbId } }),
  detail: docId => http.get(`/docs/${docId}`),
  update: (docId, data) => http.put(`/docs/${docId}`, data),
  delete: docId => http.delete(`/docs/${docId}`),
  versions: docId => http.get(`/docs/${docId}/versions`),
  diffVersions: (docId, leftVersionId, rightVersionId) =>
    http.get(`/docs/${docId}/versions/diff`, { params: { leftVersionId, rightVersionId } }),
  rollback: (docId, versionId) => http.post(`/docs/${docId}/rollback/${versionId}`),
  search: (kbId, keyword) => http.get('/docs/search', { params: { kbId, keyword } }),
  latest: kbId => http.get('/docs/latest', { params: { kbId } }),
  hot: kbId => http.get('/docs/hot', { params: { kbId } }),
  lock: docId => http.post(`/docs/${docId}/lock`),
  unlock: docId => http.delete(`/docs/${docId}/lock`),
  saveDraft: (docId, data) => http.post(`/docs/${docId}/draft`, data),
  getDraft: docId => http.get(`/docs/${docId}/draft`)
}

export const adminApi = {
  logs: (page = 0, size = 20, extraParams = {}) => http.get('/admin/logs', { params: { page, size, ...(extraParams || {}) } })
  ,
  users: (params = {}) => http.get('/admin/users', { params }),
  userById: userId => http.get(`/admin/users/${userId}`),
  updateUser: data => http.post('/admin/users/update', data),
  auditDocs: (params = {}) => http.get('/admin/audit/docs', { params }),
  auditDocViewLogs: (params = {}) => http.get('/admin/audit/doc-view-logs', { params }),
  auditDocEditLogs: (params = {}) => http.get('/admin/audit/doc-edit-logs', { params }),
  // Departments
  adminDepts: (params = {}) => http.get('/admin/departments', { params }),
  createDept: data => http.post('/admin/departments', data),
  updateDept: (deptId, data) => http.put(`/admin/departments/${deptId}`, data),
  setDeptActive: (deptId, active) => http.post(`/admin/departments/${deptId}/active`, null, { params: { active } }),
  deleteDept: deptId => http.delete(`/admin/departments/${deptId}`),
  // Knowledge bases
  kbs: (params = {}) => http.get('/admin/kbs', { params }),
  kbById: kbId => http.get(`/admin/kbs/${kbId}`),
  kbAction: data => http.post('/admin/kbs/action', data),
  // Doc moderation
  docAction: data => http.post('/admin/docs/action', data),
  // Export (CSV download endpoints)
  exportUsersUrl: (params = {}) => buildUrl('/admin/export/users.csv', params),
  exportDocsUrl: (params = {}) => buildUrl('/admin/export/docs.csv', params),
  exportOperationLogsUrl: (params = {}) => buildUrl('/admin/export/operation-logs.csv', params),
  exportDocViewLogsUrl: (params = {}) => buildUrl('/admin/export/doc-view-logs.csv', params),
  exportDocEditLogsUrl: (params = {}) => buildUrl('/admin/export/doc-edit-logs.csv', params),
  // Stats
  statsOverview: () => http.get('/admin/stats/overview')
}

export const templateApi = {
  list: kbId => http.get('/templates', { params: kbId ? { kbId } : {} }),
  get: templateId => http.get(`/templates/${templateId}`),
  create: data => http.post('/templates', data),
  update: (templateId, data) => http.put(`/templates/${templateId}`, data),
  delete: templateId => http.delete(`/templates/${templateId}`),
  listByCategory: (category, kbId) => http.get(`/templates/category/${category}`, { params: kbId ? { kbId } : {} }),
  incrementUse: templateId => http.post(`/templates/${templateId}/use`)
}

export const commentApi = {
  list: docId => http.get(`/docs/${docId}/comments`),
  create: (docId, data) => http.post(`/docs/${docId}/comments`, data),
  update: (docId, commentId, data) => http.put(`/docs/${docId}/comments/${commentId}`, data),
  delete: (docId, commentId) => http.delete(`/docs/${docId}/comments/${commentId}`),
  resolve: (docId, commentId) => http.post(`/docs/${docId}/comments/${commentId}/resolve`),
  count: docId => http.get(`/docs/${docId}/comments/count`)
}

export const reactionApi = {
  toggle: (docId, reactionType) => http.post(`/docs/${docId}/reactions/${reactionType}`),
  getStats: docId => http.get(`/docs/${docId}/reactions`)
}
