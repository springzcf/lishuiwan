import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({plugins:[vue()],base:'/admin/',server:{port:5173,proxy:{'/api':'http://localhost:8080','/static':'http://localhost:8080'}},build:{sourcemap:false,chunkSizeWarningLimit:1000,rollupOptions:{output:{manualChunks:{vue:['vue','vue-router','pinia'],element:['element-plus'],charts:['echarts'],http:['axios']}}}}})
