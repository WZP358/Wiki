<template>
  <section class="panel page">
    <h2>回收站</h2>
    <div v-for="doc in docs" :key="doc.id" class="row">
      <span>{{ doc.title }}</span>
      <div class="actions">
        <button class="btn" @click="restore(doc.id)">恢复</button>
        <button class="btn" @click="preparePurge(doc.id)">彻底删除</button>
      </div>
    </div>

    <div v-if="purgeTarget" class="confirm panel">
      <p>此操作不可逆，是否继续？</p>
      <div class="actions">
        <button class="btn" @click="purgeTarget = ''">取消</button>
        <button class="btn btn-primary" @click="purge">确认彻底删除</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { docApi } from '../api/modules'

const docs = ref([])
const purgeTarget = ref('')

onMounted(load)

async function load() {
  docs.value = await docApi.recycle()
}

async function restore(id) {
  await docApi.restore(id)
  await load()
}

function preparePurge(id) {
  purgeTarget.value = id
}

async function purge() {
  await docApi.purge(purgeTarget.value, true)
  purgeTarget.value = ''
  await load()
}
</script>

<style scoped>
.page {
  padding: 16px;
}

.page h2 {
  margin: 0 0 12px;
}

.row {
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 10px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.actions {
  display: flex;
  gap: 8px;
}

.confirm {
  margin-top: 12px;
  padding: 12px;
}

.confirm p {
  margin: 0 0 10px;
}
</style>
