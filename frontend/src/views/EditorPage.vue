<template>
  <div class="editor-layout">
    <!-- 左侧文档树 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">目录</h3>
        <button class="btn-icon" @click="createDoc" title="新建文档">+</button>
      </div>
      <div class="search-box">
        <input v-model="keyword" class="search-input" placeholder="搜索文档..." @keyup.enter="search" />
      </div>
      <nav class="doc-tree">
        <div
          v-for="node in tree"
          :key="node.id"
          class="doc-item"
          :class="{ active: String(node.id) === String(docId) }"
          @click="openDoc(node.id)"
        >
          <span class="doc-icon">📄</span>
          <span class="doc-title">{{ node.title }}</span>
        </div>
      </nav>
    </aside>

    <!-- 主编辑区 -->
    <main v-if="doc" class="editor-main">
      <!-- 顶部导航栏 -->
      <header class="editor-header">
        <div class="header-left">
          <div class="breadcrumb">
            <span class="breadcrumb-item">研发部</span>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-item current">{{ form.title || '未命名文档' }}</span>
          </div>
          <span class="status-badge" :class="getStatusClass()">{{ getStatusText() }}</span>
        </div>
        <div class="header-right">
          <div class="collab-avatars" v-if="collaborators.length > 0">
            <div v-for="p in collaborators.slice(0, 3)" :key="p.sessionId" class="collab-avatar" :title="p.username">
              <img v-if="p.avatarUrl" :src="p.avatarUrl" alt="" />
              <span v-else>{{ p.username?.slice(0, 1) || '?' }}</span>
            </div>
            <span v-if="collaborators.length > 3" class="collab-more">+{{ collaborators.length - 3 }}</span>
          </div>
          <select v-model="form.visibility" class="visibility-select">
            <option value="PUBLIC">📖 公开</option>
            <option value="TEAM">👥 团队</option>
            <option value="PRIVATE">🔒 私有</option>
          </select>
          <button class="btn-secondary" @click="openVersions">历史版本</button>
          <button class="btn-secondary" @click="createShare">分享</button>
          <button class="btn-primary" @click="saveDoc">发布</button>
          <button class="btn-icon" @click="showMoreMenu = !showMoreMenu">⋯</button>
        </div>
      </header>

      <!-- 文档标题 -->
      <div class="doc-header">
        <input v-model="form.title" class="doc-title-input" placeholder="未命名文档" />
        <div class="doc-meta">
          <span class="meta-item">👤 {{ doc.author || '王经理' }}</span>
          <span class="meta-item">📅 最后更新于 {{ formatDate(doc.updatedAt) }}</span>
          <span class="meta-item">👁️ {{ doc.viewCount || 8 }} 次阅读</span>
        </div>
      </div>

      <!-- 编辑器内容区 -->
      <div class="editor-content">
        <div class="editor-pane">
          <textarea
            ref="editorRef"
            v-model="form.markdownContent"
            class="markdown-editor"
            placeholder="输入 / 唤起快捷菜单..."
            @click="reportCursor"
            @keyup="reportCursor"
            @select="reportCursor"
          ></textarea>
        </div>
        <div class="preview-pane">
          <article class="markdown-preview" v-html="previewHtml"></article>
        </div>
      </div>

      <!-- 分享提示 -->
      <div v-if="shareLink" class="share-toast">
        <span>✓ 分享链接已生成：{{ location.origin }}/share/{{ shareLink }}</span>
        <button class="btn-text" @click="shareLink = ''">关闭</button>
      </div>
    </main>

    <!-- 空状态 -->
    <main v-else class="empty-state">
      <div class="empty-content">
        <div class="empty-icon">📝</div>
        <h3>选择一个文档开始编辑</h3>
        <p>或者创建一个新文档</p>
        <button class="btn-primary" @click="createDoc">新建文档</button>
      </div>
    </main>


    <!-- 版本历史弹窗 -->
    <div v-if="showVersions" class="modal-overlay" @click.self="showVersions = false">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3>版本历史</h3>
          <button class="btn-icon" @click="showVersions = false">×</button>
        </div>
        <div class="modal-body">
          <div class="version-compare">
            <select v-model="compare.left" class="version-select">
              <option value="">选择左侧版本</option>
              <option v-for="v in versions" :key="'l-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
            </select>
            <button class="btn-secondary" @click="compareVersions">对比</button>
            <select v-model="compare.right" class="version-select">
              <option value="">选择右侧版本</option>
              <option v-for="v in versions" :key="'r-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
            </select>
          </div>
          <div class="version-list">
            <div v-for="v in versions" :key="v.id" class="version-item" @click="rollback(v.id)">
              <div class="version-info">
                <span class="version-number">v{{ v.versionNo }}</span>
                <span class="version-author">{{ v.editorName || v.editorId }}</span>
              </div>
              <p class="version-message">{{ v.commitMessage || '更新文档' }}</p>
            </div>
          </div>
          <div v-if="versionDiff.length > 0" class="diff-viewer">
            <div v-for="(line, idx) in versionDiff" :key="idx" class="diff-line" :class="line.type.toLowerCase()">
              <code class="diff-left">{{ line.left }}</code>
              <code class="diff-right">{{ line.right }}</code>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 冲突处理弹窗 -->
    <div v-if="conflict.visible" class="modal-overlay" @click.self="conflict.visible = false">
      <div class="modal-dialog conflict-dialog">
        <div class="modal-header">
          <h3>⚠️ 检测到冲突</h3>
          <button class="btn-icon" @click="conflict.visible = false">×</button>
        </div>
        <div class="modal-body">
          <p class="conflict-message">{{ conflict.message }}</p>
          <div class="conflict-actions">
            <button class="btn-secondary" @click="useServerVersion">使用远端版本</button>
            <button class="btn-secondary" @click="useSuggestedVersion">插入冲突标记</button>
            <button class="btn-primary" @click="retryLocalVersion">保留本地并重试</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { docApi, shareApi } from '../api/modules'

