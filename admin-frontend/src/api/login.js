import request from '@/utils/request'

export function login(username, password) {
  return request({
    url: '/auth/login',
    headers: {
      isToken: false
    },
    method: 'post',
    data: {
      account: username,
      password
    }
  })
}

export function getInfo() {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

export function logout() {
  return Promise.resolve()
}

export function getCodeImg() {
  return Promise.resolve({
    captchaEnabled: false
  })
}

export function register(data) {
  return request({
    url: '/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data
  })
}

export function unlockScreen(password) {
  return Promise.resolve({ password })
}
