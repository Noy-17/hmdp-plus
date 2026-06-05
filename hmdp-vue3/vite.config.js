import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // vueDevTools(),
    AutoImport({
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // // 配置开发服务器相关选项
  // server: {
  //   // 自动打开浏览器
  //   open: true,
  //   // 指定服务器运行的端口号为1010
  //   // port: 1010,
  //   // 启用热模块替换（Hot Module Replacement，HMR）
  //   // 在开发过程中，修改代码时，浏览器可以实时更新而无需完全刷新页面
  //   hmr: true,
  //   proxy: {
  //     '/api': {
  //       // 目标服务器地址，这里是本地的另一个服务，运行在端口1011上
  //       target: 'http://localhost:8085',
  //       // 是否改变请求的源（Origin），设置为true时，会将请求的源修改为目标服务器的源
  //       changeOrigin: true,
  //       // 路径重写规则，将以/api开头的路径替换为空字符串
  //       // 例如，请求/api/users会被转发到目标服务器的/users路径
  //       rewrite: (path) => path.replace(/^\/api/, '')
  //       // 另一种常见的路径重写方式，效果与上面的rewrite函数相同
  //       // "^/api": "",
  //     }
  //   }
  // },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
