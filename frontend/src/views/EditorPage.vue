<template>
  <div class="editor-layout">
    <!-- 左侧文档树和操作栏 -->
    <aside :class="['sidebar', { collapsed: sidebarCollapsed }]">
      <div class="sidebar-header">
        <h3 v-show="!sidebarCollapsed" class="sidebar-title">目录</h3>
        <button class="btn-icon collapse-btn" @click="toggleSidebar" :title="sidebarCollapsed ? '展开' : '收起'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path :d="sidebarCollapsed ? 'M9 18l6-6-6-6' : 'M15 18l-6-6 6-6'"/>
          </svg>
        </button>
      </div>

      <div v-show="!sidebarCollapsed" class="sidebar-content">
        <!-- 搜索框 -->
        <div class="search-box">
          <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
          <input v-model="keyword" class="search-input" placeholder="搜索文档..." @keyup.enter="search" />
        </div>

        <!-- 操作按钮组 -->
        <div class="action-buttons">
          <button class="action-btn" @click="createDoc" title="新建文档">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
            <span>新建文档</span>
          </button>
          <button v-if="doc" class="action-btn" @click="openVersions" title="历史版本">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <span>历史版本</span>
          </button>
          <button
            v-if="doc"
            class="action-btn"
            :disabled="uploadingImage || !editLock.locked"
            @click="chooseImage"
            title="插入图片"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="M21 15l-5-5L5 21" />
            </svg>
            <span>{{ uploadingImage ? '上传中' : '图片' }}</span>
          </button>
          <input
            ref="imageInputRef"
            class="hidden-file-input"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            @change="uploadEditorImage"
          />
          <button
            v-if="doc"
            class="action-btn"
            :disabled="saving || !editLock.locked"
            @click="triggerSave('button')"
            title="保存（Ctrl+S）"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
              <polyline points="17 21 17 13 7 13 7 21"/>
              <polyline points="7 3 7 8 15 8"/>
            </svg>
            <span>保存</span>
            <span class="kbd-hint">Ctrl+S</span>
          </button>
          <button v-if="doc" class="action-btn action-btn-primary" :disabled="!editLock.locked" @click="saveDoc" title="发布">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
              <polyline points="17 21 17 13 7 13 7 21"/>
              <polyline points="7 3 7 8 15 8"/>
            </svg>
            <span>发布</span>
          </button>
        </div>

        <!-- 文档树（可折叠） -->
        <nav class="doc-tree">
          <div class="doc-tree-header">
            <span class="tree-title">文档列表</span>
          </div>
          <div class="doc-tree-body">
            <div
              v-for="node in treeRoots"
              :key="node.id"
            >
              <TreeItem
                :node="node"
                :level="0"
                :active-id="String(docId)"
                :expanded-ids="expandedIds"
                @toggle="toggleNode"
                @open="openDoc"
              />
            </div>
          </div>
        </nav>
      </div>
    </aside>

    <!-- 主编辑区 -->
    <main v-if="doc" class="editor-main">
      <!-- 简化的顶部栏 -->
      <header class="editor-header">
        <div class="header-left">
          <span class="status-badge" :class="getStatusClass()">{{ getStatusText() }}</span>
          <span v-if="doc && !editLock.locked" class="status-badge status-offline">
            {{ editLock.owner ? `${editLock.owner} 正在编辑` : '未获得编辑锁' }}
          </span>
          <button
            v-if="doc && !editLock.locked && editLock.owner"
            class="nudge-btn"
            type="button"
            :disabled="nudgeCooldown > 0"
            @click="nudgeLockOwner"
          >
            {{ nudgeCooldown > 0 ? `${nudgeCooldown}s 后可再催` : '催一下' }}
          </button>
          <div class="collab-avatars" v-if="collaborators.length > 0">
            <div v-for="p in collaborators.slice(0, 3)" :key="p.sessionId" class="collab-avatar" :title="p.username">
              <img v-if="p.avatarUrl" :src="p.avatarUrl" alt="" />
              <span v-else>{{ p.username?.slice(0, 1) || '?' }}</span>
            </div>
            <span v-if="collaborators.length > 3" class="collab-more">+{{ collaborators.length - 3 }}</span>
          </div>
        </div>
        <div class="header-right">
          <div class="view-switch" role="group" aria-label="编辑视图">
            <button
              type="button"
              :class="{ active: editorMode === 'edit' }"
              @click="setEditorMode('edit')"
            >
              编辑
            </button>
            <button
              type="button"
              :class="{ active: editorMode === 'split' }"
              @click="setEditorMode('split')"
            >
              分屏
            </button>
            <button
              type="button"
              :class="{ active: editorMode === 'preview' }"
              @click="setEditorMode('preview')"
            >
              预览
            </button>
          </div>
          <button v-if="showVersions" type="button" class="btn-secondary compact" @click="closeVersions">返回编辑</button>
          <select v-model="form.visibility" class="visibility-select" :disabled="!editLock.locked">
            <option value="PUBLIC">公开</option>
            <option value="TEAM">团队可见</option>
            <option value="PRIVATE">私有</option>
          </select>
        </div>
      </header>

      <!-- 文档标题 -->
      <div class="doc-header">
        <input v-model="form.title" class="doc-title-input" placeholder="未命名文档" :readonly="!editLock.locked" />
        <div class="doc-meta">
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="8" r="4"/>
              <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/>
            </svg>
            {{ doc.ownerName || doc.ownerUsername || '未知作者' }}
          </span>
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
              <path d="M16 2v4M8 2v4M3 10h18"/>
            </svg>
            最后更新于 {{ formatDate(doc.updatedAt) }}
          </span>
          <span class="meta-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            {{ doc.viewCount || 0 }} 次阅读
          </span>
        </div>
      </div>

      <!-- 编辑器内容区 -->
      <div v-if="showVersions" class="version-workspace">
        <aside class="version-sidebar">
          <div class="version-panel-title">版本历史</div>
          <div class="version-compare compact-compare">
            <select v-model="compare.left" class="version-select">
              <option value="">左侧版本</option>
              <option v-for="v in versions" :key="'l-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
            </select>
            <select v-model="compare.right" class="version-select">
              <option value="">右侧版本</option>
              <option v-for="v in versions" :key="'r-' + v.id" :value="v.id">v{{ v.versionNo }}</option>
            </select>
            <button class="btn-primary compare-btn" @click="compareVersions">对比</button>
          </div>

          <div class="version-list full">
            <div
              v-for="v in versions"
              :key="v.id"
              class="version-item"
              :class="{ active: String(v.id) === String(compare.left) || String(v.id) === String(compare.right) }"
              @click="pickVersion(v)"
            >
              <div class="version-info">
                <span class="version-number">v{{ v.versionNo }}</span>
                <span class="version-author">{{ v.editorName || v.editorId }}</span>
              </div>
              <p class="version-message">{{ v.commitMessage || '更新文档' }}</p>
              <button class="version-rollback" type="button" @click.stop="rollback(v.id)">回滚到此版本</button>
            </div>
          </div>
        </aside>

        <section class="version-diff-panel">
          <div class="diff-panel-header">
            <div>
              <strong>{{ versionName(compare.left) || '左侧版本' }}</strong>
              <span>对比</span>
              <strong>{{ versionName(compare.right) || '右侧版本' }}</strong>
            </div>
          </div>

          <div v-if="versionDiff.length === 0" class="version-empty">
            请选择两个版本后点击“对比”。
          </div>
          <div v-else class="diff-split-view">
            <div class="diff-column">
              <div class="diff-column-title">{{ versionName(compare.left) }}</div>
              <code
                v-for="(line, idx) in versionDiff"
                :key="'l-' + idx"
                class="diff-cell"
                :class="line.type.toLowerCase()"
              >{{ line.left || ' ' }}</code>
            </div>
            <div class="diff-column">
              <div class="diff-column-title">{{ versionName(compare.right) }}</div>
              <code
                v-for="(line, idx) in versionDiff"
                :key="'r-' + idx"
                class="diff-cell"
                :class="line.type.toLowerCase()"
              >{{ line.right || ' ' }}</code>
            </div>
          </div>
        </section>
      </div>

      <div v-else class="editor-content" :class="`mode-${editorMode}`">
        <div v-show="editorMode !== 'preview'" class="editor-pane">
          <textarea
            ref="editorRef"
            v-model="form.markdownContent"
            class="markdown-editor"
            placeholder="输入 / 唤起快捷菜单..."
            :readonly="!editLock.locked"
            @click="reportCursor"
            @keyup="reportCursor"
            @select="reportCursor"
          ></textarea>
        </div>
        <div v-show="editorMode !== 'edit'" class="preview-pane">
          <article class="markdown-preview" v-html="previewHtml" @click="handlePreviewClick"></article>
        </div>
      </div>

      <!-- 保存提示 -->
      <div v-if="saveToast.visible" class="save-toast" :class="saveToast.type">
        <span class="save-toast-text">{{ saveToast.message }}</span>
        <button class="btn-text" @click="hideSaveToast">关闭</button>
      </div>
    </main>

    <!-- 空状态 -->
    <main v-else class="empty-state">
      <div class="empty-content">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
          </svg>
        </div>
        <h3>选择一个文档开始编辑</h3>
        <p>或者创建一个新文档</p>
        <button class="btn-primary" @click="createDoc">新建文档</button>
      </div>
    </main>


    <!-- 冲突处理弹窗 -->
    <div v-if="conflict.visible" class="modal-overlay" @click.self="conflict.visible = false">
      <div class="modal-dialog conflict-dialog">
        <div class="modal-header">
          <h3>检测到冲突</h3>
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
import { computed, defineComponent, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { docApi } from '../api/modules'
import { showToast } from '../utils/errorBus'

marked.setOptions({
  gfm: true,
  breaks: true
})

const route = useRoute()
const router = useRouter()

const kbId = computed(() => route.params.kbId)
const docId = computed(() => route.params.docId)

const sidebarCollapsed = ref(false)
const EDITOR_MODE_KEY = 'wiki.editorMode'

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('editor-sidebar-collapsed', String(sidebarCollapsed.value))
}

