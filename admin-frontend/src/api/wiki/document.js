import request from '@/utils/request'

export function getWikiTree() {
  return request({ url: '/wiki/user/document/tree', method: 'get' })
}

export function getWikiDocument(docId) {
  return request({ url: '/wiki/user/document/' + docId, method: 'get' })
}

export function searchWikiDocuments(keyword) {
  return request({ url: '/wiki/user/document/search', method: 'get', params: { keyword } })
}

export function getLatestWikiDocuments() {
  return request({ url: '/wiki/user/document/latest', method: 'get' })
}

export function getHotWikiDocuments() {
  return request({ url: '/wiki/user/document/hot', method: 'get' })
}

export function addWikiDocument(data) {
  return request({ url: '/wiki/user/document', method: 'post', data })
}

export function updateWikiDocument(data) {
  return request({ url: '/wiki/user/document', method: 'put', data })
}

export function deleteWikiDocument(docId) {
  return request({ url: '/wiki/user/document/' + docId, method: 'delete' })
}

export function getWikiRecycleDocuments() {
  return request({ url: '/wiki/user/document/recycle', method: 'get' })
}

export function restoreWikiDocument(docId) {
  return request({ url: '/wiki/user/document/' + docId + '/restore', method: 'post' })
}

export function getWikiVersions(docId) {
  return request({ url: '/wiki/user/document/' + docId + '/versions', method: 'get' })
}

export function lockWikiDocument(docId) {
  return request({ url: '/wiki/user/document/' + docId + '/lock', method: 'post' })
}

export function unlockWikiDocument(docId) {
  return request({ url: '/wiki/user/document/' + docId + '/lock', method: 'delete' })
}

export function listAdminWikiDocuments(query) {
  return request({ url: '/wiki/admin/document/list', method: 'get', params: query })
}

export function getAdminWikiDocument(docId) {
  return request({ url: '/wiki/admin/document/' + docId, method: 'get' })
}

export function listAdminWikiDocumentOptions(query) {
  return request({ url: '/wiki/admin/document/options', method: 'get', params: query })
}

export function addAdminWikiDocument(data) {
  return request({ url: '/wiki/admin/document', method: 'post', data })
}

export function updateAdminWikiDocument(data) {
  return request({ url: '/wiki/admin/document', method: 'put', data })
}

export function deleteAdminWikiDocument(docId) {
  return request({ url: '/wiki/admin/document/' + docId, method: 'delete' })
}

export function sortAdminWikiDocuments(data) {
  return request({ url: '/wiki/admin/document/sort', method: 'put', data })
}

export function getAdminWikiRecycleDocuments() {
  return request({ url: '/wiki/admin/document/recycle', method: 'get' })
}

export function restoreAdminWikiDocument(docId) {
  return request({ url: '/wiki/admin/document/' + docId + '/restore', method: 'post' })
}

export function lockAdminWikiDocument(docId) {
  return request({ url: '/wiki/admin/document/' + docId + '/lock', method: 'post' })
}

export function unlockAdminWikiDocument(docId) {
  return request({ url: '/wiki/admin/document/' + docId + '/lock', method: 'delete' })
}

export function getAdminWikiVersions(docId) {
  return request({ url: '/wiki/admin/document/' + docId + '/versions', method: 'get' })
}

export function getAdminWikiDiff(docId, leftVersionId, rightVersionId) {
  return request({ url: '/wiki/admin/document/' + docId + '/diff/' + leftVersionId + '/' + rightVersionId, method: 'get' })
}

export function rollbackAdminWikiVersion(docId, versionId) {
  return request({ url: '/wiki/admin/document/' + docId + '/rollback/' + versionId, method: 'post' })
}

export function getWikiDeptOptions() {
  return request({ url: '/wiki/meta/depts', method: 'get' })
}

export function getWikiPostOptions() {
  return request({ url: '/wiki/meta/posts', method: 'get' })
}

export function getWikiStats() {
  return request({ url: '/wiki/meta/stats', method: 'get' })
}
