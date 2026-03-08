<template>
  <div class="editor-grid">
    <aside class="panel sidebar">
      <div class="row">
        <input v-model="keyword" class="input" placeholder="搜索标题/内容" @keyup.enter="search" />
        <button class="btn" @click="search">搜</button>
      </div>
      <button class="btn btn-primary" @click="createDoc">新建文档</button>
      <div class="tree">
        <button
          v-for="node in tree"
          :key="node.id"
          class="btn doc-item"
          :class="{ active: String(node.id) === String(docId) }"
          @click="openDoc(node.id)"
        >
          <span>{{ node.title }}</span>
          <small>v{{ node.versionNo }}</small>
        </button>
      </div>
    </aside>

    <section v-if="doc" class="panel editor">
      <div class="toolbar">
        <input v-model="form.title" class="input" placeholder="文档标题" />
        <select v-model="form.visibility" class="input short">
          <option value="PUBLIC">公开</option>
          <option value="TEAM">团队</option>
          <option value="PRIVATE">私有</option>
        </select>
        <button class="btn btn-primary" @click="saveDoc">保存</button>
        <button class="btn" @click="openVersions">历史版本</button>
        <button class="btn" @click="createShare">分享</button>
        <button class="btn" @click="removeDoc">删除</button>
      </div>

      <p v-if="shareLink" class="tip">分享链接：{{ location.origin }}/share/{{ shareLink }}</p>
      <p class="tip">协作状态：{{ collabStatus }}</p>

      <div class="collab panel">
        <div class="collab-title">协作者光标与选区</div>
        <div class="collab-list">
          <div v-for="p in collaborators" :key="p.sessionId" class="collab-item">
            <img v-if="p.avatarUrl" :src="p.avatarUrl" alt="avatar" class="avatar" />
            <span v-else class="avatar text">{{ p.username?.slice(0, 1) || '?' }}</span>
            <div class="meta">
              <strong>{{ p.username }}</strong>
              <small>位置 {{ p.cursorStart }} - {{ p.cursorEnd }}</small>
              <small class="selection">{{ selectedText(p) }}</small>
            </div>
          </div>
          <p v-if="collaborators.length === 0" class="empty-tip">暂无其他协作者</p>
        </div>
      </div>

      <div class="split">
        <textarea
          ref="editorRef"
          v-model="form.markdownContent"
          class="editor-input"
          @click="reportCursor"
          @keyup="reportCursor"
          @select="reportCursor"
        ></textarea>
        <article class="preview" v-html="previewHtml"></article>
      </div>
    </section>

    <section v-else class="panel empty">请选择文档</section>

    <div v-if="showVersions" class="overlay" @click.self="showVersions = false">
      <div class="panel versions">
        <h3>版本历史</h3>
        <div class="actions compare">
          <select v-model="compare.left" class="input">
            <option value="">左侧版本</option>
            <option v-for="v in versions" :key="'l-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
          </select>
          <select v-model="compare.right" class="input">
            <option value="">右侧版本</option>
            <option v-for="v in versions" :key="'r-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
          </select>
          <button class="btn" @click="compareVersions">对比</button>
        </div>
        <div class="version-list">
          <button v-for="v in versions" :key="v.id" class="btn version-item" @click="rollback(v.id)">
            <span>v{{ v.versionNo }} - {{ v.editorName || v.editorId }}</span>
            <small>{{ v.commitMessage || '更新文档' }}</small>
          </button>
        </div>
        <div v-if="versionDiff.length > 0" class="diff-wrap">
          <div v-for="(line, idx) in versionDiff" :key="idx" class="diff-row" :class="line.type.toLowerCase()">
            <code class="left">{{ line.left }}</code>
            <code class="right">{{ line.right }}</code>
          </div>
        </div>
        <button class="btn" @click="showVersions = false">关闭</button>
      </div>
    </div>

    <div v-if="conflict.visible" class="overlay" @click.self="conflict.visible = false">
      <div class="panel versions">
        <h3>检测到冲突</h3>
        <p>{{ conflict.message }}</p>
        <div class="actions">
          <button class="btn" @click="useServerVersion">使用远端版本</button>
          <button class="btn" @click="useSuggestedVersion">插入冲突标记</button>
          <button class="btn btn-primary" @click="retryLocalVersion">保留本地并重试</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { useAuthStore } from '../store/auth'
