<template>
  <section class="panel page">
    <h2>个人中心</h2>
    <div class="form">
      <label>用户ID</label>
      <div class="user-id-row">
        <input class="input" :value="auth.user?.id" disabled />
        <button class="btn btn-sm" @click="copyUserId">复制</button>
      </div>
      <p class="hint">其他用户可以通过此ID搜索你的公开知识库</p>

      <label>用户名</label>
      <input class="input" :value="auth.user?.username" disabled />

      <label>昵称</label>
      <input class="input" v-model="form.nickname" />

      <label>头像URL</label>
      <input class="input" v-model="form.avatarUrl" />

      <label>邮箱</label>
      <div class="row">
        <input class="input" v-model="form.email" placeholder="请输入新邮箱" />
        <button class="btn" @click="sendUpdateCode('EMAIL')">发送邮箱验证码</button>
      </div>
      <input class="input" v-model="form.emailCode" placeholder="邮箱验证码（修改邮箱时必填）" />

      <label>手机号</label>
      <div class="row">
        <input class="input" v-model="form.phone" placeholder="请输入新手机号" />
        <button class="btn" @click="sendUpdateCode('PHONE')">发送短信验证码</button>
      </div>
      <input class="input" v-model="form.phoneCode" placeholder="短信验证码（修改手机号时必填）" />

      <button class="btn btn-primary" @click="save">保存</button>
      <p v-if="tip" class="tip">{{ tip }}</p>
    </div>
  </section>
</template>

<script setup>
import { reactive, watchEffect, ref } from 'vue'
import { authApi } from '../api/modules'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const tip = ref('')
const form = reactive({
  nickname: '',
  avatarUrl: '',
  email: '',
  emailCode: '',
  phone: '',
  phoneCode: ''
})

watchEffect(() => {
  form.nickname = auth.user?.nickname || ''
  form.avatarUrl = auth.user?.avatarUrl || ''
  form.email = auth.user?.email || ''
  form.phone = auth.user?.phone || ''
})

async function sendUpdateCode(type) {
  const target = type === 'EMAIL' ? form.email : form.phone
  if (!target) {
    tip.value = type === 'EMAIL' ? '请先输入邮箱' : '请先输入手机号'
    return
  }
  const result = await authApi.sendUpdateCode({ target, type })
  tip.value = result.testMode ? `测试模式验证码：${result.code}` : result.message
}

async function save() {
  const user = await authApi.updateProfile(form)
  auth.setLogin({ token: auth.token, user })
  form.emailCode = ''
  form.phoneCode = ''
  tip.value = '保存成功'
}

function copyUserId() {
  const userId = auth.user?.id
  if (userId) {
    navigator.clipboard.writeText(String(userId))
    tip.value = '用户ID已复制到剪贴板'
  }
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.form {
  max-width: 640px;
  display: grid;
  gap: 8px;
}

.row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.tip {
  margin: 0;
  color: var(--brand);
}

.user-id-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
}

.hint {
  margin: -4px 0 8px 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}
</style>