const route = useRoute()
const router = useRouter()

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
const showMoreMenu = ref(false)

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

function getStatusClass() {
  return collabStatus.value === '已连接' ? 'status-online' : 'status-offline'
}

function getStatusText() {
  return collabStatus.value === '已连接' ? 'DRAFT' : '离线'
}

function formatDate(date) {
  if (!date) return '今天'
  const d = new Date(date)
  const now = new Date()
  const diff = Math.floor((now - d) / (1000 * 60 * 60 * 24))
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  return d.toLocaleDateString('zh-CN')
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
.editor-layout {
  display: flex;
  height: 100vh;
  background: var(--bg);
}

/* 左侧边栏 */
.sidebar {
  width: 260px;
  background: var(--panel);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
}

.btn-icon {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 18px;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: var(--line-light);
  color: var(--brand);
}

.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  font-size: 13px;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: var(--brand);
  background: var(--panel);
}

.doc-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 2px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.doc-item:hover {
  background: var(--line-light);
}

.doc-item.active {
  background: var(--brand-light);
  color: var(--brand);
}

.doc-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.doc-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主编辑区 */
.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--panel);
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.breadcrumb-item {
  transition: color 0.2s;
}

.breadcrumb-item:hover {
  color: var(--brand);
  cursor: pointer;
}

.breadcrumb-item.current {
  color: var(--text);
  font-weight: 500;
}

.breadcrumb-sep {
  color: var(--muted);
}

.status-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.status-online {
  background: var(--brand-soft);
  color: var(--brand);
}