import { docApi, shareApi } from '../api/modules'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const kbId = computed(() => route.params.kbId)
const docId = computed(() => route.params.docId)

const tree = ref([])
const doc = ref(null)
const form = reactive({
  title: '',
  markdownContent: '',
  visibility: 'PUBLIC',
  baseVersion: 0
})
const keyword = ref('')
const showVersions = ref(false)
const versions = ref([])
const compare = reactive({ left: '', right: '' })
const versionDiff = ref([])
const shareLink = ref('')
const editorRef = ref(null)

const previewHtml = computed(() => marked.parse(form.markdownContent || ''))

const ws = ref(null)
const mySessionId = ref('')
const collabStatus = ref('未连接')
const participants = ref([])
const applyingRemote = ref(false)
const conflict = reactive({
  visible: false,
  message: '',
  serverContent: '',
  suggestedContent: '',
  serverVersion: 0
})
let draftTimer = null
let syncTimer = null

const collaborators = computed(() => participants.value.filter(p => p.sessionId !== mySessionId.value))

onMounted(async () => {
  await loadTree()
  if (docId.value) {
    await loadDoc(docId.value)
  }
  draftTimer = setInterval(() => {
    if (docId.value) {
      saveDraft()
    }
  }, 30000)
  window.addEventListener('beforeunload', beforeUnload)
})

watch(() => route.params.docId, async next => {
  if (!next) {
    return
  }
  await loadDoc(next)
})

watch(
  () => [form.title, form.markdownContent],
  () => {
    if (applyingRemote.value || !ws.value || ws.value.readyState !== WebSocket.OPEN || !doc.value) {
      return
    }
    if (syncTimer) {
      clearTimeout(syncTimer)
    }
    syncTimer = setTimeout(() => {
      sendWs({
        type: 'update',
        docId: doc.value.id,
        title: form.title,
        content: form.markdownContent,
        baseVersion: form.baseVersion
      })
    }, 450)
  }
)

onBeforeUnmount(() => {
  clearInterval(draftTimer)
  if (syncTimer) {
    clearTimeout(syncTimer)
  }
  disconnectCollab()
  window.removeEventListener('beforeunload', beforeUnload)
})

function beforeUnload() {
  disconnectCollab()
}

async function loadTree() {
  tree.value = await docApi.tree(kbId.value)
}

async function loadDoc(id) {
  disconnectCollab()
  doc.value = await docApi.detail(id)
  form.title = doc.value.title
  form.markdownContent = doc.value.markdownContent
  form.visibility = doc.value.visibility
  form.baseVersion = doc.value.versionNo
  const draft = await docApi.getDraft(id)
  if (draft?.markdownContent) {
    form.markdownContent = draft.markdownContent
    form.title = draft.title || form.title
  }
  connectCollab(id)
}

