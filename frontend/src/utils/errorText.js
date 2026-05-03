const mapping = {
  NETWORK_ERROR: '网络不稳，请重试',
  INVALID_CREDENTIALS: '账号或密码错误',
  UNAUTHORIZED: '登录状态已过期，请重新登录',
  FORBIDDEN: '当前账号没有对应权限',
  USER_ALREADY_EXISTS: '该账号信息已被注册',
  VERIFY_CODE_INVALID: '验证码错误或已过期',
  RATE_LIMITED: '请求太频繁，请稍后再试',
  DOC_CONFLICT: '文档存在冲突，请先同步最新版本',
  EDIT_LOCKED: '文档被其他人占用编辑中',
  INTERNAL_ERROR: '服务繁忙，请稍后再试'
}

const suggestionMapping = {
  NETWORK_ERROR: '检查网络/代理后重试；如持续失败请联系管理员。',
  INVALID_CREDENTIALS: '确认账号/密码是否正确；如忘记密码请联系管理员重置。',
  UNAUTHORIZED: '请重新登录后再操作。',
  FORBIDDEN: '确认是否加入对应知识库或团队；如需编辑，请联系知识库管理员加入协作名单。',
  VALIDATION_FAILED: '检查必填项与格式是否正确。',
  RATE_LIMITED: '稍后再试，避免频繁点击。',
  DOC_CONFLICT: '先刷新获取最新版本，再重新编辑保存。',
  EDIT_LOCKED: '稍后重试或联系正在编辑的成员。',
  INTERNAL_ERROR: '稍后再试；如反复出现，请把“错误详情”复制给管理员。'
}

export function mapError(code, fallback) {
  return mapping[code] || fallback || '请求失败，请稍后重试'
}

export function mapSuggestion(code) {
  return suggestionMapping[code] || ''
}
