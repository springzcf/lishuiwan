<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import QrScanner from '../../components/QrScanner.vue'
import { api } from '../../api.js'
import { errorMessage, money, toast } from '../../ui.js'

const router = useRouter()
const products = ref([])
const productId = ref('')
const paidAmount = ref('')
const member = ref(null)
const memberCodeToken = ref('')
const staffPin = ref('')
const hasPin = ref(null)
const scanning = ref(false)
const submitting = ref(false)
const product = computed(() => products.value.find(item => String(item.id) === String(productId.value)))
function productChanged() { paidAmount.value = product.value?.currentPrice || ''; member.value = null; memberCodeToken.value = ''; staffPin.value = '' }
async function scanned(token) { scanning.value = false; try { const result = await api.post('/wx/staff/member-codes/parse', { token }); member.value = result.member; memberCodeToken.value = token } catch (error) { errorMessage(error) } }
async function submit() {
  if (!product.value) return toast('请先选择卡券类型')
  if (!member.value) return toast('请先扫描会员码')
  if (!/^\d{4,6}$/.test(staffPin.value)) return toast('请输入 4 至 6 位操作密码')
  const amount = Number(paidAmount.value)
  if (!Number.isFinite(amount) || amount <= 0) return toast('请输入正确的实收金额')
  if (!confirm(`确认现场发卡\n会员：${member.value.phone}\n卡券：${product.value.name}\n实收：¥${amount.toFixed(2)}`)) return
  submitting.value = true
  try { await api.post('/wx/staff/cash-orders', { requestNo: api.uuid(), memberCodeToken: memberCodeToken.value, productId: product.value.id, paidAmount: amount, staffPin: staffPin.value }); toast('发卡成功'); router.replace('/staff') } catch (error) { errorMessage(error) } finally { submitting.value = false; staffPin.value = '' }
}
onMounted(async () => { try { [products.value, hasPin.value] = await Promise.all([api.get('/wx/products'), api.get('/wx/staff/pin').then(value => value.hasPin)]) } catch (error) { errorMessage(error) } })
</script>

<template>
  <div class="page staff-page cash-page">
    <p class="flow-tip">请依次完成以下 3 步</p>
    <section class="staff-surface step-card"><div class="step-head"><i>1</i><span><b>选择卡券类型</b><small>确定本次需要发放的卡券</small></span></div><select v-model="productId" class="staff-field" @change="productChanged"><option value="" disabled>请选择卡券类型</option><option v-for="item in products" :key="item.id" :value="item.id">{{ item.name }} · ¥{{ money(item.currentPrice) }}</option></select><template v-if="product"><label class="staff-label">实收金额</label><input v-model="paidAmount" class="staff-field amount-input" inputmode="decimal"><p class="muted">如实收金额与售价不同，系统将记录差额。</p></template></section>
    <section :class="['staff-surface step-card',{ disabled: !product }]"><div class="step-head"><i>2</i><span><b>扫描会员码</b><small>扫描顾客个人中心的会员身份码</small></span></div><div v-if="member" class="member-result"><div class="avatar green">{{ member.nickname?.[0] || '会' }}</div><span><b>{{ member.nickname || '丽水湾会员' }}</b><small>{{ member.phone }}</small></span><em>已识别</em></div><button class="secondary-button" :disabled="!product" @click="scanning = true">{{ member ? '重新扫描会员码' : '扫描会员码' }}</button></section>
    <section :class="['staff-surface step-card',{ disabled: !member }]"><div class="step-head"><i>3</i><span><b>操作员授权</b><small>输入您本人的操作密码确认发卡</small></span></div><input v-if="hasPin" v-model="staffPin" class="staff-field" type="password" inputmode="numeric" maxlength="6" placeholder="请输入您的操作密码" :disabled="!member"><p v-else class="pin-empty">尚未设置操作密码，请联系后台管理员。</p></section>
    <button class="staff-primary" :disabled="!product || !member || !hasPin || !staffPin || submitting" @click="submit">{{ submitting ? '正在发卡…' : '确认收款并发卡' }}</button><p class="security-tip">操作密码仅用于本次服务端安全校验，不会保存在 H5 中。</p>
    <QrScanner v-if="scanning" @close="scanning = false" @result="scanned" />
  </div>
</template>