function connectCollab(id) {
  const token = localStorage.getItem('wiki-token')
  if (!token) {
    return
  }
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const endpoint = `${protocol}://${location.host}/ws/collab?token=${encodeURIComponent(token)}`
  const socket = new WebSocket(endpoint)
  ws.value = socket
  collabStatus.value = '连接中'

  socket.onopen = () => {
    collabStatus.value = '已连接'
    sendWs({ type: 'join', docId: String(id), cursorStart: 0, cursorEnd: 0 })
  }

  socket.onclose = () => {
    collabStatus.value = '已断开'
  }

  socket.onerror = () => {
    collabStatus.value = '连接失败'
  }

  socket.onmessage = event => {
    const payload = JSON.parse(event.data)
    if (payload.type === 'init') {
      mySessionId.value = payload.mySessionId
      participants.value = payload.participants || []
      applyingRemote.value = true
      form.title = payload.title
      form.markdownContent = payload.content
      form.baseVersion = payload.version
      applyingRemote.value = false
      return
    }

    if (payload.type === 'user_joined') {
      participants.value = upsertParticipant(payload.participant)
      return
    }

    if (payload.type === 'user_left') {
      participants.value = participants.value.filter(p => p.sessionId !== payload.participant?.sessionId)
      return
    }

    if (payload.type === 'cursor') {
      participants.value = upsertParticipant(payload.participant)
      return
    }

    if (payload.type === 'update_applied') {
      applyingRemote.value = true
      form.title = payload.title
      form.markdownContent = payload.content
      form.baseVersion = payload.version
      applyingRemote.value = false
      if (payload.autoMerged) {
        collabStatus.value = '已自动合并远端冲突'
      }
      return
    }

    if (payload.type === 'conflict') {
      conflict.visible = true
      conflict.message = payload.message || '协作冲突，请手动处理'
      conflict.serverContent = payload.serverContent || ''
      conflict.suggestedContent = payload.suggestedContent || ''
      conflict.serverVersion = payload.serverVersion || form.baseVersion
    }
  }
}

function disconnectCollab() {
  if (!ws.value) {
    return
  }
  try {
    sendWs({ type: 'leave' })
  } catch (e) {
    // ignore
  }
  ws.value.close()
  ws.value = null
  participants.value = []
  mySessionId.value = ''
}

function sendWs(payload) {
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify(payload))
  }
}

function upsertParticipant(participant) {
  const next = participants.value.slice()
  const index = next.findIndex(p => p.sessionId === participant.sessionId)
  if (index === -1) {
    next.push(participant)
  } else {
    next[index] = { ...next[index], ...participant }
  }
  return next
}

function reportCursor() {
  const el = editorRef.value
  if (!el) {
    return
  }
  sendWs({
    type: 'cursor',
    docId: String(docId.value),
    cursorStart: el.selectionStart,
    cursorEnd: el.selectionEnd
  })
}

function selectedText(p) {
  if (!form.markdownContent) {
    return ''
  }
  const start = Math.max(0, p.cursorStart || 0)
  const end = Math.max(start, p.cursorEnd || start)
  if (end === start) {
    return '无选区'
  }
  return form.markdownContent.slice(start, end).replace(/\n/g, ' ')
}

function useServerVersion() {
  form.markdownContent = conflict.serverContent
  form.baseVersion = conflict.serverVersion
  conflict.visible = false
}

function useSuggestedVersion() {
  form.markdownContent = conflict.suggestedContent
  form.baseVersion = conflict.serverVersion
  conflict.visible = false
}

function retryLocalVersion() {
  form.baseVersion = conflict.serverVersion
  conflict.visible = false
  sendWs({
    type: 'update',
    docId: String(docId.value),
    title: form.title,
    content: form.markdownContent,
    baseVersion: form.baseVersion
  })
}

async function openDoc(id) {
  await router.push(`/editor/${kbId.value}/${id}`)
}

async function createDoc() {
  const created = await docApi.create({
    kbId: kbId.value,
    title: '未命名文档',
    markdownContent: '# 新文档\n',
    visibility: 'PUBLIC',
    published: true
  })
  await loadTree()
  await router.push(`/editor/${kbId.value}/${created.id}`)
}

async function saveDoc() {
  if (!doc.value) {
    return
  }
  const updated = await docApi.update(doc.value.id, {
    title: form.title,
    markdownContent: form.markdownContent,
    visibility: form.visibility,
    baseVersion: form.baseVersion,
    commitMessage: '手动保存'
  })
  doc.value = updated
  form.baseVersion = updated.versionNo
  await loadTree()
}

