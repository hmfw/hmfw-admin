import { createApp } from 'vue'
import '@hmfw/ant-design/style.css'
import '@hmfw/tiptap-editor/tiptap-editor.css'
import './style.css'
import App from './App.vue'
import router from './router/index.ts'

createApp(App).use(router).mount('#app')
