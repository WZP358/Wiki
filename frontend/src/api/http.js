import axios from 'axios'
import { showError } from '../utils/errorBus'
import { mapError } from '../utils/errorText'
import { startLoading, stopLoading } from '../utils/loadingBus'

const TOKEN_KEY = 'wiki-token'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use(config => {
  startLoading()
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    stopLoading()
    const payload = response.data
    if (payload?.success === false) {
      const error = new Error(payload.message || '请求失败')
      error.code = payload.code
      throw error
    }
    return payload.data
  },
  error => {
    stopLoading()
    const res = error.response?.data
    const code = res?.code || error.code || (error.message === 'Network Error' ? 'NETWORK_ERROR' : 'INTERNAL_ERROR')
    const message = mapError(code, res?.message || error.message)
    showError({
      message,
      code,
      raw: JSON.stringify(res || { message: error.message })
    })
    return Promise.reject(error)
  }
)

export default http
