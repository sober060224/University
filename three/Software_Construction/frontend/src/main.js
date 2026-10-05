import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/main.css'
import 'leaflet/dist/leaflet.css'

// pinia 必须先于 router 安装：路由守卫里会用 useUserStore()
createApp(App)
    .use(createPinia())
    .use(router)
    .mount('#app')
