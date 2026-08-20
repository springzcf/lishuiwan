<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, saveLogin } from '../api.js'

const route=useRoute()
const router=useRouter()
const error=ref('')

function appPath(returnUrl){
  const value=String(returnUrl||'/h5/activity')
  if(!value.startsWith('/h5/'))return '/activity'
  const path=value.slice(3)
  return path.startsWith('/')?path:'/activity'
}
async function exchange(){
  const ticket=String(route.query.ticket||'')
  if(!ticket){error.value='微信登录票据缺失';return}
  try{const result=await api.publicPost('/public/h5/wechat/exchange',{ticket});saveLogin(result);router.replace(appPath(route.query.returnUrl))}
  catch(e){error.value=e?.message||'微信登录失败，请重新进入'}
}
function retry(){router.replace({name:'login',query:{redirect:appPath(route.query.returnUrl)}})}
onMounted(exchange)
</script>

<template>
  <div class="oauth-callback-page">
    <div class="brand-mark">水</div>
    <h1>{{ error ? '登录未完成' : '正在登录' }}</h1>
    <p>{{ error || '正在安全交换微信身份，请稍候…' }}</p>
    <button v-if="error" class="primary-button" @click="retry">重新登录</button>
  </div>
</template>
