<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../api.js'
import { errorMessage, toast } from '../../ui.js'

const router = useRouter()
const pending = ref(null)
const benefitIndex = ref(0)
const quantity = ref(1)
const submitting = ref(false)
const card = computed(() => pending.value?.parsed?.card)
const member = computed(() => pending.value?.parsed?.member)
const benefit = computed(() => card.value?.benefits?.[benefitIndex.value])
async function submit() {
  if (!benefit.value) return toast('没有可用权益')
  const count = Number(quantity.value)
  if (!Number.isFinite(count) || count <= 0) return toast('请输入正确的核销数量')
  const unit = benefit.value.type === 'hours' ? '小时' : '次'
  if (!confirm(`确认核销\n${card.value.productName || '权益卡'}\n${benefit.value.item} ${count}${unit}`)) return
  submitting.value = true
  try {
    await api.post('/wx/staff/verifications', { requestNo: api.uuid(), cardCodeToken: pending.value.token, cardId: card.value.id, benefitId: benefit.value.benefitId, quantity: count })
    sessionStorage.removeItem('h5_pending_scan'); localStorage.setItem('h5_verification_quantity', '1'); toast(`已核销 ${count} ${unit}`); router.replace('/staff')
  } catch (error) { errorMessage(error) } finally { submitting.value = false }
}
onMounted(() => { try { pending.value = JSON.parse(sessionStorage.getItem('h5_pending_scan') || 'null'); quantity.value = pending.value?.quantity || 1 } catch (_) {} if (!pending.value) router.replace('/staff') })
</script>

<template>
  <div v-if="pending" class="page staff-page">
    <section class="staff-surface customer-summary"><div class="avatar blue">{{ member.nickname?.[0] || '会' }}</div><div><b>{{ member.nickname }}</b><small>{{ member.phone }}</small></div><span>已识别</span></section>
    <h2 class="staff-block-title">本次核销卡</h2><section class="staff-surface"><div class="row"><b>{{ card.productName || '会员权益卡' }}</b><span>#{{ card.id }}</span></div><p class="muted">有效期至 {{ card.validUntil }}</p></section>
    <h2 class="staff-block-title">选择权益</h2><select v-model.number="benefitIndex" class="staff-field"><option v-for="(item,index) in card.benefits" :key="item.benefitId" :value="index">{{ item.item }} · 剩余 {{ item.type === 'unlimited' ? '不限' : item.remaining }}</option></select>
    <h2 class="staff-block-title">本次扣减</h2><input v-model="quantity" class="staff-field quantity-confirm" inputmode="decimal">
    <section class="staff-surface confirm-card"><div class="row"><span>本次扣减</span><b>{{ quantity }} {{ benefit?.type === 'hours' ? '小时' : '次' }}</b></div><div class="row"><span>扣减后</span><b>{{ benefit?.type === 'unlimited' ? '不限次' : Number(benefit?.remaining || 0) - Number(quantity || 0) }}</b></div></section>
    <button class="staff-primary" :disabled="submitting" @click="submit">{{ submitting ? '正在核销…' : `确认核销 ${quantity} ${benefit?.type === 'hours' ? '小时' : '次'}` }}</button>
  </div>
</template>
