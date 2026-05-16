import axios from 'axios'
import { Message, MessageBox, Loading } from 'element-ui'
import store from '@/store'
import { getToken, removeToken } from '@/utils/auth'
import { tansParams, blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'

let downloadLoadingInstance
export let isRelogin = { show: false }

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 10000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers.Authorization = 'Bearer ' + getToken()
  }
  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }
  return config
}, error => Promise.reject(error))

service.interceptors.response.use(res => {
  if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
    return res.data
  }

  const payload = res.data
  if (payload && Object.prototype.hasOwnProperty.call(payload, 'success')) {
    if (payload.success) {
      return payload.data
    }
    const code = payload.code
    const msg = payload.message || '请求失败'
    if (code === 'UNAUTHORIZED') {
      handleUnauthorized()
    } else {
      Message({ message: msg, type: code === 'FORBIDDEN' ? 'warning' : 'error', duration: 5000 })
    }
    return Promise.reject(new Error(msg))
  }

  return payload
}, error => {
  const status = error.response && error.response.status
  const data = error.response && error.response.data
  const message = (data && (data.message || data.msg)) || error.message || '请求失败'
  if (status === 401) {
    handleUnauthorized()
  } else {
    Message({ message, type: status === 403 ? 'warning' : 'error', duration: 5000 })
  }
  return Promise.reject(error)
})

function handleUnauthorized() {
  removeToken()
  const basePath = process.env.BASE_URL || '/'
  const normalizedBase = basePath.replace(/\/$/, '')
  if (window.location.pathname === `${normalizedBase}/login`) {
    return
  }
  if (isRelogin.show) {
    return
  }
  isRelogin.show = true
  MessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    isRelogin.show = false
    store.dispatch('FedLogOut').then(() => {
      location.href = `${basePath}login`
    })
  }).catch(() => {
    isRelogin.show = false
  })
}

export function download(url, params, filename, config) {
  downloadLoadingInstance = Loading.service({
    text: '正在下载数据，请稍候',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  return service.post(url, params, {
    transformRequest: [(params) => tansParams(params)],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    ...config
  }).then(async (data) => {
    const isBlob = blobValidate(data)
    if (isBlob) {
      saveAs(new Blob([data]), filename)
    } else {
      const resText = await data.text()
      const rspObj = JSON.parse(resText)
      Message.error(rspObj.message || rspObj.msg || '下载失败')
    }
    downloadLoadingInstance.close()
  }).catch((error) => {
    console.error(error)
    Message.error('下载文件出现错误，请联系管理员')
    downloadLoadingInstance.close()
  })
}

export default service
