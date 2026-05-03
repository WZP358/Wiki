import request from "@/utils/request"

export function listTag(query) {
  return request({
    url: "/wiki/admin/tag/list",
    method: "get",
    params: query
  })
}

export function getTag(tagId) {
  return request({
    url: "/wiki/admin/tag/" + tagId,
    method: "get"
  })
}

export function addTag(data) {
  return request({
    url: "/wiki/admin/tag",
    method: "post",
    data
  })
}

export function updateTag(data) {
  return request({
    url: "/wiki/admin/tag",
    method: "put",
    data
  })
}

export function delTag(tagIds) {
  return request({
    url: "/wiki/admin/tag/" + tagIds,
    method: "delete"
  })
}

export function listTagOptions(kbId) {
  return request({
    url: "/wiki/admin/tag/options",
    method: "get",
    params: { kbId }
  })
}
