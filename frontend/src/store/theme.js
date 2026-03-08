import { defineStore } from 'pinia'

export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: localStorage.getItem('theme') || 'system'
  }),
  actions: {
    apply(mode = this.mode) {
      this.mode = mode
      localStorage.setItem('theme', mode)
      const root = document.documentElement
      if (mode === 'system') {
        root.removeAttribute('data-theme')
      } else {
        root.setAttribute('data-theme', mode)
      }
    }
  }
})
