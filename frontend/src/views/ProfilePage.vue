<template>
  <main class="app-container ry-page">
    <el-card shadow="never">
      <template #header>
        <div class="ry-card-header">
          <span>个人中心</span>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="用户ID">
          <el-input :model-value="auth.user?.id" disabled>
            <template #append>
              <el-button @click="copyUserId">复制</el-button>
            </template>
          </el-input>
          <div class="form-tip">其他用户可以通过此 ID 搜索你的公开知识库。</div>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input :model-value="auth.user?.username" disabled />
        </el-form-item>

        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="头像">
          <div class="avatar-row">
            <el-avatar :size="64" :src="form.avatarUrl">{{ avatarText }}</el-avatar>
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              accept="image/jpeg,image/png,image/webp,image/gif"
              :on-change="handleAvatarChange"
            >
              <el-button>上传头像</el-button>
            </el-upload>
          </div>
          <div class="form-tip">仅支持上传图片文件。</div>
        </el-form-item>

        <el-divider />

        <el-form-item label="邮箱">
          <div class="inline-row">
            <el-input v-model="form.email" placeholder="请输入新邮箱" clearable />
            <el-button @click="sendUpdateCode('EMAIL')">发送验证码</el-button>
          </div>
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <el-input v-model="form.emailCode" placeholder="修改邮箱时必填" clearable />
        </el-form-item>

        <el-form-item label="手机号">
          <div class="inline-row">
            <el-input v-model="form.phone" placeholder="请输入新手机号" clearable />
            <el-button @click="sendUpdateCode('PHONE')">发送验证码</el-button>
          </div>
        </el-form-item>
        <el-form-item label="短信验证码">
          <el-input v-model="form.phoneCode" placeholder="修改手机号时必填" clearable />
        </el-form-item>
      </el-form>

      <el-alert v-if="tip" class="profile-tip" :title="tip" type="success" :closable="false" show-icon />
    </el-card>
  </main>
</template>

<script setup>
import { computed, reactive, ref, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/modules'
import { useAuthStore } from '../store/auth'
import { showToast } from '../utils/errorBus'

const auth = useAuthStore()
const tip = ref('')
const saving = ref(false)
const form = reactive({
  nickname: '',
  avatarUrl: '',
  email: '',
  emailCode: '',
  phone: '',
  phoneCode: ''
})

const avatarText = computed(() => (form.nickname || auth.user?.username || 'U').slice(0, 1).toUpperCase())

watchEffect(() => {
  form.nickname = auth.user?.nickname || ''
  form.avatarUrl = auth.user?.avatarUrl || ''
  form.email = auth.user?.email || ''
  form.phone = auth.user?.phone || ''
})

async function sendUpdateCode(type) {
  const target = type === 'EMAIL' ? form.email : form.phone
  if (!target) {
    showToast({ type: 'warning', message: type === 'EMAIL' ? '请先输入邮箱' : '请先输入手机号' })
    return
  }
  const result = await authApi.sendUpdateCode({ target, type })
  const message = result.testMode ? `验证码：${result.code}` : result.message
  tip.value = message
  showToast({ type: 'success', message, duration: 5000 })
}

async function save() {
  saving.value = true
  try {
    const user = await authApi.updateProfile(form)
    auth.setLogin({ token: auth.token, user })
    form.emailCode = ''
    form.phoneCode = ''
    tip.value = '个人资料已保存'
    showToast({ type: 'success', message: '个人资料已保存' })
  } finally {
    saving.value = false
  }
}

async function handleAvatarChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  try {
    const result = await authApi.uploadAvatar(file)
    form.avatarUrl = result.avatarUrl
    tip.value = '头像上传成功'
    showToast({ type: 'success', message: '头像上传成功' })
  } catch {
    showToast({ type: 'error', message: '头像上传失败，请稍后重试' })
  }
}

async function copyUserId() {
  const userId = auth.user?.id
  if (!userId) return
  await navigator.clipboard.writeText(String(userId))
  tip.value = '用户ID已复制到剪贴板'
  ElMessage.success('用户ID已复制到剪贴板')
}
</script>

<style scoped>
.profile-form {
  max-width: 720px;
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.avatar-row,
.inline-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.inline-row .el-input {
  flex: 1;
}

.profile-tip {
  max-width: 720px;
  margin-top: 14px;
}

@media (max-width: 720px) {
  .inline-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
