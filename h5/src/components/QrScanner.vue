<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { errorMessage, toast } from '../ui.js'

const emit = defineEmits(['result', 'close'])
const video = ref(null)
const manual = ref('')
const cameraError = ref('')
let stream
let timer

async function start() {
  if (!('BarcodeDetector' in window)) {
    cameraError.value = '当前浏览器不支持自动识别，请粘贴二维码内容'
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
    await nextTick()
    video.value.srcObject = stream
    await video.value.play()
    const detector = new BarcodeDetector({ formats: ['qr_code'] })
    timer = window.setInterval(async () => {
      try {
        const codes = await detector.detect(video.value)
        if (codes[0]?.rawValue) finish(codes[0].rawValue)
      } catch (_) { /* camera frame may not be ready */ }
    }, 500)
  } catch (error) {
    cameraError.value = error?.name === 'NotAllowedError' ? '相机权限未开启，请粘贴二维码内容' : '相机启动失败，请粘贴二维码内容'
  }
}
function stop() {
  clearInterval(timer)
  stream?.getTracks().forEach(track => track.stop())
}
function finish(value) {
  const token = String(value || '').trim()
  if (!token) return toast('请输入二维码内容')
  stop()
  emit('result', token)
}
async function scanFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    if (!('BarcodeDetector' in window)) throw new Error('当前浏览器不支持图片识别')
    const bitmap = await createImageBitmap(file)
    const codes = await new BarcodeDetector({ formats: ['qr_code'] }).detect(bitmap)
    if (!codes[0]?.rawValue) throw new Error('图片中未识别到二维码')
    finish(codes[0].rawValue)
  } catch (error) { errorMessage(error) }
}
function close() { stop(); emit('close') }
onBeforeUnmount(stop)
start()
</script>

<template>
  <div class="modal-mask" @click.self="close">
    <section class="scanner-sheet">
      <div class="sheet-handle"></div>
      <div class="sheet-title-row"><div><h2>扫描二维码</h2><p>将顾客二维码放入取景框</p></div><button class="close-button" @click="close">×</button></div>
      <div v-if="!cameraError" class="camera-wrap"><video ref="video" muted playsinline></video><div class="scan-frame"></div></div>
      <div v-else class="camera-fallback">{{ cameraError }}</div>
      <label class="file-button">从相册识别<input type="file" accept="image/*" capture="environment" @change="scanFile"></label>
      <div class="manual-row"><input v-model="manual" class="field" placeholder="或粘贴二维码内容"><button class="small-button" @click="finish(manual)">确认</button></div>
    </section>
  </div>
</template>