const tree = ref([])
const expandedIds = ref(new Set())
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
const editorRef = ref(null)
const imageInputRef = ref(null)
const showMoreMenu = ref(false)
const saving = ref(false)
const uploadingImage = ref(false)
const editorMode = ref(readEditorMode())
const editLock = reactive({
  locked: false,
  owner: '',
  message: ''
})
const saveToast = reactive({
  visible: false,
  message: '',
  type: 'info' // info | success | error
})
let saveToastTimer = null

const previewHtml = computed(() => makePreviewHtml(form.markdownContent || ''))

function makePreviewHtml(markdown) {
  let taskIndex = 0
  const renderer = new marked.Renderer()
  renderer.checkbox = ({ checked }) => {
    const index = taskIndex++
    return `<button type="button" class="task-checkbox${checked ? ' is-checked' : ''}" data-task-index="${index}" aria-pressed="${checked}" aria-label="切换任务状态">${checked ? '✓' : ''}</button>`
  }
  return marked.parse(markdown, { renderer })
}

function readEditorMode() {
  const saved = localStorage.getItem(EDITOR_MODE_KEY)
  return ['edit', 'split', 'preview'].includes(saved) ? saved : 'split'
}

function setEditorMode(mode) {
  showVersions.value = false
  editorMode.value = mode
  localStorage.setItem(EDITOR_MODE_KEY, mode)
  if (mode !== 'preview') {
    requestAnimationFrame(() => editorRef.value?.focus())
  }
}

