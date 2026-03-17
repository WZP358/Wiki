<template>
  <div class="app-shell">
    <AppNavigation v-if="!isPublicRoute" @search="goToSearch" />

    <main :class="['main-content', { full: isPublicRoute, 'with-nav': !isPublicRoute }]">
      <router-view />
    </main>

    <LoadingMask />
    <ErrorDialog />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from './store/theme'
import AppNavigation from './components/AppNavigation.vue'
import ErrorDialog from './components/ErrorDialog.vue'
import LoadingMask from './components/LoadingMask.vue'

const route = useRoute()
const router = useRouter()
const theme = useThemeStore()

const isPublicRoute = computed(() => route.meta.public)

theme.apply()

function goToSearch() {
  router.push('/search')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.main-content {
  min-width: 0;
  padding: 24px;
}

.main-content.with-nav {
  margin-left: 0;
  margin-top: 56px;
}

.main-content.full {
  margin-left: 0;
  margin-top: 0;
}

@media (max-width: 900px) {
  .main-content { padding: 16px; }
}
</style>
