import request from "@/utils/request"

export function listGroup(query) {
  return request({
    url: "/wiki/admin/group/list",
    method: "get",
    params: query
  })
}

export function getGroup(groupId) {
  return request({
    url: "/wiki/admin/group/" + groupId,
    method: "get"
  })
}

export function addGroup(data) {
  return request({
    url: "/wiki/admin/group",
    method: "post",
    data
  })
}

export function updateGroup(data) {
  return request({
    url: "/wiki/admin/group",
    method: "put",
    data
  })
}

export function delGroup(groupId) {
  return request({
    url: "/wiki/admin/group/" + groupId,
    method: "delete"
  })
}

export function listGroupOptions(kbId) {
  return request({
    url: "/wiki/admin/group/options",
    method: "get",
    params: { kbId }
  })
}
