<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api.js'
import { errorMessage } from '../ui.js'

const route = useRoute()
const isMember = computed(() => route.name === 'member-code')
const card = ref(null)
const qrImage = ref('')
const loading = ref(true)
const seconds = ref(10)
let timer

async function refresh() {
  try {
    const result = await api.get(isMember.value ? '/wx/member/code' : `/wx/cards/${route.params.id}/code`)
    if (!result.qrImage) throw new Error('二维码生成失败')
    qrImage.value = result.qrImage
    seconds.value = 10
  } catch (error) { qrImage.value = ''; errorMessage(error) } finally { loading.value = false }
}
async function load() {
  if (!isMember.value) {
    try { card.value = (await api.get(`/wx/cards/${route.params.id}`)).card } catch (error) { errorMessage(error) }
  }
  await refresh()
  if (!isMember.value) timer = window.setInterval(() => { seconds.value--; if (seconds.value <= 0) refresh() }, 1000)
}
onMounted(load)
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div class="code-page">
    <div class="code-intro">
      <span class="eyebrow">{{ isMember ? 'MEMBER ID' : 'MEMBERSHIP PASS' }}</span>
      <h1>{{ isMember ? '会员身份码' : (card?.productName || '动态核销码') }}</h1>
      <p>{{ isMember ? '现场购卡或发卡时，向工作人员出示' : `权益卡 #${card?.id || ''} · 有效期至 ${card?.validUntil || ''}` }}</p>
    </div>
    <section class="qr-card">
      <span class="live-badge"><i>{{ isMember ? 'ID' : '' }}</i>{{ isMember ? '会员身份识别' : '动态核销码' }}</span>
      <button class="qr-wrap" @click="!qrImage && refresh()"><img v-if="qrImage" :src="qrImage" alt="二维码"><span v-else>{{ loading ? '正在生成二维码…' : '生成失败，点击重试' }}</span></button>
      <b class="refresh-text">{{ isMember ? '此码长期有效，无需刷新' : `${seconds} 秒后自动刷新` }}</b>
      <p>{{ isMember ? '仅用于现场购卡、现金发卡和会员身份识别' : '此二维码仅能核销当前权益卡，请勿截图或转发' }}</p>
    </section>
    <section class="usage-tip"><b>使用说明</b><p>{{ isMember ? '权益核销请前往具体权益卡，出示该卡的动态核销码。' : '向工作人员出示此码，扫码后选择本卡权益和核销数量。' }}</p></section>
  </div>
</template>
