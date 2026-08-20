<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../../api.js'
import { errorMessage } from '../../ui.js'
const items = ref([])
async function load() { try { items.value = (await api.get('/wx/staff/verifications')).map(item => ({ ...item, displayTime: String(item.createdAt || '').replace('T', ' ').slice(0, 19) })) } catch (error) { errorMessage(error) } }
onMounted(load)
</script>

<template>
  <div class="page staff-page">
    <div v-if="!items.length" class="empty-state"><div>录</div><b>暂无核销记录</b></div>
    <article v-for="item in items" :key="item.id" class="staff-surface verification-record"><div class="record-head"><div><b>{{ item.productName }}</b><small>#{{ item.cardId }}</small></div><strong>-{{ item.quantity }}</strong></div><div class="benefit-line"><span>核销项目</span><b>{{ item.itemSnapshot }}</b></div><div class="customer-block"><div class="row"><span>会员</span><b>{{ item.memberName }}</b></div><div class="row"><span>手机号</span><b>{{ item.memberPhone || '未绑定' }}</b></div></div><div class="record-foot"><span>操作人员 <b>{{ item.operatorName }}</b></span><time>{{ item.displayTime }}</time></div></article>
  </div>
</template>
