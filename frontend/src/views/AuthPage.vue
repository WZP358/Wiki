<template>
  <div class="auth-wrap">
    <el-card class="auth-card" shadow="never">
      <template #header>
        <div class="auth-title">
          <strong>协同 Wiki</strong>
          <span>企业内部知识库系统</span>
        </div>
      </template>

      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent>
            <el-form-item label="账号">
              <el-input v-model="loginForm.account" placeholder="用户名 / 邮箱 / 手机号" clearable />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter="login" />
            </el-form-item>
            <el-button type="primary" class="full-btn" :loading="submitting" @click="login">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-position="top" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="邮箱或手机号">
              <el-input v-model="registerForm.contact" placeholder="用于接收验证码" clearable />
            </el-form-item>
            <el-form-item label="头像">
              <div class="avatar-row">
                <el-avatar :size="56" :src="registerForm.avatarUrl">{{ avatarText }}</el-avatar>
                <el-upload
                  :show-file-list="false"
                  :auto-upload="false"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  :on-change="handleAvatarChange"
                >
                  <el-button>上传头像</el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item label="验证码">
              <div class="code-row">
                <el-input v-model="registerForm.code" placeholder="请输入验证码" />
                <el-button @click="sendCode">发送验证码</el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter="register" />
            </el-form-item>
            <el-button type="primary" class="full-btn" :loading="submitting" @click="register">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <el-alert v-if="tip" class="auth-tip" :title="tip" type="info" :closable="false" show-icon />
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { authApi } from '../api/modules'
import { useAuthStore } from '../store/auth'
import { showToast } from '../utils/errorBus'

const router = useRouter()
const authStore = useAuthStore()
const mode = ref('login')
const tip = ref('')
const submitting = ref(false)

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

const avatarText = computed(() => (registerForm.username || 'U').slice(0, 1).toUpperCase())

async function sendCode() {
  const target = registerForm.contact.trim()
  if (!target) {
    tip.value = '请填写邮箱或手机号'
    return
  }
  try {
    const res = await authApi.sendCode({ target })
    if (res?.code) {
      registerForm.code = res.code
      tip.value = '验证码已自动填入'
      await ElMessageBox.alert(`本次注册验证码：${res.code}`, '验证码', {
        confirmButtonText: '知道了',
        type: 'info'
      })
      return
    }
    tip.value = res?.message || '验证码已发送'
  } catch {
    tip.value = '发送验证码失败，请稍后重试'
  }
}

async function register() {
  registerForm.contact = registerForm.contact.trim()
  if (!registerForm.contact) {
    tip.value = '请填写邮箱或手机号'
    return
  }
  submitting.value = true
  try {
    const res = await authApi.register(registerForm)
    authStore.setLogin(res)
    router.push('/')
  } catch {
    tip.value = '注册失败，请检查填写信息后重试'
  } finally {
    submitting.value = false
  }
}

async function handleAvatarChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  try {
    const result = await authApi.uploadAvatar(file)
    registerForm.avatarUrl = result.avatarUrl
    showToast({ type: 'success', message: '头像上传成功' })
  } catch {
    showToast({ type: 'error', message: '头像上传失败，请稍后重试' })
  }
}

async function login() {
  if (!loginForm.account || !loginForm.password) {
    tip.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  try {
    const res = await authApi.login(loginForm)
    authStore.setLogin(res)
    router.push('/')
  } catch (e) {
    const status = e?.response?.status
    tip.value = status === 401 ? '账号或密码错误' : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f0f2f5;
}

.auth-card {
  width: min(460px, 100%);
}

.auth-title {
  display: grid;
  gap: 4px;
}

.auth-title strong {
  font-size: 22px;
}

.auth-title span {
  color: #909399;
  font-size: 13px;
}

.full-btn {
  width: 100%;
}

.code-row,
.avatar-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.code-row .el-input {
  flex: 1;
}

.auth-tip {
  margin-top: 14px;
}
</style>
