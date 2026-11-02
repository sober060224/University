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
            },
            // 上传接口返回的是 /uploads/... 的相对路径，打包后由后端托管，
            // 开发时同样要走代理，否则所有已上传的图片都是 404
            '/uploads': {
                target: 'http://localhost:8088',
                changeOrigin: true
            }
        }
    },
    build: {
        // 直接产出到后端托管的静态目录：后端拦 /uploads/** 之外的路径时会回退到
        // classpath:/static/index.html，所以这份产物就是线上前端。原来默认输出
        // dist/ 再手工拷，结果仓库里长期躺着一份旧产物（旧 UserManageView 分页
        // 换算就是错的），而改源码根本不会让它更新。
        outDir: '../backend/src/main/resources/static',
        emptyOutDir: true,
        chunkSizeWarningLimit: 1500
    }
})
