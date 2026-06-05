import { defineConfig, globalIgnores } from 'eslint/config'
import globals from 'globals'
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'

export default defineConfig([
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}']
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ElMessage: 'readonly',
        ElMessageBox: 'readonly',
        ElLoading: 'readonly'
      }
    }
  },
  {
    rules: {
      // 使用插件提供的规则（格式：`插件名/规则名`）
      'vue/multi-word-component-names': [
        'warn',
        {
          ignores: ['index'] // vue组件名称多单词组成（忽略index.vue）
        }
      ],
      'vue/no-setup-props-destructure': ['off'], // 关闭 props 解构的校验
      // 💡 添加未定义变量错误提示，create-vue@3.6.3 关闭，这里加上是为了支持下一个章节演示。
      'no-undef': 'off',
      // 新增规则
      'space-before-function-paren': ['error', 'always'] // ← 添加在此处
      // // 禁用自动导入规则
      // 'import/named': 'off',
      // 'import/namespace': 'off',
      // // 禁用未使用变量的自动删除（如果不需要）
      // 'no-unused-vars': 'off',
      // // 如果使用 TypeScript 相关规则，也需禁用
      // '@typescript-eslint/no-unused-vars': 'off'
    }
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  skipFormatting,
  eslintPluginPrettierRecommended
])
