import { reactive } from 'vue'

export const loadingState = reactive({
  count: 0
})

export function startLoading() {
  loadingState.count += 1
}

export function stopLoading() {
  loadingState.count = Math.max(0, loadingState.count - 1)
}
