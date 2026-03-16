<template>
  <div class="auth-wrap">
    <div class="auth-card panel">
      <h1>协同 Wiki</h1>
      <div class="tabs">
        <button class="btn" :class="{ 'btn-primary': mode === 'login' }" @click="mode = 'login'">登录</button>
        <button class="btn" :class="{ 'btn-primary': mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <template v-if="mode === 'login'">
        <input v-model="loginForm.account" class="input" placeholder="用户名 / 邮箱 / 手机号" />
        <input v-model="loginForm.password" type="password" class="input" placeholder="密码" />
        <button class="btn btn-primary" @click="login">登录</button>
      </template>

      <template v-else>
        <input v-model="registerForm.username" class="input" placeholder="用户名" />
        <input v-model="registerForm.contact" class="input" placeholder="邮箱或手机号（必填）" />
        <div class="row">
          <input v-model="registerForm.avatarUrl" class="input" placeholder="头像URL（可选）" />
          <label class="btn upload-btn">
            上传头像
            <input class="file-input" type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadAvatar" />
          </label>
        </div>
        <img v-if="registerForm.avatarUrl" :src="registerForm.avatarUrl" alt="avatar-preview" class="avatar-preview" />
        <div class="row">
          <input v-model="registerForm.code" class="input" placeholder="验证码" />
          <button class="btn" @click="sendCode">发送验证码</button>
        </div>
        <input v-model="registerForm.password" type="password" class="input" placeholder="密码" />
        <button class="btn btn-primary" @click="register">注册并登录</button>
      </template>

      <p v-if="tip" class="tip">{{ tip }}</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/modules'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref('login')
const tip = ref('')

const loginForm = reactive({
  account: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  contact: '',
  avatarUrl: '',
  code: '',
  password: ''
})

async function sendCode() {
  const target = registerForm.contact.trim()
  if (!target) {
    tip.value = '请填写邮箱或手机号'
    return
  }
  const res = await authApi.sendCode({ target })
  tip.value = res.testMode ? `测试模式验证码：${res.code}` : res.message
}

async function register() {
  registerForm.contact = registerForm.contact.trim()
  registerForm.avatarUrl = registerForm.avatarUrl.trim()
  if (!registerForm.contact) {
    tip.value = '请填写邮箱或手机号'
    return
  }
  const res = await authApi.register(registerForm)
  authStore.setLogin(res)
  router.push('/')
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  const result = await authApi.uploadAvatar(file)
  registerForm.avatarUrl = result.avatarUrl
  tip.value = '头像上传成功'
}

async function login() {
  if (!loginForm.account || !loginForm.password) {
    tip.value = '请输入用户名和密码'
    return
  }
  const res = await authApi.login(loginForm)
  authStore.setLogin(res)
  router.push('/')
}
</script>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-card {
  width: min(520px, 100%);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-card h1 {
  margin: 0;
  font-size: 30px;
}

.tabs {
  display: flex;
  gap: 8px;
}

.row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.upload-btn {
  position: relative;
  overflow: hidden;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
}

.tip {
  margin: 0;
  color: var(--brand);
}
</style>
