import request from '@/utils/request'

function pageParams(query) {
  const pageNum = Number(query.pageNum || 1)
  const pageSize = Number(query.pageSize || 10)
  return {
    ...query,
    page: Math.max(pageNum - 1, 0),
    size: pageSize
  }
}

function toTablePage(page) {
  return {
    rows: page.content || [],
    total: Number(page.totalElements || 0)
  }
}

export function normalizePage(page) {
  return toTablePage(page || {})
}

export function getOverview() {
  return request({
    url: '/admin/stats/overview',
    method: 'get'
  })
}

export function listUsers(query) {
  return request({
    url: '/admin/users',
    method: 'get',
    params: pageParams(query)
  }).then(normalizePage)
}

export function listPendingUsers(query) {
  return request({
    url: '/admin/users/pending',
    method: 'get',
    params: pageParams(query)
  }).then(normalizePage)
}

export function getPendingUserCount() {
  return request({
    url: '/admin/users/pending/count',
    method: 'get'
  })
}

export function updateUser(data) {
  return request({
    url: '/admin/users/update',
    method: 'post',
    data
  })
}

export function assignPendingUser(data) {
  return request({
    url: '/admin/users/assign-pending',
    method: 'post',
    data
  })
}

export function listDepartments(query) {
  return request({
    url: '/admin/departments',
    method: 'get',
    params: query
  }).then(data => ({
    rows: data || [],
    total: Number((data || []).length)
  }))
}

export function addDepartment(data) {
  return request({
    url: '/admin/departments',
    method: 'post',
    data
  })
}

export function updateDepartment(deptId, data) {
  return request({
    url: `/admin/departments/${deptId}`,
    method: 'put',
    data
  })
}

export function setDepartmentActive(deptId, active) {
  return request({
    url: `/admin/departments/${deptId}/active`,
    method: 'post',
    params: { active }
  })
}

export function deleteDepartment(deptId) {
  return request({
    url: `/admin/departments/${deptId}`,
    method: 'delete'
  })
}

export function listKbs(query) {
  return request({
    url: '/admin/kbs',
    method: 'get',
    params: pageParams(query)
  }).then(normalizePage)
}

export function kbAction(data) {
  return request({
    url: '/admin/kbs/action',
    method: 'post',
    data
  })
}

export function listDocs(query) {
  return request({
    url: '/admin/audit/docs',
    method: 'get',
    params: pageParams(query)
  }).then(normalizePage)
}

export function docAction(data) {
  return request({
    url: '/admin/docs/action',
    method: 'post',
    data
  })
}

export function listLogs(query) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params: pageParams(query)
  }).then(normalizePage)
}
