import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export const errorState = reactive({
  visible: false,
  title: '',
  message: '',
  code: '',
  suggestion: '',
  request: {
    method: '',
    url: '',
    status: null
  },
  details: '',
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

function boxType(tone) {
  if (tone === 'danger') return 'error'
  if (tone === 'warning') return 'warning'
  return 'info'
}

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

  const messageParts = [
    errorState.message,
    errorState.suggestion ? `建议：${errorState.suggestion}` : '',
    errorState.request?.url
      ? `请求：${String(errorState.request.method || 'GET').toUpperCase()} ${errorState.request.url}${errorState.request.status ? `（HTTP ${errorState.request.status}）` : ''}`
      : ''
  ].filter(Boolean)

  return ElMessageBox.alert(messageParts.join('\n\n'), errorState.title, {
    type: 'error',
    confirmButtonText: '知道了',
    closeOnClickModal: false
  }).catch(() => {})
}

export function showToast(payload) {
  const message = typeof payload === 'string' ? payload : (payload.message || '操作提示')
  const type = typeof payload === 'string' ? 'info' : (payload.type || 'info')
  const title = typeof payload === 'string' ? '' : (payload.title || '')
  const duration = typeof payload === 'string' ? 2600 : (payload.duration || 2600)

  toastState.visible = true
  toastState.message = message
  toastState.type = type
  toastState.title = title
  toastState.time = Date.now()
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
  toastTimer = setTimeout(() => {
    toastState.visible = false
  }, duration)

  return ElMessage({
    message: title ? `${title}：${message}` : message,
    type,
    duration,
    showClose: true
  })
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
  confirmState.input = false
  confirmState.inputValue = ''
  confirmState.inputPlaceholder = ''

  return ElMessageBox.confirm(confirmState.message, confirmState.title, {
    type: boxType(confirmState.tone),
    confirmButtonText: confirmState.confirmText,
    cancelButtonText: confirmState.cancelText,
    closeOnClickModal: false
  }).then(() => true).catch(() => null).finally(() => {
    confirmState.visible = false
    confirmState.resolve = null
  })
}

export function promptDialog(payload) {
  confirmState.visible = true
  confirmState.title = payload.title || '确认操作'
  confirmState.message = payload.message || ''
  confirmState.tone = payload.tone || 'default'
  confirmState.confirmText = payload.confirmText || '保存'
  confirmState.cancelText = payload.cancelText || '取消'
  confirmState.input = true
  confirmState.inputValue = payload.inputValue || ''
  confirmState.inputPlaceholder = payload.inputPlaceholder || ''

  return ElMessageBox.prompt(confirmState.message, confirmState.title, {
    type: boxType(confirmState.tone),
    confirmButtonText: confirmState.confirmText,
    cancelButtonText: confirmState.cancelText,
    inputValue: confirmState.inputValue,
    inputPlaceholder: confirmState.inputPlaceholder,
    closeOnClickModal: false
  }).then(({ value }) => value).catch(() => null).finally(() => {
    confirmState.visible = false
    confirmState.resolve = null
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