// 将扁平列表转换为树形结构
const treeRoots = computed(() => {
  const byId = new Map()
  const roots = []
  const raw = tree.value || []

  raw.forEach(item => {
    byId.set(String(item.id), { ...item, children: [] })
  })

  byId.forEach(node => {
    const pid = node.parentId != null ? String(node.parentId) : null
    if (pid && byId.has(pid)) {
      byId.get(pid).children.push(node)
    } else {
      roots.push(node)
    }
  })

  // 默认展开第一层
  if (expandedIds.value.size === 0) {
    roots.forEach(n => expandedIds.value.add(String(n.id)))
  }

  return roots
})

const ws = ref(null)
const mySessionId = ref('')
const collabStatus = ref('未连接')
const participants = ref([])
const applyingRemote = ref(false)
const nudgeCooldown = ref(0)
const conflict = reactive({
  visible: false,
  message: '',
  serverContent: '',
  suggestedContent: '',
  serverVersion: 0
})
let draftTimer = null
let syncTimer = null
let nudgeTimer = null

const collaborators = computed(() => participants.value.filter(p => p.sessionId !== mySessionId.value))

onMounted(async () => {
  const saved = localStorage.getItem('editor-sidebar-collapsed')
  if (saved !== null) {
    sidebarCollapsed.value = saved === 'true'
  }
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
  window.addEventListener('keydown', handleGlobalKeydown, { capture: true })
})

