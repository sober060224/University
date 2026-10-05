import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端开发时由 Vite 代理 /api 到后端 8088，浏览器视角同源，
// Session Cookie 与 CSRF 令牌都能正常工作，因此后端不需要开放 CORS。
export default defineConfig({
    plugins: [vue()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8088',
                changeOrigin: true
            }
        }
    },
    build: {
        outDir: 'dist',
        chunkSizeWarningLimit: 1500
    }
})
