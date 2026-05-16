import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const ADMIN_APP_URL = process.env.VITE_ADMIN_APP_URL || '/admin'
const API_BASE_URL = process.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function portalUrlsPlugin() {
  return {
    name: 'wiki-portal-urls',
    configureServer(server) {
      server.httpServer?.once('listening', () => {
        const address = server.httpServer.address()
        const port = typeof address === 'object' && address ? address.port : server.config.server.port
        setTimeout(() => {
          console.log('')
          console.log('  Wiki entry points:')
          console.log(`  - User portal:  http://localhost:${port}/`)
          console.log(`  - Admin portal: ${ADMIN_APP_URL}`)
          console.log(`  - Backend API:   ${API_BASE_URL}`)
          console.log('')
        }, 0)
      })
    }
  }
}

export default defineConfig({
  plugins: [vue(), portalUrlsPlugin()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true
      }
    }
  }
})