watch(() => route.params.docId, async next => {
  if (!next) {
    await releaseEditLock()
    doc.value = null
    disconnectCollab()
    return
  }
  await loadDoc(next)
})

watch(() => route.params.kbId, async () => {
  await releaseEditLock()
  doc.value = null
  disconnectCollab()
  await loadTree()
  if (route.params.docId) {
    await loadDoc(route.params.docId)
  }
})

watch(
  () => [form.title, form.markdownContent],
  () => {
    // 正文保存由 Redis 编辑锁保护的保存接口负责；WebSocket 只保留在线状态和催办提醒。
    if (applyingRemote.value) return
  }
)

onBeforeUnmount(() => {
  clearInterval(draftTimer)
  if (syncTimer) {
    clearTimeout(syncTimer)
  }
  disconnectCollab()
  releaseEditLock()
  window.removeEventListener('beforeunload', beforeUnload)
  window.removeEventListener('keydown', handleGlobalKeydown, { capture: true })
  if (saveToastTimer) {
    clearTimeout(saveToastTimer)
    saveToastTimer = null
  }
  if (nudgeTimer) {
    clearInterval(nudgeTimer)
    nudgeTimer = null
  }
})

function beforeUnload() {
  disconnectCollab()
  releaseEditLock()
}

async function loadTree() {
  tree.value = await docApi.tree(kbId.value)
}

async function loadDoc(id) {
  disconnectCollab()
  await releaseEditLock()
  doc.value = await docApi.detail(id)
  form.title = doc.value.title
  form.markdownContent = doc.value.markdownContent
  form.visibility = doc.value.visibility
  form.baseVersion = doc.value.versionNo
  showVersions.value = false
  const draft = await docApi.getDraft(id)
  if (draft?.markdownContent) {
    form.markdownContent = draft.markdownContent
    form.title = draft.title || form.title
  }
  await acquireEditLock(id)
  connectCollab(id)
}

async function acquireEditLock(id) {
  editLock.locked = false
  editLock.owner = ''
  editLock.message = ''
  try {
    const result = await docApi.lock(id)
    editLock.locked = Boolean(result?.locked)
    editLock.owner = result?.owner || ''
    editLock.message = result?.message || ''
    if (!editLock.locked) {
      showSaveToast(`文档正在被 ${editLock.owner || '其他用户'} 编辑，当前仅建议查看`, 'error', 5000)
    }
  } catch (e) {
    editLock.locked = false
  }
}

