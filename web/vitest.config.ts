import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'

// Vitest 配置：jsdom 环境，复用 @ 别名
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.ts'],
  },
})
