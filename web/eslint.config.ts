import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

// Vue 3 + TypeScript flat config. `skipFormatting` must stay last so Prettier
// owns all stylistic rules and ESLint only reports correctness problems.
export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },
  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**', '**/node_modules/**']),
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  {
    // 路由页面（views）天然是单词命名，关掉多词组件名规则
    name: 'app/views-single-word',
    files: ['src/views/**/*.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },
  skipFormatting,
)
