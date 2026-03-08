<template>
  <div v-if="errorState.visible" class="overlay" @click.self="closeError">
    <div class="dialog panel">
      <h3>请求失败</h3>
      <p>{{ errorState.message }}</p>
      <div class="code">错误码：{{ errorState.code }}</div>
      <div class="actions">
        <button class="btn" @click="copyCode">复制错误码</button>
        <button class="btn btn-primary" @click="closeError">知道了</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { errorState, closeError } from '../utils/errorBus'

function copyCode() {
  navigator.clipboard.writeText(errorState.code || 'UNKNOWN')
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog {
  width: min(92vw, 460px);
  padding: 20px;
}

.dialog h3 {
  margin: 0 0 8px;
}

.dialog p {
  margin: 0;
  color: var(--muted);
}

.code {
  margin-top: 12px;
  padding: 10px;
  border-radius: 8px;
  background: var(--bg);
  border: 1px solid var(--line);
}

.actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
