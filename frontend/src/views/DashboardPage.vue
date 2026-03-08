<template>
  <div class="page">
    <section class="panel block">
      <h2>我的知识库</h2>
      <div class="create">
        <input v-model="newKb.name" class="input" placeholder="知识库名称" />
        <select v-model="newKb.type" class="input">
          <option value="PUBLIC">公开</option>
          <option value="PRIVATE">私有</option>
          <option value="TEAM">团队</option>
        </select>
        <button class="btn btn-primary" @click="createKb">创建</button>
      </div>
      <div class="kbs">
        <button v-for="kb in kbs" :key="kb.id" class="kb-item btn" @click="pickKb(kb.id)">
          <span>{{ kb.name }}</span>
          <span class="tag">{{ kb.type }}</span>
        </button>
      </div>
    </section>

    <section v-if="activeKbId" class="panel block">
      <div class="head">
        <h2>文档看板</h2>
        <button class="btn btn-primary" @click="createDoc">新建文档</button>
      </div>
      <div class="lists">
        <div>
          <h3>最近更新</h3>
          <p v-for="doc in latest" :key="doc.id" class="link" @click="openDoc(doc.id)">{{ doc.title }}</p>
        </div>
        <div>
          <h3>最多阅读</h3>
          <p v-for="doc in hot" :key="doc.id" class="link" @click="openDoc(doc.id)">{{ doc.title }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { kbApi, docApi } from '../api/modules'

const router = useRouter()
const kbs = ref([])
const activeKbId = ref('')
const latest = ref([])
const hot = ref([])
const newKb = reactive({
  name: '',
  type: 'PUBLIC'
})

onMounted(async () => {
  await loadKbs()
  if (kbs.value[0]) {
    await pickKb(kbs.value[0].id)
  }
})

async function loadKbs() {
  kbs.value = await kbApi.mine()
}

async function createKb() {
  if (!newKb.name) {
    return
  }
  await kbApi.create(newKb)
  newKb.name = ''
  await loadKbs()
}

async function pickKb(kbId) {
  activeKbId.value = kbId
  latest.value = await docApi.latest(kbId)
  hot.value = await docApi.hot(kbId)
}

async function createDoc() {
  const created = await docApi.create({
    kbId: activeKbId.value,
    title: '未命名文档',
    markdownContent: '# 新文档\n',
    visibility: 'PUBLIC',
    published: true
  })
  router.push(`/editor/${activeKbId.value}/${created.id}`)
}

function openDoc(docId) {
  router.push(`/editor/${activeKbId.value}/${docId}`)
}
</script>

<style scoped>
.page {
  display: grid;
  gap: 12px;
}

.block {
  padding: 16px;
}

.block h2 {
  margin: 0 0 12px;
}

.create {
  display: grid;
  grid-template-columns: 1fr 130px auto;
  gap: 8px;
  margin-bottom: 12px;
}

.kbs {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}

.kb-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.lists {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.link {
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid var(--line);
}

@media (max-width: 900px) {
  .lists {
    grid-template-columns: 1fr;
  }

  .create {
    grid-template-columns: 1fr;
  }
}
</style>
