import http from './http'

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
  updateProfile: data => http.put('/auth/profile', data)
}

export const kbApi = {
  mine: () => http.get('/kbs/mine'),
  create: data => http.post('/kbs', data),
  members: kbId => http.get(`/kbs/${kbId}/members`),
  updateMember: (kbId, data) => http.post(`/kbs/${kbId}/members`, data),
  publicByUser: userId => http.get(`/kbs/user/${userId}/public`)
}

export const docApi = {
  create: data => http.post('/docs', data),
  tree: kbId => http.get('/docs/tree', { params: { kbId } }),
  detail: docId => http.get(`/docs/${docId}`),
  update: (docId, data) => http.put(`/docs/${docId}`, data),
  delete: docId => http.delete(`/docs/${docId}`),
  recycle: () => http.get('/docs/recycle'),
  restore: docId => http.post(`/docs/${docId}/restore`),
  purge: (docId, confirmed) => http.delete(`/docs/${docId}/purge`, { params: { confirmed } }),
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

export const shareApi = {
  create: docId => http.post(`/shares/docs/${docId}`),
  view: token => http.get(`/shares/public/${token}`)
}

export const adminApi = {
  logs: (page = 0, size = 20) => http.get('/admin/logs', { params: { page, size } })
}

export const favoriteApi = {
  add: docId => http.post(`/favorites/docs/${docId}`),
  remove: docId => http.delete(`/favorites/docs/${docId}`),
  mine: kbId => http.get('/favorites/mine', { params: kbId ? { kbId } : {} }),
  check: docId => http.get(`/favorites/check/${docId}`)
}
