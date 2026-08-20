<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { errorMessage, statusText } from '../ui.js'

const router = useRouter()
const cards = ref([])
const loading = ref(true)
async function load() { try { cards.value = await api.get('/wx/cards') } catch (error) { errorMessage(error) } finally { loading.value = false } }
onMounted(load)
</script>

<template>
  <div class="page cards-page">
    <div class="page-intro"><span class="eyebrow dark">MY MEMBERSHIP</span><h1>我的权益卡</h1><p>到店使用时，请出示卡片内的动态核销码</p></div>
    <div v-if="loading" class="skeleton-card tall"></div>
    <div v-else-if="!cards.length" class="empty-state"><div>◇</div><b>暂时没有权益卡</b><p>在活动页购买后会显示在这里</p><router-link to="/activity" class="small-button">去看看</router-link></div>
    <article v-for="card in cards" :key="card.id" class="membership-card" @click="router.push(`/cards/${card.id}`)">
      <div class="card-watermark">VIP</div>
      <div class="row card-head"><div><small>MEMBERSHIP · {{ card.id }}</small><h2>{{ card.productName || '会员权益卡' }}</h2></div><span class="gold-tag">{{ statusText(card.status) }}</span></div>
      <div v-for="benefit in card.benefits" :key="benefit.benefitId" class="membership-benefit"><span>{{ benefit.item }}</span><b>{{ benefit.type === 'unlimited' ? '不限次' : benefit.remaining + (benefit.type === 'hours' ? ' 小时' : ' 次') }}</b></div>
      <div class="row card-footer"><span>有效期至 {{ card.validUntil }}</span><button v-if="['unused','using'].includes(card.status)" @click.stop="router.push(`/cards/${card.id}/code`)">▦&nbsp; 核销码</button></div>
    </article>
  </div>
</template>