async function releaseEditLock() {
  if (!doc.value?.id || !editLock.locked) {
    return
  }
  const id = doc.value.id
  editLock.locked = false
  try {
    await docApi.unlock(id)
  } catch (e) {
    // ignore unload/switch failures; the Redis TTL is the safety net.
  }
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
      if (payload.by?.username !== editLock.owner && payload.by?.username !== undefined) {
        return
      }
      if (payload.autoMerged) {
        collabStatus.value = '已自动合并远端冲突'
      }
      return
    }

    if (payload.type === 'conflict') {
      showSaveToast('文档已改为锁定编辑模式，请使用保存按钮提交内容', 'info', 3500)
      return
    }

    if (payload.type === 'nudge') {
      showSaveToast(`${payload.from || '其他用户'} 想编辑这篇文档，请完成后关闭或切换文档以释放编辑锁`, 'info', 8000)
      return
    }

    if (payload.type === 'nudge_sent') {
      showSaveToast(`已提醒 ${payload.to || editLock.owner}`, 'success', 2500)
      return
    }

    if (payload.type === 'nudge_unavailable') {
      showSaveToast(payload.message || '对方当前不在线，暂时无法提醒', 'error', 3500)
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

function handlePreviewClick(event) {
  const target = event.target instanceof Element ? event.target.closest('.task-checkbox') : null
  if (!(target instanceof HTMLElement)) {
    return
  }
  event.preventDefault()
  const taskIndex = Number(target.dataset.taskIndex)
  if (!Number.isInteger(taskIndex)) {
    return
  }
  const checked = target.getAttribute('aria-pressed') !== 'true'
  if (!editLock.locked) {
    showSaveToast(editLock.message || '未获得编辑锁，不能修改任务状态', 'error', 3500)
    return
  }
  if (toggleMarkdownTask(taskIndex, checked)) {
    showSaveToast('任务状态已同步到左侧 Markdown，保存后生效', 'success', 1800)
  }
}

function toggleMarkdownTask(taskIndex, checked) {
  let seen = -1
  let changed = false
  const lines = String(form.markdownContent || '').split('\n').map(line => {
    const match = line.match(/^(\s*[-*+]\s+\[)( |x|X)(\]\s+)/)
    if (!match) {
      return line
    }
    seen += 1
    if (seen !== taskIndex) {
      return line
    }
    changed = true
    return line.replace(/^(\s*[-*+]\s+\[)( |x|X)(\]\s+)/, `$1${checked ? 'x' : ' '}$3`)
  })
  if (changed) {
    form.markdownContent = lines.join('\n')
  }
  return changed
}

function nudgeLockOwner() {
  if (!doc.value?.id || !editLock.owner || nudgeCooldown.value > 0) {
    return
  }
  sendWs({
    type: 'nudge',
    docId: String(doc.value.id),
    to: editLock.owner
  })
  nudgeCooldown.value = 30
  if (nudgeTimer) {
    clearInterval(nudgeTimer)
  }
  nudgeTimer = setInterval(() => {
    nudgeCooldown.value -= 1
    if (nudgeCooldown.value <= 0) {
      clearInterval(nudgeTimer)
      nudgeTimer = null
      nudgeCooldown.value = 0
    }
  }, 1000)
}

function chooseImage() {
  if (!doc.value) {
    return
  }
  if (!editLock.locked) {
    showSaveToast(editLock.message || '未获得编辑锁，不能插入图片', 'error', 3500)
    return
  }
  imageInputRef.value?.click()
}

async function uploadEditorImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!editLock.locked) {
    showSaveToast(editLock.message || '未获得编辑锁，不能插入图片', 'error', 3500)
    return
  }
  uploadingImage.value = true
  showSaveToast('正在上传图片...', 'info', 0)
  try {
    const result = await docApi.uploadImage(file)
    insertMarkdownAtCursor(`![${imageAltText(file.name)}](${result.imageUrl})`)
    showSaveToast('图片已插入，保存后生效', 'success', 2600)
  } catch (e) {
    showSaveToast('图片上传失败，请检查文件格式和大小', 'error', 3500)
  } finally {
    uploadingImage.value = false
  }
}

function insertMarkdownAtCursor(markdown) {
  const el = editorRef.value
  const current = form.markdownContent || ''
  const insertion = needsLeadingBlankLine(current, el?.selectionStart || 0) ? `\n\n${markdown}\n\n` : `${markdown}\n\n`
  if (!el) {
    form.markdownContent = `${current}${insertion}`
    return
  }
  const start = el.selectionStart
  const end = el.selectionEnd
  form.markdownContent = current.slice(0, start) + insertion + current.slice(end)
  requestAnimationFrame(() => {
    el.focus()
    const next = start + insertion.length
    el.setSelectionRange(next, next)
  })
}

function needsLeadingBlankLine(content, index) {
  if (!content || index === 0) return false
  return !content.slice(0, index).endsWith('\n\n')
}

