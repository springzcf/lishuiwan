<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const toast = ref('')
let toastTimer
const isRoot = computed(() => ['activity', 'mine', 'login'].includes(String(route.name)))

function showToast(event) {
  toast.value = event.detail || '操作失败'
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = '' }, 2500)
}
function unauthorized() {
  if (route.name !== 'login') router.replace({ name: 'login', query: { redirect: route.fullPath } })
}
onMounted(() => {
  window.addEventListener('h5-toast', showToast)
  window.addEventListener('h5-unauthorized', unauthorized)
})
onUnmounted(() => {
  window.removeEventListener('h5-toast', showToast)
  window.removeEventListener('h5-unauthorized', unauthorized)
})
</script>

<template>
  <div class="app-shell">
    <header v-if="!route.meta.plain" class="topbar">
      <button v-if="!isRoot" class="back" aria-label="返回" @click="router.back()">‹</button>
      <div class="topbar-title">{{ route.meta.title }}</div>
      <div v-if="!isRoot" class="topbar-space"></div>
    </header>
    <main class="view">
      <router-view />
    </main>
    <Transition name="toast"><div v-if="toast" class="toast">{{ toast }}</div></Transition>
  </div>
</template>
