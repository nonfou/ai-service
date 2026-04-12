import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.VITE_PORT || 5174)

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return
            }

            if (id.includes('zrender')) {
              return 'vendor-zrender'
            }

            if (id.includes('echarts')) {
              return 'vendor-echarts'
            }

            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'vendor-element-plus'
            }

            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
              return 'vendor-vue'
            }
          }
        }
      }
    },
    server: {
      port
    }
  }
})