async function saveDraft() {
  if (!doc.value) {
    return
  }
  await docApi.saveDraft(doc.value.id, {
    title: form.title,
    markdownContent: form.markdownContent
  })
}

async function openVersions() {
  if (!doc.value) {
    return
  }
  versions.value = await docApi.versions(doc.value.id)
  compare.left = ''
  compare.right = ''
  versionDiff.value = []
  showVersions.value = true
}

async function compareVersions() {
  if (!doc.value || !compare.left || !compare.right) {
    return
  }
  const data = await docApi.diffVersions(doc.value.id, compare.left, compare.right)
  versionDiff.value = data.lines || []
}

async function rollback(versionId) {
  if (!doc.value) {
    return
  }
  doc.value = await docApi.rollback(doc.value.id, versionId)
  form.title = doc.value.title
  form.markdownContent = doc.value.markdownContent
  form.visibility = doc.value.visibility
  form.baseVersion = doc.value.versionNo
  showVersions.value = false
  await loadTree()
}

async function createShare() {
  if (!doc.value) {
    return
  }
  const shared = await shareApi.create(doc.value.id)
  shareLink.value = shared.token
}

async function removeDoc() {
  if (!doc.value) {
    return
  }
  await docApi.delete(doc.value.id)
  doc.value = null
  disconnectCollab()
  await loadTree()
  await router.push(`/editor/${kbId.value}`)
}

async function search() {
  if (!keyword.value.trim()) {
    await loadTree()
    return
  }
  const result = await docApi.search(kbId.value, keyword.value.trim())
  tree.value = result.map(item => ({
    id: item.id,
    parentId: item.parentId,
    title: item.title,
    versionNo: item.versionNo
  }))
}
</script>

<style scoped>
.editor-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 12px;
  min-height: calc(100vh - 24px);
}

.sidebar {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.tree {
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.doc-item {
  justify-content: space-between;
  display: flex;
}

.doc-item.active {
  border-color: var(--brand);
}

.editor {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.toolbar {
  display: grid;
  grid-template-columns: 2fr 120px repeat(4, auto);
  gap: 8px;
}

.short {
  width: 120px;
}

.tip {
  margin: 0;
  color: var(--brand);
  font-size: 13px;
}

.collab {
  padding: 10px;
  border-radius: 10px;
}

.collab-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.collab-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.collab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 6px 8px;
  min-width: 210px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar.text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 700;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.selection {
  color: var(--muted);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-tip {
  margin: 0;
  color: var(--muted);
}

.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  min-height: 60vh;
}

.editor-input {
  width: 100%;
  min-height: 100%;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px;
  resize: vertical;
  background: var(--panel);
  color: var(--text);
}

.preview {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 12px;
  overflow: auto;
}

.empty {
  padding: 18px;
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: grid;
  place-items: center;
}

.versions {
  width: min(90vw, 640px);
  padding: 14px;
}

.version-list {
  max-height: 360px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.version-item {
  display: flex;
  justify-content: space-between;
}

.actions {
  display: flex;
  gap: 8px;
}

.compare {
  margin-bottom: 8px;
}

.diff-wrap {
  border: 1px solid var(--line);
  border-radius: 8px;
  max-height: 260px;
  overflow: auto;
  margin-bottom: 10px;
}

.diff-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 6px 8px;
  border-bottom: 1px solid var(--line);
}

.diff-row .left,
.diff-row .right {
  white-space: pre-wrap;
}

.diff-row.added {
  background: rgba(34, 197, 94, 0.12);
}

.diff-row.removed {
  background: rgba(239, 68, 68, 0.12);
}

.diff-row.changed {
  background: rgba(245, 158, 11, 0.15);
}

@media (max-width: 1080px) {
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .split {
    grid-template-columns: 1fr;
  }
}
</style>
