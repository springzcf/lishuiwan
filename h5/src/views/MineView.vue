<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, logout } from '../api.js'
import { errorMessage, toast } from '../ui.js'

const router = useRouter()
const member = ref(null)
const editing = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', birthday: '', address: '' })
async function load() {
  try { member.value = await api.get('/wx/member/profile'); localStorage.setItem('h5_member', JSON.stringify(member.value)) } catch (error) { errorMessage(error) }
}
function openEdit() { Object.assign(form, { nickname: member.value.nickname || '', birthday: member.value.birthday || '', address: member.value.address || '' }); editing.value = true }
async function save() {
  if (!form.nickname.trim()) return toast('请输入昵称')
  saving.value = true
  try { member.value = await api.put('/wx/member/profile', { ...form, nickname: form.nickname.trim() }); localStorage.setItem('h5_member', JSON.stringify(member.value)); editing.value = false; toast('资料已保存') } catch (error) { errorMessage(error) } finally { saving.value = false }
}
function signOut() { logout(); router.replace('/login') }
onMounted(load)
</script>

<template>
  <div v-if="member" class="page mine-page">
    <section class="hero profile-hero"><div class="profile-row"><div class="avatar">{{ member.nickname?.[0] || '会' }}</div><div class="profile-main"><h1>{{ member.nickname || '丽水湾会员' }}</h1><p>{{ member.phone || '未绑定手机号' }}</p><span>丽水湾会员</span></div><div class="profile-actions"><button @click="openEdit">编辑资料</button><button class="gold" @click="router.push('/member-code')">▦ 身份码</button></div></div></section>
    <h2 class="block-title">常用服务</h2>
    <div class="quick-grid"><button @click="router.push('/cards')"><i class="green">卡</i><b>我的权益</b><small>查看权益卡及余量</small></button><button @click="router.push('/orders')"><i class="orange">单</i><b>我的订单</b><small>查看购买与支付记录</small></button></div>
    <h2 class="block-title">其他服务</h2>
    <section class="surface service-list"><button @click="router.push('/notifications')"><i class="purple">知</i><span><b>核销与发卡通知</b><small>查看权益变动消息</small></span><em>›</em></button><button v-if="['verifier','admin'].includes(member.staffRole)" @click="router.push('/staff')"><i class="blue">管</i><span><b class="blue-text">进入管理端</b><small>扫码核销与门店管理</small></span><em>›</em></button><div><i class="green">店</i><span><b>门店客服</b><small>如需帮助，请联系前台</small></span><em class="value">前台咨询</em></div></section>
    <p class="account-meta">注册时间 {{ member.createdAt }}</p><button class="logout-button" @click="signOut">退出登录</button>
    <div v-if="editing" class="modal-mask" @click.self="editing = false"><section class="bottom-sheet edit-sheet"><div class="sheet-handle"></div><div class="sheet-title-row"><div><h2>编辑会员资料</h2><p>完善资料，方便门店为您提供服务</p></div><button class="close-button" @click="editing = false">×</button></div><label>昵称<input v-model="form.nickname" class="field" maxlength="64"></label><label>手机号<input :value="member.phone" class="field" disabled></label><label>生日<input v-model="form.birthday" class="field" type="date"></label><label>联系地址<textarea v-model="form.address" class="field" maxlength="255" placeholder="请输入地址（选填）"></textarea></label><button class="primary-button" :disabled="saving" @click="save">{{ saving ? '正在保存…' : '保存资料' }}</button></section></div>
  </div>
</template>
