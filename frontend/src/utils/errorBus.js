import { reactive } from 'vue'

export const errorState = reactive({
  visible: false,
  title: '',
  message: '',
  code: '',
  suggestion: '',
  // request context (best-effort)
  request: {
    method: '',
    url: '',
    status: null
  },
  // developer / support details (string)
  details: '',
  // original response body string (legacy)
  raw: '',
  time: 0
})

export const toastState = reactive({
  visible: false,
  message: '',
  type: 'info',
  title: '',
  time: 0
})

export const confirmState = reactive({
  visible: false,
  title: '',
  message: '',
  tone: 'default',
  confirmText: '确定',
  cancelText: '取消',
  input: false,
  inputValue: '',
  inputPlaceholder: '',
  resolve: null
})

let toastTimer = null

export function showError(payload) {
  errorState.visible = true
  errorState.title = payload.title || '请求失败'
  errorState.message = payload.message || '请求失败，请稍后重试'
  errorState.code = payload.code || 'UNKNOWN'
  errorState.suggestion = payload.suggestion || ''
  errorState.request = payload.request || { method: '', url: '', status: null }
  errorState.details = payload.details || ''
  errorState.raw = payload.raw || payload.details || ''
  errorState.time = payload.time || Date.now()
}

export function showToast(payload) {
  toastState.visible = true
  toastState.message = typeof payload === 'string' ? payload : (payload.message || '操作提示')
  toastState.type = typeof payload === 'string' ? 'info' : (payload.type || 'info')
  toastState.title = typeof payload === 'string' ? '' : (payload.title || '')
  toastState.time = Date.now()
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  toastTimer = setTimeout(() => {
    toastState.visible = false
  }, typeof payload === 'string' ? 2600 : (payload.duration || 2600))
}

export function closeError() {
  errorState.visible = false
}

export function confirmDialog(payload) {
  confirmState.visible = true
  confirmState.title = payload.title || '确认操作'
  confirmState.message = payload.message || ''
  confirmState.tone = payload.tone || 'default'
  confirmState.confirmText = payload.confirmText || '确定'
  confirmState.cancelText = payload.cancelText || '取消'
  confirmState.input = Boolean(payload.input)
  confirmState.inputValue = payload.inputValue || ''
  confirmState.inputPlaceholder = payload.inputPlaceholder || ''

  return new Promise(resolve => {
    confirmState.resolve = resolve
  })
}

export function promptDialog(payload) {
  return confirmDialog({
    ...payload,
    input: true,
    confirmText: payload.confirmText || '保存'
  })
}

export function resolveConfirm(confirmed) {
  const resolve = confirmState.resolve
  const value = confirmState.input ? confirmState.inputValue : confirmed
  confirmState.visible = false
  confirmState.resolve = null
  if (resolve) {
    resolve(confirmed ? value : null)
  }
}
