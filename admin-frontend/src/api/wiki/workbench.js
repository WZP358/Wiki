import request from "@/utils/request"

export function listMyWikiFavorites() {
  return request({
    url: "/wiki/manage/workbench/favorite/list",
    method: "get"
  })
}

export function addMyWikiFavorite(docId) {
  return request({
    url: "/wiki/manage/workbench/favorite/" + docId,
    method: "post"
  })
}

export function deleteMyWikiFavorite(docId) {
  return request({
    url: "/wiki/manage/workbench/favorite/" + docId,
    method: "delete"
  })
}

export function listMyWikiNotes() {
  return request({
    url: "/wiki/manage/workbench/note/list",
    method: "get"
  })
}

export function addWikiNote(data) {
  return request({
    url: "/wiki/manage/workbench/note",
    method: "post",
    data
  })
}

export function updateWikiNote(data) {
  return request({
    url: "/wiki/manage/workbench/note",
    method: "put",
    data
  })
}

export function deleteWikiNote(noteId) {
  return request({
    url: "/wiki/manage/workbench/note/" + noteId,
    method: "delete"
  })
}
