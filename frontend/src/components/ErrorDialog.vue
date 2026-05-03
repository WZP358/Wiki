<template>
  <transition name="error-fade">
    <div v-if="errorState.visible" class="overlay" @click.self="closeError">
      <section class="dialog" role="alertdialog" aria-modal="true" @keyup.esc="closeError">
        <header class="dialog-header">
          <div class="status-icon">!</div>
          <div>
            <p class="eyebrow">{{ errorState.code || 'UNKNOWN' }}</p>
            <h2>{{ errorState.title || '请求失败' }}</h2>
          </div>
          <button class="icon-btn" type="button" title="关闭" @click="closeError">×</button>
        </header>

        <p class="message">{{ errorState.message || '请求失败，请稍后重试' }}</p>

        <div v-if="errorState.request?.url" class="request-line">
          <span>{{ (errorState.request.method || 'GET').toUpperCase() }}</span>
          <code>{{ errorState.request.url }}</code>
          <b v-if="errorState.request.status">HTTP {{ errorState.request.status }}</b>
        </div>

        <div v-if="errorState.suggestion" class="hint">
          {{ errorState.suggestion }}
        </div>

        <details v-if="detailsText" class="details">
          <summary>技术详情</summary>
          <pre>{{ detailsText }}</pre>
        </details>

        <footer class="actions">
          <button class="ghost-btn" type="button" @click="copyAll">复制详情</button>
          <button class="primary-btn" type="button" @click="closeError">知道了</button>
        </footer>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { errorState, closeError, showToast } from '../utils/errorBus'

const detailsText = computed(() => String(errorState.details || errorState.raw || '').trim())

function handleKeydown(event) {
  if (event.key === 'Escape' && errorState.visible) {
    closeError()
  }
}

async function copyAll() {
  const parts = [
    errorState.title ? `标题：${errorState.title}` : '',
    `错误码：${errorState.code || 'UNKNOWN'}`,
    errorState.message ? `提示：${errorState.message}` : '',
    errorState.suggestion ? `建议：${errorState.suggestion}` : '',
    errorState.request?.url
      ? `请求：${String(errorState.request.method || '').toUpperCase()} ${errorState.request.url}${errorState.request.status ? ` (HTTP ${errorState.request.status})` : ''}`
      : '',
    detailsText.value ? `详情：\n${detailsText.value}` : ''
  ].filter(Boolean)
  await navigator.clipboard.writeText(parts.join('\n'))
  showToast({ title: '已复制', message: '错误详情已放入剪贴板', type: 'success' })
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 2500;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(6px);
}

.dialog {
  width: min(520px, 100%);
  border: 1px solid var(--line);
  border-radius: 10px;
  background: color-mix(in srgb, var(--panel) 96%, transparent);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
  padding: 20px;
}

.dialog-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.status-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  background: rgba(239, 68, 68, 0.12);
  color: var(--danger);
  font-weight: 800;
}

.eyebrow {
  margin: 0 0 2px;
  color: var(--danger);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.icon-btn {
  margin-left: auto;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 20px;
}

.icon-btn:hover {
  background: var(--line-light);
  color: var(--text);
}

.message {
  margin: 16px 0 0;
  color: var(--text);
  line-height: 1.65;
}

.request-line {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px;
  border-radius: 8px;
  background: var(--bg);
  border: 1px solid var(--line);
  color: var(--text-secondary);
}

.request-line span,
.request-line b {
  font-size: 12px;
  font-weight: 700;
}

.request-line code {
  max-width: 100%;
  overflow-wrap: anywhere;
}

.hint {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--brand-soft);
  color: var(--text);
  line-height: 1.55;
}

.details {
  margin-top: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--bg);
}

.details summary {
  padding: 10px 12px;
  cursor: pointer;
  color: var(--text-secondary);
  font-weight: 600;
}

.details pre {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  border-top: 1px solid var(--line);
  border-radius: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.ghost-btn,
.primary-btn {
  border-radius: 6px;
  border: 1px solid var(--line);
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 600;
}

.ghost-btn {
  background: var(--panel);
  color: var(--text-secondary);
}

.primary-btn {
  background: var(--brand);
  border-color: var(--brand);
  color: white;
}

.error-fade-enter-active,
.error-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.error-fade-enter-from,
.error-fade-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.98);
}
</style>
