<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api.js'
import { errorMessage, money } from '../ui.js'

const orders = ref([])
const loading = ref(true)
async function load() { try { orders.value = await api.get('/wx/orders') } catch (error) { errorMessage(error) } finally { loading.value = false } }
onMounted(load)
</script>

<template>
  <div class="page list-page">
    <div v-if="loading" class="skeleton-card"></div>
    <div v-else-if="!orders.length" class="empty-state"><div>单</div><b>暂无订单</b></div>
    <article v-for="order in orders" :key="order.id" class="surface order-card">
      <div class="row"><b>{{ order.item?.name || (order.payMethod === 'cash' ? '现金购卡' : '微信购卡') }}</b><span class="tag">{{ order.payStatus === 0 ? '待支付' : order.payStatus === 1 ? '已支付' : '已关闭' }}</span></div>
      <strong class="order-amount">¥{{ money(order.paidAmount || order.payableAmount) }}</strong>
      <p>{{ order.payMethod === 'cash' ? '现金购买' : '微信支付' }} · 订单号 {{ order.orderNo }}</p><p>{{ order.paidAt || order.createdAt }}</p>
    </article>
  </div>
</template>
