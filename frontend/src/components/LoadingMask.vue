<template>
  <div v-if="loadingState.count > 0" class="mask">
    <div class="box">
      <span class="spinner" />
      <span>正在处理</span>
    </div>
  </div>
</template>

<script setup>
import { loadingState } from '../utils/loadingBus'
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.16);
  display: grid;
  place-items: center;
  z-index: 1900;
  pointer-events: none;
}

.box {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 15px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: color-mix(in srgb, var(--panel) 94%, transparent);
  color: var(--text-secondary);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(8px);
}

.spinner {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid var(--line);
  border-top-color: var(--brand);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
