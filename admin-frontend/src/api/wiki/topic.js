import request from "@/utils/request"

export function listTopic(query) {
  return request({
    url: "/wiki/admin/topic/list",
    method: "get",
    params: query
  })
}

export function getTopic(topicId) {
  return request({
    url: "/wiki/admin/topic/" + topicId,
    method: "get"
  })
}

export function addTopic(data) {
  return request({
    url: "/wiki/admin/topic",
    method: "post",
    data
  })
}

export function updateTopic(data) {
  return request({
    url: "/wiki/admin/topic",
    method: "put",
    data
  })
}

export function delTopic(topicIds) {
  return request({
    url: "/wiki/admin/topic/" + topicIds,
    method: "delete"
  })
}

export function listTopicVisible() {
  return request({
    url: "/wiki/user/topic/list",
    method: "get"
  })
}

export function getTopicVisible(topicId) {
  return request({
    url: "/wiki/user/topic/" + topicId,
    method: "get"
  })
}
