import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

// Vitest 配置：jsdom 环境，复用 @ 别名
export default defineConfig({
  plugins: [vue()],
  define: {
    'import.meta.env.DEV': 'false',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.ts'],
    server: {
      deps: {
        inline: [/@hmfw\/ant-design/],
      },
    },
  },
})
