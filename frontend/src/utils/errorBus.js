import { reactive } from 'vue'

export const errorState = reactive({
  visible: false,
  message: '',
  code: '',
  raw: ''
})

export function showError(payload) {
  errorState.visible = true
  errorState.message = payload.message || '请求失败，请稍后重试'
  errorState.code = payload.code || 'UNKNOWN'
  errorState.raw = payload.raw || ''
}

export function closeError() {
  errorState.visible = false
}
