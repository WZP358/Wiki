<template>
  <section class="panel share">
    <h2>{{ doc?.title || '分享文档' }}</h2>
    <article v-if="doc" v-html="doc.htmlContent"></article>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { shareApi } from '../api/modules'

const route = useRoute()
const doc = ref(null)

onMounted(async () => {
  doc.value = await shareApi.view(route.params.token)
})
</script>

<style scoped>
.share {
  max-width: 980px;
  margin: 24px auto;
  padding: 18px;
}

.share h2 {
  margin-top: 0;
}
</style>
