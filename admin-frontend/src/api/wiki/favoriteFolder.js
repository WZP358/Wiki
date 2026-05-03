import request from "@/utils/request"

export function listFavoriteFolder(query) {
  return request({
    url: "/wiki/admin/favoriteFolder/list",
    method: "get",
    params: query
  })
}

export function getFavoriteFolder(folderId) {
  return request({
    url: "/wiki/admin/favoriteFolder/" + folderId,
    method: "get"
  })
}

export function addFavoriteFolder(data) {
  return request({
    url: "/wiki/admin/favoriteFolder",
    method: "post",
    data
  })
}

export function updateFavoriteFolder(data) {
  return request({
    url: "/wiki/admin/favoriteFolder",
    method: "put",
    data
  })
}

export function delFavoriteFolder(folderIds) {
  return request({
    url: "/wiki/admin/favoriteFolder/" + folderIds,
    method: "delete"
  })
}

export function listFavoriteFolderOptions(query) {
  return request({
    url: "/wiki/admin/favoriteFolder/options",
    method: "get",
    params: query
  })
}
