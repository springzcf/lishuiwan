<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api.js'
import { errorMessage } from '../ui.js'

const items = ref([])
async function load() {
  try {
    items.value = await api.get('/wx/notifications')
    if (items.value.some(item => !item.isRead)) api.put('/wx/notifications/read', {}).catch(() => {})
  } catch (error) { errorMessage(error) }
}
onMounted(load)
</script>

<template>
  <div class="page list-page">
    <div v-if="!items.length" class="empty-state"><div>知</div><b>暂无消息</b></div>
    <article v-for="item in items" :key="item.id" class="surface notification-card"><i :class="{ read: item.isRead }"></i><div><b>{{ item.title }}</b><p class="notification-content">{{ item.content }}</p><small>{{ item.createdAt }}</small></div></article>
  </div>
</template>
