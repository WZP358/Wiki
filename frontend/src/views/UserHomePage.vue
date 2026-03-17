<template>
  <div class="user-home">
    <header class="user-header">
      <button class="back-btn" @click="$router.back()">←</button>
      <div class="user-info">
        <img v-if="user?.avatarUrl" :src="user.avatarUrl" class="avatar" />
        <div class="meta">
          <h2>{{ user?.nickname || user?.username || '用户主页' }}</h2>
          <p class="sub">
            <span>ID: {{ userId }}</span>
            <span v-if="user?.username" class="dot">·</span>
            <span v-if="user?.username">用户名：{{ user.username }}</span>
            <span v-if="user?.departmentName" class="dot">·</span>
            <span v-if="user?.departmentName">{{ user.departmentName }}</span>
          </p>
        </div>
      </div>
    </header>

    <main class="user-content">
      <section class="section">
        <div class="section-header">
          <h3>公开知识库</h3>
          <span class="count">{{ kbs.length }} 个</span>
        </div>
        <div class="kb-list">
          <div
            v-for="kb in kbs"
            :key="kb.id"
            class="kb-card"
            @click="$router.push(`/kb/${kb.id}`)"
          >
            <div class="kb-avatar-mini kb-company">
              {{ kb.name.charAt(0).toUpperCase() }}
            </div>
            <div class="kb-body">
              <h4>{{ kb.name }}</h4>
              <p class="desc">{{ kb.description || '暂无描述' }}</p>
            </div>
          </div>
          <div v-if="kbs.length === 0" class="empty-tip">该用户暂无公开知识库</div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { authApi, kbApi } from '../api/modules'

const route = useRoute()
const userId = route.params.userId

const user = ref(null)
const kbs = ref([])

onMounted(async () => {
  user.value = await authApi.publicUserById(userId)
  kbs.value = await kbApi.publicByUser(userId)
})
</script>

<style scoped>
.user-home {
  min-height: 100vh;
  background: var(--bg);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
}

.back-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.meta h2 {
  margin: 0;
  font-size: 18px;
  color: var(--text);
}

.sub {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.dot::before {
  content: '·';
  margin: 0 2px;
}

.user-content {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

.section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 15px;
}

.count {
  font-size: 13px;
  color: var(--text-secondary);
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.kb-card:hover {
  background: var(--line-light);
}

.kb-avatar-mini {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
}

.kb-body h4 {
  margin: 0 0 2px 0;
  font-size: 14px;
}

.kb-body .desc {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
}

.empty-tip {
  padding: 24px 0;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}
</style>

