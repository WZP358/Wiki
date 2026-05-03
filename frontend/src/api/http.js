import axios from 'axios'
import { showError, showToast } from '../utils/errorBus'
import { mapError, mapSuggestion } from '../utils/errorText'
import { startLoading, stopLoading } from '../utils/loadingBus'

const TOKEN_KEY = 'wiki-token'

function normalizeBaseUrl(raw) {
  if (!raw) return ''
  // remove trailing slashes
  return String(raw).replace(/\/+$/, '')
}

// Dev: default '/api' and rely on Vite proxy.
// Prod: set VITE_API_BASE_URL (e.g. 'http://localhost:8080/api' or 'https://api.example.com/api')
const API_BASE_URL = normalizeBaseUrl(import.meta?.env?.VITE_API_BASE_URL) || '/api'

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
})

export function buildUrl(path, params = {}) {
  const q = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    q.append(k, String(v))
  })
  const qs = q.toString()
  const p = String(path || '')
  // Ensure we return an absolute-ish URL that works for browser download in both dev proxy and prod baseURL.
  const base = API_BASE_URL || ''
  const full = (base.endsWith('/') ? base.slice(0, -1) : base) + (p.startsWith('/') ? p : `/${p}`)
  return qs ? `${full}?${qs}` : full
}

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
    const status = error.response?.status ?? null
    const method = (error.config?.method || '').toUpperCase()
    const url = error.config?.url || ''

    // Special-case: login credential failure should not be shown as "login expired".
    const isLoginRequest = status === 401 && typeof url === 'string' && url.includes('/auth/login')

    if (status === 401 && !isLoginRequest) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('wiki-user')
      if (window.location.pathname !== '/auth') {
        window.location.assign('/auth')
      }
      return Promise.reject(error)
    }

    const code =
      (isLoginRequest ? 'INVALID_CREDENTIALS' : res?.code) ||
      error.code ||
      (error.message === 'Network Error' ? 'NETWORK_ERROR' : status === 401 ? 'UNAUTHORIZED' : status === 403 ? 'FORBIDDEN' : 'INTERNAL_ERROR')
    const message = mapError(code, res?.message || error.message)
    const suggestion = mapSuggestion(code)

    const toastOnlyCodes = new Set([
      'FORBIDDEN',
      'VALIDATION_FAILED',
      'VERIFY_CODE_INVALID',
      'RATE_LIMITED',
      'DOC_CONFLICT',
      'EDIT_LOCKED',
      'USER_ALREADY_EXISTS'
    ])

    if (!isLoginRequest && (toastOnlyCodes.has(code) || (status >= 400 && status < 500))) {
      showToast({
        message,
        type: status === 403 || code === 'FORBIDDEN' ? 'warning' : 'error',
        duration: 3200
      })
      return Promise.reject(error)
    }

    const detailsObj = {
      time: new Date().toISOString(),
      code,
      status,
      request: { method, url },
      message: res?.message || error.message,
      response: res || null
    }
    showError({
      title: status ? `请求失败（HTTP ${status}）` : '请求失败',
      message,
      code,
      suggestion,
      request: { method, url, status },
      details: JSON.stringify(detailsObj, null, 2),
      raw: JSON.stringify(res || { message: error.message })
    })
    return Promise.reject(error)
  }
)

export default http
