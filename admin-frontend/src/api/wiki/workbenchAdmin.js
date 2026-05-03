import request from "@/utils/request"

export function listAdminFavorites(query) {
  return request({
    url: "/wiki/admin/workbench/favorite/list",
    method: "get",
    params: query
  })
}

export function addAdminFavorite(data) {
  return request({
    url: "/wiki/admin/workbench/favorite",
    method: "post",
    data
  })
}

export function deleteAdminFavorite(favoriteIds) {
  return request({
    url: "/wiki/admin/workbench/favorite/" + favoriteIds,
    method: "delete"
  })
}

export function listAdminFavoriteFolderOptions(query) {
  return request({
    url: "/wiki/admin/workbench/favorite/folder/options",
    method: "get",
    params: query
  })
}

export function listAdminNotes(query) {
  return request({
    url: "/wiki/admin/workbench/note/list",
    method: "get",
    params: query
  })
}

export function getAdminNote(noteId) {
  return request({
    url: "/wiki/admin/workbench/note/" + noteId,
    method: "get"
  })
}

export function addAdminNote(data) {
  return request({
    url: "/wiki/admin/workbench/note",
    method: "post",
    data
  })
}

export function updateAdminNote(data) {
  return request({
    url: "/wiki/admin/workbench/note",
    method: "put",
    data
  })
}

export function deleteAdminNote(noteIds) {
  return request({
    url: "/wiki/admin/workbench/note/" + noteIds,
    method: "delete"
  })
}

export function archiveAdminNote(noteId, archiveKbId) {
  return request({
    url: "/wiki/admin/workbench/note/" + noteId + "/archive",
    method: "post",
    data: { archiveKbId }
  })
}

export function listAdminHistories(query) {
  return request({
    url: "/wiki/admin/workbench/history/list",
    method: "get",
    params: query
  })
}

export function deleteAdminHistory(historyIds) {
  return request({
    url: "/wiki/admin/workbench/history/" + historyIds,
    method: "delete"
  })
}

export function cleanAdminHistory() {
  return request({
    url: "/wiki/admin/workbench/history/clean",
    method: "delete"
  })
}

export function listAdminComments(query) {
  return request({
    url: "/wiki/admin/workbench/comment/list",
    method: "get",
    params: query
  })
}

export function getAdminComment(commentId) {
  return request({
    url: "/wiki/admin/workbench/comment/" + commentId,
    method: "get"
  })
}

export function addAdminComment(data) {
  return request({
    url: "/wiki/admin/workbench/comment",
    method: "post",
    data
  })
}

export function updateAdminComment(data) {
  return request({
    url: "/wiki/admin/workbench/comment",
    method: "put",
    data
  })
}

export function deleteAdminComment(commentIds) {
  return request({
    url: "/wiki/admin/workbench/comment/" + commentIds,
    method: "delete"
  })
}

export function getAdminDashboard() {
  return request({
    url: "/wiki/admin/workbench/dashboard",
    method: "get"
  })
}
