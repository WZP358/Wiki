<template>
  <div v-if="loadingState.count > 0" class="mask">
    <div class="panel box">加载中...</div>
  </div>
</template>

<script setup>
import { loadingState } from '../utils/loadingBus'
</script>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  display: grid;
  place-items: center;
  z-index: 1900;
}

.box {
  padding: 14px 18px;
  border-radius: 10px;
}
</style>
