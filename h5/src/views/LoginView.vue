<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, isWechatBrowser, saveLogin } from '../api.js'
import { errorMessage } from '../ui.js'

const route = useRoute()
const router = useRouter()
const openId = ref(localStorage.getItem('h5_dev_openid') || 'local_customer')
const phone = ref(localStorage.getItem('h5_dev_phone') || '13800000000')
const loading = ref(false)
const oauthError = ref('')

async function login() {
  if (!api.isLocal) return
  loading.value = true
  try {
    localStorage.setItem('h5_dev_openid', openId.value)
    localStorage.setItem('h5_dev_phone', phone.value)
    const first = await api.publicPost('/public/wx/login', { loginCode: `dev:${openId.value}` })
    const result = first.registered ? first : await api.publicPost('/public/wx/phone', {
      registrationToken: first.registrationToken,
      phoneCode: `dev:${phone.value}`
    })
    saveLogin(result)
    router.replace(String(route.query.redirect || '/'))
  } catch (error) { errorMessage(error) } finally { loading.value = false }
}
function startWechatLogin() {
  if (!isWechatBrowser()) { oauthError.value = '请从丽水湾服务号菜单进入此页面'; return }
  loading.value = true
  const redirect = String(route.query.redirect || '/')
  const returnUrl = redirect === '/' ? '/h5/' : `/h5${redirect.startsWith('/') ? redirect : '/'}`
  window.location.assign(api.publicUrl(`/public/h5/wechat/authorize?returnUrl=${encodeURIComponent(returnUrl)}`))
}
onMounted(() => {
  if (localStorage.getItem('h5_token')) { router.replace(String(route.query.redirect || '/')); return }
  if (!api.isLocal && !isWechatBrowser()) { oauthError.value = '请从丽水湾服务号菜单进入此页面'; return }
  if (!api.isLocal && isWechatBrowser()) startWechatLogin()
})
</script>

<template>
  <div class="login-page">
    <div class="login-glow"></div>
    <div class="login-content">
      <div class="brand-mark">水</div>
      <div class="eyebrow">LISHUIWAN BATHHOUSE</div>
      <h1>丽水湾洗浴中心</h1>
      <p class="login-lead">登录后购买会员卡、出示会员码并查看权益</p>
      <div v-if="api.isLocal" class="login-form">
        <label>开发用户标识<input v-model.trim="openId" class="field" autocomplete="username"></label>
        <label>开发手机号<input v-model.trim="phone" class="field" inputmode="tel" maxlength="11" autocomplete="tel"></label>
        <button class="primary-button" :disabled="loading" @click="login">{{ loading ? '正在登录…' : '本地开发登录' }}</button>
        <p class="dev-tip">本地模式复用小程序开发账号，购买后将自动模拟支付成功。</p>
      </div>
      <div v-else class="production-login">
        <b>{{ loading ? '正在识别微信身份…' : '微信服务号自动登录' }}</b>
        <p>{{ oauthError || '从丽水湾服务号菜单进入后，将通过微信静默识别会员身份，无需输入账号密码。' }}</p>
        <button v-if="!loading" class="primary-button oauth-button" @click="startWechatLogin">重新识别微信身份</button>
      </div>
      <p class="privacy">登录即表示同意《用户服务与隐私保护说明》</p>
    </div>
  </div>
</template>