function imageAltText(filename) {
  return String(filename || '图片')
    .replace(/\.[^.]+$/, '')
    .replace(/[()[\]]/g, '')
    .trim() || '图片'
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
  if (!kbId.value) {
    showToast({ title: '请选择知识库', message: '先从左侧列表或首页选择一个知识库，再创建文档。', type: 'warning' })
    return
  }
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
  if (!editLock.locked) {
    showSaveToast(editLock.message || '未获得编辑锁，不能保存', 'error', 3500)
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

function showSaveToast(message, type = 'info', autoCloseMs = 2000) {
  saveToast.visible = true
  saveToast.message = message
  saveToast.type = type
  if (saveToastTimer) {
    clearTimeout(saveToastTimer)
    saveToastTimer = null
  }
  if (autoCloseMs > 0) {
    saveToastTimer = setTimeout(() => {
      saveToast.visible = false
      saveToastTimer = null
    }, autoCloseMs)
  }
}

function hideSaveToast() {
  saveToast.visible = false
  if (saveToastTimer) {
    clearTimeout(saveToastTimer)
    saveToastTimer = null
  }
}

async function triggerSave(source = 'manual') {
  if (saving.value) return
  if (!doc.value) return
  if (!editLock.locked) {
    showSaveToast(editLock.message || '未获得编辑锁，不能保存', 'error', 3500)
    return
  }
  saving.value = true
  showSaveToast('正在保存…', 'info', 0)
  try {
    await saveDoc()
    showSaveToast(source === 'hotkey' ? '已保存（Ctrl+S）' : '保存成功', 'success', 2000)
  } catch (e) {
    // 全局 http 拦截器会弹 ErrorDialog，这里补一个轻提示即可
    showSaveToast('保存失败，请检查错误提示', 'error', 3500)
  } finally {
    saving.value = false
  }
}

function handleGlobalKeydown(e) {
  // Ctrl+S / Cmd+S：拦截浏览器默认“保存网页”
  const key = (e.key || '').toLowerCase()
  if ((e.ctrlKey || e.metaKey) && key === 's') {
    e.preventDefault()
    triggerSave('hotkey')
  }
}

async function saveDraft() {
  if (!doc.value) {
    return
  }
  if (!editLock.locked) {
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
  compare.left = versions.value[1]?.id || versions.value[0]?.id || ''
  compare.right = versions.value[0]?.id || ''
  versionDiff.value = []
  showVersions.value = true
  if (compare.left && compare.right && String(compare.left) !== String(compare.right)) {
    await compareVersions()
  }
}

function closeVersions() {
  showVersions.value = false
}

function versionName(versionId) {
  const version = versions.value.find(item => String(item.id) === String(versionId))
  return version ? `v${version.versionNo}` : ''
}

function pickVersion(version) {
  if (!version?.id) return
  if (!compare.right || String(compare.right) === String(version.id)) {
    compare.right = version.id
    const previous = versions.value.find(item => item.versionNo === version.versionNo - 1)
    compare.left = previous?.id || compare.left || version.id
    return
  }
  compare.left = compare.right
  compare.right = version.id
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
    versionNo: item.versionNo,
    searchHighlightHtml: highlightSnippet(item.searchHighlight || item.title, keyword.value.trim())
  }))
}

function highlightSnippet(text, keywordText) {
  if (!text) return ''
  const escaped = escapeHtml(text)
  if (!keywordText) return escaped
  const escapedKeyword = escapeRegExp(escapeHtml(keywordText))
  return escaped.replace(new RegExp(`(${escapedKeyword})`, 'ig'), '<mark>$1</mark>')
}

