<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import QrScanner from '../../components/QrScanner.vue'
import { api } from '../../api.js'
import { errorMessage, toast } from '../../ui.js'

const router = useRouter()
const member = ref(null)
const quantity = ref(Number(localStorage.getItem('h5_verification_quantity')) || 1)
const scanning = ref(false)
async function load() {
  try {
    const result = await api.get('/wx/member/profile')
    if (!['verifier', 'admin'].includes(result.staffRole)) { toast('无管理权限'); router.replace('/mine'); return }
    member.value = result
  } catch (error) { errorMessage(error) }
}
function setQuantity(value) { quantity.value = Math.max(1, Number(value) || 1); localStorage.setItem('h5_verification_quantity', quantity.value) }
async function scanned(token) {
  scanning.value = false
  try {
    const parsed = await api.post('/wx/staff/card-codes/parse', { token })
    sessionStorage.setItem('h5_pending_scan', JSON.stringify({ token, parsed, quantity: quantity.value }))
    router.push('/staff/verify')
  } catch (error) { errorMessage(error) }
}
onMounted(load)
</script>

<template>
  <div v-if="member" class="page staff-page">
    <section class="staff-hero"><span>STORE OPERATIONS</span><h1>现场工作台</h1><p><i></i>{{ member.nickname }} · {{ member.staffRole === 'admin' ? '管理员' : '核销员' }}</p></section>
    <div class="staff-section-head"><div><b>扫码核销</b><small>顾客到店使用权益</small></div><span>常用</span></div>
    <section class="staff-surface verify-panel"><div class="quantity-row"><div class="staff-meta"><i class="green">次</i><span><b>本次扣减</b><small>多人同行可一次扣减</small></span></div><div class="stepper"><button @click="setQuantity(quantity - 1)">−</button><input :value="quantity" inputmode="numeric" @input="setQuantity($event.target.value)"><button @click="setQuantity(quantity + 1)">＋</button></div></div><button class="scan-button" @click="scanning = true"><i>▦</i><span><b>扫描权益卡核销码</b><small>本次将核销 {{ quantity }} 次</small></span><em>›</em></button></section>
    <template v-if="member.staffRole === 'admin'"><div class="staff-section-head"><div><b>现场购卡</b><small>为顾客办理新卡</small></div></div><button class="staff-surface cash-entry" @click="router.push('/staff/cash-issue')"><i>卡</i><span><b>现场收款并发卡</b><small>选择卡券、扫描会员码、操作密码授权</small></span><em>办理 ›</em></button></template>
    <div class="staff-section-head"><div><b>管理工具</b><small>记录查询与经营数据</small></div></div><div class="staff-grid"><button class="staff-surface" @click="router.push('/staff/records')"><i>录</i><b>核销记录</b><small>查看当日与历史</small><em>›</em></button><button v-if="member.staffRole === 'admin'" class="staff-surface" @click="router.push('/staff/report')"><i class="green">数</i><b>数据速览</b><small>查看门店经营数据</small><em>›</em></button></div>
    <QrScanner v-if="scanning" @close="scanning = false" @result="scanned" />
  </div>
</template>
