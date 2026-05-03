import request from "@/utils/request"

export function listTemplate(query) {
  return request({
    url: "/wiki/admin/template/list",
    method: "get",
    params: query
  })
}

export function getTemplate(templateId) {
  return request({
    url: "/wiki/admin/template/" + templateId,
    method: "get"
  })
}

export function addTemplate(data) {
  return request({
    url: "/wiki/admin/template",
    method: "post",
    data
  })
}

export function updateTemplate(data) {
  return request({
    url: "/wiki/admin/template",
    method: "put",
    data
  })
}

export function delTemplate(templateIds) {
  return request({
    url: "/wiki/admin/template/" + templateIds,
    method: "delete"
  })
}