function escapeHtml(text) {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function escapeRegExp(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function toggleNode(id) {
  const key = String(id)
  const next = new Set(expandedIds.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedIds.value = next
}

// 内联树节点组件：用 render 函数避免 runtime-only 构建下的 template 编译警告
const TreeItem = defineComponent({
  name: 'TreeItem',
  props: {
    node: { type: Object, required: true },
    level: { type: Number, required: true },
    activeId: { type: String, required: true },
    expandedIds: { type: Object, required: true }
  },
  emits: ['toggle', 'open'],
  setup(props, { emit }) {
    const isFolder = computed(() => (props.node?.children || []).length > 0)
    const isExpanded = computed(() => props.expandedIds.has(String(props.node.id)))

    const handleToggle = (e) => {
      e.stopPropagation()
      if (isFolder.value) {
        emit('toggle', props.node.id)
      } else {
        emit('open', props.node.id)
      }
    }

    const handleClickRow = () => {
      emit('open', props.node.id)
    }

    return () => {
      const paddingLeft = `${12 + props.level * 16}px`
      const row = h(
        'div',
        {
          class: ['doc-item', { active: String(props.node.id) === props.activeId }],
          style: { paddingLeft },
          onClick: handleClickRow
        },
        [
          isFolder.value
            ? h(
                'button',
                { class: 'tree-toggle-btn', onClick: handleToggle },
                [
                  h(
                    'svg',
                    { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' },
                    [h('path', { d: isExpanded.value ? 'M9 18l6-6-6-6' : 'M6 9l6 6 6-6' })]
                  )
                ]
              )
            : h('span', { class: 'tree-toggle-placeholder' }),
          h('span', { class: 'doc-title' }, props.node.title),
          props.node.searchHighlightHtml
            ? h('span', { class: 'doc-highlight', innerHTML: props.node.searchHighlightHtml })
            : null
        ]
      )

      const children =
        isFolder.value && isExpanded.value
          ? h(
              'div',
              { class: 'tree-children' },
              (props.node.children || []).map(child =>
                h(TreeItem, {
                  key: child.id,
                  node: child,
                  level: props.level + 1,
                  activeId: props.activeId,
                  expandedIds: props.expandedIds,
                  onToggle: id => emit('toggle', id),
                  onOpen: id => emit('open', id)
                })
              )
            )
          : null

      return h('div', {}, [row, children ? h('transition', { name: 'tree-collapse' }, { default: () => children }) : null])
    }
  }
})
</script>

<style scoped>
.editor-layout {
  display: flex;
  height: 100vh;
  background: var(--bg);
  overflow: hidden;
}

/* 左侧边栏 */
.sidebar {
  width: 280px;
  background: var(--panel);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.3s ease;
}

.sidebar.collapsed {
  width: 48px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--line);
  gap: 8px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar.collapsed .sidebar-title {
  display: none;
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
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
  flex-shrink: 0;
}

.btn-icon:hover {
  background: var(--line-light);
  color: var(--brand);
}

.collapse-btn svg {
  width: 16px;
  height: 16px;
}

.search-box {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  position: relative;
}

.search-icon {
  position: absolute;
  left: 28px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: var(--text-secondary);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
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

/* 操作按钮组 */
.action-buttons {
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--panel);
  color: var(--text);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  text-align: left;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hidden-file-input {
  display: none;
}

.kbd-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-secondary);
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 2px 6px;
  background: var(--bg);
}

.action-btn:hover {
  background: var(--line-light);
  border-color: var(--brand);
}

.action-btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--text-secondary);
}

.action-btn-primary {
  background: var(--brand);
  color: white;
  border-color: var(--brand);
}

.action-btn-primary:hover {
  background: var(--brand-hover);
  border-color: var(--brand-hover);
}

.action-btn-primary svg {
  color: white;
}

/* 文档树 */
.doc-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.doc-tree-body {
  padding-top: 4px;
}

.doc-tree-header {
  padding: 8px 12px;
  margin-bottom: 8px;
}

.tree-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.doc-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
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

.doc-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-highlight {
  display: block;
  flex-basis: 100%;
  margin-left: 24px;
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-highlight :deep(mark) {
  background: rgba(245, 158, 11, 0.22);
  color: inherit;
  padding: 0 2px;
  border-radius: 3px;
}

.tree-toggle-btn {
  width: 16px;
  height: 16px;
  border: none;
  padding: 0;
  margin-right: 2px;
  border-radius: 4px;
  background: transparent;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}

.tree-toggle-btn svg {
  width: 14px;
  height: 14px;
}

.tree-toggle-btn:hover {
  background: var(--line-light);
  color: var(--brand);
}

.tree-toggle-placeholder {
  width: 16px;
  height: 16px;
  margin-right: 2px;
  flex-shrink: 0;
}

.tree-children {
  margin-top: 2px;
}

.tree-collapse-enter-active,
.tree-collapse-leave-active {
  transition: all 0.18s ease-out;
}

.tree-collapse-enter-from,
.tree-collapse-leave-to {
  opacity: 0;
  transform: translateY(-2px);
}

/* 主编辑区 */
.editor-main {
  flex: 1;
  min-width: 0;
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
  min-height: 52px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
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

.nudge-btn {
  border: 1px solid var(--brand);
  background: var(--panel);
  color: var(--brand);
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
}

.nudge-btn:hover:not(:disabled) {
  background: var(--brand-soft);
}

.nudge-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-switch {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--panel);
  overflow: hidden;
}

.view-switch button {
  min-width: 48px;
  height: 30px;
  border: none;
  border-right: 1px solid var(--line);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
}

.view-switch button:last-child {
  border-right: none;
}

.view-switch button:hover {
  color: var(--brand);
  background: var(--line-light);
}

.view-switch button.active {
  color: #fff;
  background: var(--brand);
}

.btn-secondary.compact {
  height: 32px;
  padding: 0 12px;
  font-size: 13px;
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

.meta-item svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

/* 编辑器内容 */
.editor-content {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
}

.editor-content.mode-edit,
.editor-content.mode-preview {
  grid-template-columns: 1fr;
}

.editor-pane,
.preview-pane {
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 32px 80px;
}

.editor-pane {
  border-right: 1px solid var(--line);
}

.editor-content.mode-edit .editor-pane,
.editor-content.mode-preview .preview-pane {
  max-width: 980px;
  width: 100%;
  margin: 0 auto;
}

.editor-content.mode-edit .editor-pane {
  border-right: none;
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
  width: 100%;
  max-width: 100%;
  min-width: 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text);
  overflow-wrap: anywhere;
  word-break: break-word;
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

.markdown-preview :deep(.task-checkbox) {
  width: 16px;
  height: 16px;
  min-width: 16px;
  margin: 0 8px 0 0;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: -2px;
  background: var(--panel);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.markdown-preview :deep(.task-checkbox.is-checked) {
  border-color: var(--brand);
  background: var(--brand);
}

.markdown-preview :deep(li) {
  margin: 4px 0;
}

.markdown-preview :deep(img),
.markdown-preview :deep(video),
.markdown-preview :deep(canvas),
.markdown-preview :deep(svg) {
  display: block;
  max-width: 100%;
  height: auto;
  object-fit: contain;
}

.markdown-preview :deep(table) {
  width: 100%;
  max-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-preview code {
  background: var(--line-light);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 14px;
  white-space: break-spaces;
}

.markdown-preview pre {
  background: var(--line-light);
  padding: 16px;
  border-radius: 8px;
  max-width: 100%;
  overflow-x: hidden;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 16px 0;
}

.markdown-preview pre :deep(code) {
  white-space: inherit;
  word-break: inherit;
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

.save-toast {
  position: fixed;
  bottom: 84px;
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
  animation: slideUp 0.2s ease;
  z-index: 1500;
}

.save-toast.info {
  border-color: var(--line);
}

.save-toast.success {
  border-color: var(--success);
}

.save-toast.error {
  border-color: var(--danger, #ef4444);
}

.save-toast-text {
  color: var(--text);
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
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: var(--muted);
}

.empty-icon svg {
  width: 100%;
  height: 100%;
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

.version-workspace {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  border-top: 1px solid var(--line);
  overflow: hidden;
}

.version-sidebar {
  min-width: 0;
  padding: 18px;
  border-right: 1px solid var(--line);
  background: var(--bg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.version-panel-title {
  margin-bottom: 14px;
  color: var(--text);
  font-size: 18px;
  font-weight: 700;
}

.compact-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 14px;
}

.compare-btn {
  grid-column: 1 / -1;
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

.version-list.full {
  flex: 1;
  max-height: none;
  min-height: 0;
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

.version-item.active {
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

.version-rollback {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--brand);
  font-size: 13px;
  cursor: pointer;
}

.version-diff-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--panel);
  overflow: hidden;
}

.diff-panel-header {
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
  color: var(--text-secondary);
}

.diff-panel-header strong {
  color: var(--text);
}

.diff-panel-header span {
  margin: 0 8px;
}

.version-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

.diff-split-view {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  overflow: hidden;
}

.diff-column {
  min-width: 0;
  overflow: auto;
  border-right: 1px solid var(--line);
}

.diff-column:last-child {
  border-right: none;
}

.diff-column-title {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 10px 14px;
  border-bottom: 1px solid var(--line);
  background: var(--panel);
  color: var(--text);
  font-weight: 700;
}

.diff-cell {
  display: block;
  min-height: 30px;
  padding: 7px 14px;
  border-bottom: 1px solid var(--line-light);
  color: var(--text);
  background: var(--panel);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.diff-cell.added {
  background: rgba(34, 197, 94, 0.12);
}

.diff-cell.removed {
  background: rgba(239, 68, 68, 0.12);
}

.diff-cell.changed {
  background: rgba(245, 158, 11, 0.16);
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
