<template>
  <transition name="toast-fade">
    <div v-if="toastState.visible" class="app-toast" :class="toastState.type">
      <div class="toast-dot" />
      <div class="toast-copy">
        <strong v-if="toastState.title">{{ toastState.title }}</strong>
        <span>{{ toastState.message }}</span>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { toastState } from '../utils/errorBus'
</script>

<style scoped>
.app-toast {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 3000;
  width: min(380px, calc(100vw - 32px));
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 13px 14px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--panel) 96%, transparent);
  color: var(--text);
  font-size: 14px;
  line-height: 1.45;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(8px);
}

.toast-dot {
  width: 9px;
  height: 9px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--brand);
  flex: 0 0 auto;
}

.toast-copy {
  display: grid;
  gap: 2px;
}

.toast-copy strong {
  font-size: 13px;
}

.toast-copy span {
  color: var(--text-secondary);
}

.app-toast.success .toast-dot {
  background: var(--success);
}

.app-toast.warning .toast-dot {
  background: var(--warning);
}

.app-toast.error .toast-dot {
  background: var(--danger);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
