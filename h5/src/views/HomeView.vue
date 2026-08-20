<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { errorMessage, money, toast } from '../ui.js'

const router = useRouter()
const activities = ref([])
const products = ref([])
const selected = ref(null)
const loading = ref(true)
const buying = ref(false)

async function load() {
  try {
    const result = await api.get('/wx/home')
    activities.value = (result.activities || []).map(item => ({ ...item, image: api.asset(item.image) }))
    products.value = (result.products || []).map(item => ({ ...item, cover: api.asset(item.cover) }))
  } catch (error) { errorMessage(error) } finally { loading.value = false }
}
async function buy() {
  if (!localStorage.getItem('h5_token')) {
    router.push({ name: 'login', query: { redirect: '/activity' } })
    return
  }
  buying.value = true
  try {
    const order = await api.post('/wx/orders', { productId: selected.value.id, requestNo: api.uuid() })
    if (api.mockPayment) {
      await api.post(`/wx/orders/${order.orderNo}/mock-pay`, { requestNo: api.uuid() })
    } else {
      const payment = await api.post(`/wx/orders/${order.orderNo}/prepay`, { channel: 'official_account' })
      await payInWechat(payment)
    }
    toast('购买成功')
    selected.value = null
    router.push('/cards')
  } catch (error) { errorMessage(error) } finally { buying.value = false }
}
function payInWechat(params) {
  return new Promise((resolve, reject) => {
    if (!window.WeixinJSBridge) return reject(new Error('请在微信内打开页面完成支付'))
    window.WeixinJSBridge.invoke('getBrandWCPayRequest', params, result => {
      result.err_msg === 'get_brand_wcpay_request:ok' ? resolve() : reject(new Error('支付未完成'))
    })
  })
}
onMounted(load)
</script>

<template>
  <div class="page home-page">
    <section class="hero home-hero">
      <div class="eyebrow">LISHUIWAN BATHHOUSE</div>
      <h1>丽水湾洗浴中心</h1>
      <p>安心洗浴 · 舒适相伴</p>
    </section>
    <div v-if="activities.length" class="banner-strip">
      <img v-for="item in activities" :key="item.id" :src="item.image" :alt="item.title || '丽水湾活动'">
    </div>
    <div class="section-heading"><div><span>会员权益卡</span><small>精选门店权益</small></div><i>MEMBERSHIP</i></div>
    <div v-if="loading" class="skeleton-list"><div v-for="n in 2" :key="n" class="skeleton-card"></div></div>
    <button v-for="product in products" :key="product.id" class="product-card" @click="selected = product">
      <img :src="product.cover" :alt="product.name">
      <div class="product-main">
        <div class="row"><b>{{ product.name }}</b><span class="tag">{{ product.category }}</span></div>
        <p>购买后 {{ product.validDays }} 天内有效</p>
        <div class="row price-line"><span><strong>¥{{ money(product.currentPrice) }}</strong><del>¥{{ money(product.price) }}</del></span><i>›</i></div>
      </div>
    </button>
    <div v-if="!loading && !products.length" class="empty">暂无在售权益卡</div>

    <div v-if="selected" class="modal-mask" @click.self="selected = null">
      <section class="bottom-sheet">
        <div class="sheet-handle"></div>
        <button class="close-button sheet-close" @click="selected = null">×</button>
        <span class="eyebrow dark">MEMBERSHIP PASS</span>
        <h2>{{ selected.name }}</h2>
        <div class="sheet-price">¥{{ money(selected.currentPrice) }}</div>
        <div v-for="benefit in selected.benefits" :key="benefit.benefitId" class="benefit-row">
          <span><i>✓</i>{{ benefit.item }}</span>
          <b>{{ benefit.type === 'unlimited' ? '不限次' : benefit.total + (benefit.type === 'hours' ? ' 小时' : ' 次') }}</b>
        </div>
        <p class="rules">{{ selected.rules }}</p>
        <button class="primary-button" :disabled="buying" @click="buy">{{ buying ? '正在处理…' : '立即购买' }}</button>
      </section>
    </div>
  </div>
</template>
