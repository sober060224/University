import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 组件测试需要 DOM：utils/api/store 的纯逻辑用默认 node 也跑得动，
// 但 .vue 组件的竞态/在飞锁断言依赖挂载，统一用 jsdom。
export default defineConfig({
    plugins: [vue()],
    test: {
        environment: 'jsdom',
        include: ['src/**/*.test.js']
    }
})
