<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api.js'
import { errorMessage } from '../ui.js'

const route = useRoute()
const router = useRouter()
const card = ref(null)
const records = ref([])
async function load() { try { const result = await api.get(`/wx/cards/${route.params.id}`); card.value = result.card; records.value = result.verifications || [] } catch (error) { errorMessage(error) } }
onMounted(load)
</script>

<template>
  <div v-if="card" class="page detail-page">
    <section class="hero card-detail-hero"><div class="eyebrow">MEMBERSHIP PASS · {{ card.id }}</div><h1>{{ card.productName || '会员权益卡' }}</h1><p>{{ card.validFrom }} 至 {{ card.validUntil }}</p>
      <button v-if="['unused','using'].includes(card.status)" class="code-entry" @click="router.push(`/cards/${card.id}/code`)"><span>▦</span><div><b>出示动态核销码</b><small>工作人员扫码后核销本卡权益</small></div><i>›</i></button>
      <div v-else class="code-disabled">该权益卡当前不可核销</div>
    </section>
    <h2 class="block-title">剩余权益</h2>
    <section class="surface"><div v-for="benefit in card.benefits" :key="benefit.benefitId" class="benefit-row"><span>{{ benefit.item }}</span><b>{{ benefit.type === 'unlimited' ? '不限次' : benefit.remaining + (benefit.type === 'hours' ? ' 小时' : ' 次') }}</b></div></section>
    <h2 class="block-title">使用记录</h2>
    <div v-if="!records.length" class="empty-state compact"><div>✓</div><b>尚未使用</b></div>
    <article v-for="record in records" :key="record.id" class="surface record"><div class="row"><b>{{ record.itemSnapshot }}</b><span class="danger">-{{ record.quantity }}</span></div><p>{{ record.createdAt }} · 核销人 {{ record.operatorName }}</p></article>
  </div>
</template>
