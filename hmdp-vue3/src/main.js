import { createApp } from 'vue'
import pinia from '@/stores'

import App from '@/App.vue'
import router from '@/router'
// 引入全局样式
import '@/assets/css/main.css'
// 引入 Element Plus 全量样式，确保 Loading 遮罩等样式可用
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')
