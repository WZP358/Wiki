import request from "@/utils/request"

export function listShare(query) {
  return request({
    url: "/wiki/admin/share/list",
    method: "get",
    params: query
  })
}

export function getShare(shareId) {
  return request({
    url: "/wiki/admin/share/" + shareId,
    method: "get"
  })
}

export function addShare(data) {
  return request({
    url: "/wiki/admin/share",
    method: "post",
    data
  })
}

export function updateShare(data) {
  return request({
    url: "/wiki/admin/share",
    method: "put",
    data
  })
}

export function delShare(shareIds) {
  return request({
    url: "/wiki/admin/share/" + shareIds,
    method: "delete"
  })
}

export function openShare(shareCode) {
  return request({
    url: "/wiki/share/open/" + shareCode,
    method: "get",
    headers: { isToken: false }
  })
}
