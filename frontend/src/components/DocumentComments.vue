<template>
  <el-card class="comments-section" shadow="never">
    <template #header>
      <div class="ry-card-header">
        <span>评论（{{ comments.length }}）</span>
        <el-button v-if="!showInput" type="primary" plain size="small" @click="showInput = true">写评论</el-button>
      </div>
    </template>

    <el-card v-if="showInput" shadow="never" class="comment-input-box">
      <el-input v-model="newComment" type="textarea" :rows="3" placeholder="写下你的评论..." />
      <div class="input-actions">
        <el-button @click="cancelComment">取消</el-button>
        <el-button type="primary" :disabled="!newComment.trim()" @click="submitComment">发表</el-button>
      </div>
    </el-card>

    <el-empty v-if="comments.length === 0" description="暂无评论，来发表第一条评论吧。" />

    <div v-else class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <el-avatar :size="36" :src="comment.authorAvatar">{{ comment.authorName?.charAt(0) }}</el-avatar>
        <div class="comment-content">
          <div class="comment-header">
            <strong>{{ comment.authorName }}</strong>
            <span>{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
          <div class="comment-actions">
            <el-button link type="primary" size="small" @click="replyTo(comment)">回复</el-button>
            <el-button v-if="canEdit(comment)" link type="primary" size="small" @click="editComment(comment)">编辑</el-button>
            <el-button v-if="canDelete(comment)" link type="danger" size="small" @click="deleteComment(comment)">删除</el-button>
            <el-button v-if="!comment.isResolved" link type="success" size="small" @click="resolveComment(comment)">标记为已解决</el-button>
          </div>

          <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <el-avatar :size="28" :src="reply.authorAvatar">{{ reply.authorName?.charAt(0) }}</el-avatar>
              <div class="comment-content">
                <div class="comment-header">
                  <strong>{{ reply.authorName }}</strong>
                  <span>{{ formatTime(reply.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ reply.content }}</div>
              </div>
            </div>
          </div>

          <el-card v-if="replyingTo === comment.id" shadow="never" class="reply-input-box">
            <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="写下你的回复..." />
            <div class="input-actions">
              <el-button @click="cancelReply">取消</el-button>
              <el-button type="primary" :disabled="!replyContent.trim()" @click="submitReply(comment)">回复</el-button>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { commentApi } from '../api/modules'
import { useAuthStore } from '../store/auth'
import { showToast } from '../utils/errorBus'

const props = defineProps({
  docId: { type: [String, Number], required: true }
})

const auth = useAuthStore()
const comments = ref([])
const showInput = ref(false)
const newComment = ref('')
const replyingTo = ref(null)
const replyContent = ref('')

onMounted(async () => {
  await loadComments()
})

async function loadComments() {
  try {
    comments.value = await commentApi.list(props.docId)
  } catch (err) {
    console.error('加载评论失败:', err)
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  try {
    await commentApi.create(props.docId, { content: newComment.value })
    newComment.value = ''
    showInput.value = false
    await loadComments()
  } catch (err) {
    console.error('发表评论失败:', err)
  }
}

function cancelComment() {
  newComment.value = ''
  showInput.value = false
}

function replyTo(comment) {
  replyingTo.value = comment.id
  replyContent.value = ''
}

function cancelReply() {
  replyingTo.value = null
  replyContent.value = ''
}

async function submitReply(comment) {
  if (!replyContent.value.trim()) return
  try {
    await commentApi.create(props.docId, {
      content: replyContent.value,
      parentId: comment.id
    })
    replyContent.value = ''
    replyingTo.value = null
    await loadComments()
  } catch (err) {
    console.error('回复失败:', err)
  }
}

async function editComment(comment) {
  const { value } = await ElMessageBox.prompt('修改评论内容后保存。', '编辑评论', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: comment.content,
    inputType: 'textarea',
    inputPattern: /\S+/,
    inputErrorMessage: '评论内容不能为空'
  }).catch(() => ({}))
  if (value && value !== comment.content) {
    try {
      await commentApi.update(props.docId, comment.id, { content: value })
      await loadComments()
      showToast({ title: '已保存', message: '评论已更新。', type: 'success' })
    } catch (err) {
      console.error('编辑评论失败:', err)
    }
  }
}

async function deleteComment(comment) {
  await ElMessageBox.confirm('删除后无法在页面中恢复。', '删除评论', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => Promise.reject(new Error('cancel')))
  try {
    await commentApi.delete(props.docId, comment.id)
    await loadComments()
    showToast({ title: '已删除', message: '评论已删除。', type: 'success' })
  } catch (err) {
    console.error('删除评论失败:', err)
  }
}

async function resolveComment(comment) {
  try {
    await commentApi.resolve(props.docId, comment.id)
    await loadComments()
  } catch (err) {
    console.error('标记失败:', err)
  }
}

function canEdit(comment) {
  return comment.authorId === auth.user?.id
}

function canDelete(comment) {
  return comment.authorId === auth.user?.id
}

function formatTime(date) {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`

  return d.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.comments-section {
  margin-top: 24px;
}

.comment-input-box,
.reply-input-box {
  margin-bottom: 18px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.comments-list {
  display: grid;
  gap: 22px;
}

.comment-item,
.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-header span {
  color: #909399;
  font-size: 12px;
}

.comment-text {
  color: var(--text);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.replies-list {
  margin-top: 14px;
  padding-left: 14px;
  border-left: 2px solid var(--line);
  display: grid;
  gap: 14px;
}

.reply-input-box {
  margin-top: 12px;
}
</style>