.status-badge.status-offline {
  background: var(--line-light);
  color: var(--muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collab-avatars {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
}

.collab-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  border: 2px solid var(--panel);
  overflow: hidden;
}

.collab-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.collab-more {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.visibility-select {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  font-size: 13px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

.visibility-select:hover {
  border-color: var(--brand);
}

.btn-primary,
.btn-secondary {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  outline: none;
}

.btn-primary {
  background: var(--brand);
  color: white;
}

.btn-primary:hover {
  background: var(--brand-hover);
}

.btn-secondary {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--line);
}

.btn-secondary:hover {
  background: var(--line-light);
  color: var(--text);
  border-color: var(--brand);
}

/* 文档头部 */
.doc-header {
  padding: 32px 80px 24px;
  border-bottom: 1px solid var(--line);
}

.doc-title-input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 32px;
  font-weight: 700;
  color: var(--text);
  outline: none;
  padding: 0;
  margin-bottom: 16px;
}

.doc-title-input::placeholder {
  color: var(--muted);
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 编辑器内容 */
.editor-content {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
}

.editor-pane,
.preview-pane {
  overflow-y: auto;
  padding: 32px 80px;
}

.editor-pane {
  border-right: 1px solid var(--line);
}

.markdown-editor {
  width: 100%;
  min-height: 100%;
  border: none;
  background: transparent;
  color: var(--text);
  font-size: 15px;
  line-height: 1.8;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  resize: none;
  outline: none;
}

.markdown-editor::placeholder {
  color: var(--muted);
}

.markdown-preview {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
}

.markdown-preview h1 {
  font-size: 28px;
  margin: 24px 0 16px;
}

.markdown-preview h2 {
  font-size: 22px;
  margin: 20px 0 12px;
}

.markdown-preview h3 {
  font-size: 18px;
  margin: 16px 0 10px;
}

.markdown-preview p {
  margin: 12px 0;
}

.markdown-preview code {
  background: var(--line-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 14px;
}

.markdown-preview pre {
  background: var(--line-light);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

/* 分享提示 */
.share-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: var(--shadow-lg);
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.btn-text {
  background: none;
  border: none;
  color: var(--brand);
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
}

.btn-text:hover {
  text-decoration: underline;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--panel);
}

.empty-content {
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-content h3 {
  font-size: 18px;
  color: var(--text);
  margin-bottom: 8px;
}

.empty-content p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-dialog {
  background: var(--panel);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  width: 90%;
  max-width: 700px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

.version-compare {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.version-select {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  font-size: 13px;
  outline: none;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.version-item {
  padding: 12px 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.version-item:hover {
  border-color: var(--brand);
  background: var(--brand-light);
}

.version-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.version-number {
  font-weight: 600;
  color: var(--brand);
}

.version-author {
  font-size: 13px;
  color: var(--text-secondary);
}

.version-message {
  font-size: 13px;
  color: var(--text);
  margin: 0;
}

.diff-viewer {
  margin-top: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  max-height: 400px;
  overflow-y: auto;
}

.diff-line {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--line);
}

.diff-left,
.diff-right {
  padding: 8px 12px;
  background: var(--panel);
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-line.added {
  background: rgba(34, 197, 94, 0.1);
}

.diff-line.removed {
  background: rgba(239, 68, 68, 0.1);
}

.diff-line.changed {
  background: rgba(245, 158, 11, 0.1);
}

.conflict-dialog {
  max-width: 500px;
}

.conflict-message {
  font-size: 14px;
  color: var(--text);
  margin-bottom: 20px;
}

.conflict-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

@media (max-width: 1200px) {
  .editor-content {
    grid-template-columns: 1fr;
  }

  .editor-pane {
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .doc-header,
  .editor-pane,
  .preview-pane {
    padding: 24px 40px;
  }
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -260px;
    top: 0;
    bottom: 0;
    z-index: 100;
    transition: left 0.3s;
  }

  .sidebar.open {
    left: 0;
  }

  .doc-header,
  .editor-pane,
  .preview-pane {
    padding: 20px 24px;
  }

  .header-right {
    flex-wrap: wrap;
  }
}
</style>
