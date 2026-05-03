import request from '@/utils/request'

export function listKb(query) {
  return request({
    url: '/wiki/admin/kb/list',
    method: 'get',
    params: query
  })
}

export function getKb(kbId) {
  return request({
    url: '/wiki/admin/kb/' + kbId,
    method: 'get'
  })
}

export function getKbPermission(kbId) {
  return request({
    url: '/wiki/admin/kb/' + kbId + '/permission',
    method: 'get'
  })
}

export function addKb(data) {
  return request({
    url: '/wiki/admin/kb',
    method: 'post',
    data
  })
}

export function updateKb(data) {
  return request({
    url: '/wiki/admin/kb',
    method: 'put',
    data
  })
}

export function transferKbOwner(kbId, targetUserId) {
  return request({
    url: '/wiki/admin/kb/' + kbId + '/transfer-owner',
    method: 'post',
    data: { targetUserId }
  })
}

export function delKb(kbIds) {
  return request({
    url: '/wiki/admin/kb/' + kbIds,
    method: 'delete'
  })
}

export function listKbMember(query) {
  return request({
    url: '/wiki/admin/kb/member/list',
    method: 'get',
    params: query
  })
}

export function getKbMember(memberId) {
  return request({
    url: '/wiki/admin/kb/member/' + memberId,
    method: 'get'
  })
}

export function addKbMember(data) {
  return request({
    url: '/wiki/admin/kb/member',
    method: 'post',
    data
  })
}

export function updateKbMember(data) {
  return request({
    url: '/wiki/admin/kb/member',
    method: 'put',
    data
  })
}

export function delKbMember(memberIds) {
  return request({
    url: '/wiki/admin/kb/member/' + memberIds,
    method: 'delete'
  })
}
