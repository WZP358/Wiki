<template>
  <transition name="dialog-fade">
    <div v-if="confirmState.visible" class="overlay" @click.self="cancel">
      <section class="dialog" :class="confirmState.tone" role="dialog" aria-modal="true">
        <div class="icon-wrap" aria-hidden="true">
          <span v-if="confirmState.tone === 'danger'">!</span>
          <span v-else-if="confirmState.tone === 'warning'">!</span>
          <span v-else>i</span>
        </div>

        <div class="content">
          <h2>{{ confirmState.title }}</h2>
          <p v-if="confirmState.message">{{ confirmState.message }}</p>
          <input
            v-if="confirmState.input"
            v-model="confirmState.inputValue"
            class="dialog-input"
            :placeholder="confirmState.inputPlaceholder"
            autofocus
            @keyup.enter="confirm"
          />
        </div>

        <footer class="actions">
          <button class="ghost-btn" type="button" @click="cancel">{{ confirmState.cancelText }}</button>
          <button class="primary-btn" :class="confirmState.tone" type="button" @click="confirm">
            {{ confirmState.confirmText }}
          </button>
        </footer>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { confirmState, resolveConfirm } from '../utils/errorBus'

function cancel() {
  resolveConfirm(false)
}

function confirm() {
  resolveConfirm(true)
}

function handleKeydown(event) {
  if (!confirmState.visible) {
    return
  }
  if (event.key === 'Escape') {
    cancel()
  }
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
  z-index: 2600;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(15, 23, 42, 0.34);
  backdrop-filter: blur(6px);
}

.dialog {
  width: min(440px, 100%);
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 14px;
  padding: 20px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--panel) 96%, transparent);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
}

.icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: 700;
  background: var(--brand-soft);
  color: var(--brand);
}

.dialog.warning .icon-wrap {
  background: rgba(245, 158, 11, 0.14);
  color: var(--warning);
}

.dialog.danger .icon-wrap {
  background: rgba(239, 68, 68, 0.12);
  color: var(--danger);
}

.content h2 {
  margin: 0 0 6px;
  font-size: 17px;
}

.content p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.dialog-input {
  width: 100%;
  margin-top: 14px;
  padding: 9px 11px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--bg);
  color: var(--text);
  outline: none;
}

.dialog-input:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 4px;
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
  color: var(--text-secondary);
  background: var(--panel);
}

.primary-btn {
  color: white;
  background: var(--brand);
  border-color: var(--brand);
}

.primary-btn.danger {
  background: var(--danger);
  border-color: var(--danger);
}

.primary-btn.warning {
  background: var(--warning);
  border-color: var(--warning);
}

.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.98);
}
</style>
