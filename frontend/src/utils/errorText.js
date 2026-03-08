const mapping = {
  NETWORK_ERROR: '网络不稳，请重试',
  UNAUTHORIZED: '登录状态已过期，请重新登录',
  FORBIDDEN: '当前账号没有操作权限',
  USER_ALREADY_EXISTS: '该账号信息已被注册',
  VERIFY_CODE_INVALID: '验证码错误或已过期',
  RATE_LIMITED: '请求太频繁，请稍后再试',
  DOC_CONFLICT: '文档存在冲突，请先同步最新版本',
  EDIT_LOCKED: '文档被其他人占用编辑中',
  INTERNAL_ERROR: '服务繁忙，请稍后再试'
}

export function mapError(code, fallback) {
  return mapping[code] || fallback || '请求失败，请稍后重试'
}
