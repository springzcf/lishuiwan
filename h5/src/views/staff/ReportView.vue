<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../../api.js'
import { errorMessage, money } from '../../ui.js'
const data = ref({})
onMounted(async () => { try { data.value = await api.get('/wx/staff/reports/summary') } catch (error) { errorMessage(error) } })
</script>

<template>
  <div class="page staff-page"><section class="staff-hero report-hero"><span>BUSINESS OVERVIEW</span><h1>门店经营速览</h1><p>实时统计已支付订单与核销记录</p></section><div class="metrics"><article class="staff-surface"><span>销售额</span><b>¥{{ money(data.sales) }}</b></article><article class="staff-surface"><span>售卡数</span><b>{{ data.orders || 0 }}</b></article><article class="staff-surface"><span>核销数</span><b>{{ data.verifications || 0 }}</b></article><article class="staff-surface"><span>会员数</span><b>{{ data.members || 0 }}</b></article></div><p class="muted">详细趋势、筛选与导出请使用 Web 管理端。</p></div>
</template>
