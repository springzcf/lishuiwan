import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'
import CardsView from './views/CardsView.vue'
import CardDetailView from './views/CardDetailView.vue'
import CodeView from './views/CodeView.vue'
import OrdersView from './views/OrdersView.vue'
import NotificationsView from './views/NotificationsView.vue'
import MineView from './views/MineView.vue'
import StaffHomeView from './views/staff/StaffHomeView.vue'
import VerifyView from './views/staff/VerifyView.vue'
import CashIssueView from './views/staff/CashIssueView.vue'
import RecordsView from './views/staff/RecordsView.vue'
import ReportView from './views/staff/ReportView.vue'
import AuthCallbackView from './views/AuthCallbackView.vue'
import { api, isWechatBrowser, logout } from './api.js'

const routes = [
  { path: '/', redirect: '/activity' },
  { path: '/activity', name: 'activity', component: HomeView, meta: { title: '活动', auth: true } },
  { path: '/login', name: 'login', component: LoginView, meta: { title: '会员登录', plain: true } },
  { path: '/auth/callback', name: 'auth-callback', component: AuthCallbackView, meta: { title: '微信登录', plain: true } },
  { path: '/cards', name: 'cards', component: CardsView, meta: { title: '我的权益', auth: true } },
  { path: '/cards/:id', name: 'card-detail', component: CardDetailView, meta: { title: '权益卡详情', auth: true } },
  { path: '/cards/:id/code', name: 'card-code', component: CodeView, meta: { title: '动态核销码', auth: true } },
  { path: '/member-code', name: 'member-code', component: CodeView, meta: { title: '会员身份码', auth: true } },
  { path: '/orders', name: 'orders', component: OrdersView, meta: { title: '我的订单', auth: true } },
  { path: '/notifications', name: 'notifications', component: NotificationsView, meta: { title: '服务通知', auth: true } },
  { path: '/mine', name: 'mine', component: MineView, meta: { title: '个人中心', auth: true } },
  { path: '/staff', name: 'staff', component: StaffHomeView, meta: { title: '现场工作台', auth: true } },
  { path: '/staff/verify', name: 'verify', component: VerifyView, meta: { title: '确认核销', auth: true } },
  { path: '/staff/cash-issue', name: 'cash-issue', component: CashIssueView, meta: { title: '现场发卡', auth: true } },
  { path: '/staff/records', name: 'records', component: RecordsView, meta: { title: '核销记录', auth: true } },
  { path: '/staff/report', name: 'report', component: ReportView, meta: { title: '数据速览', auth: true } }
]

const router = createRouter({ history: createWebHistory('/h5/'), routes, scrollBehavior: () => ({ top: 0 }) })
router.beforeEach(to => {
  document.title = `${to.meta.title || '会员服务'} · 丽水湾`
  if (!api.isLocal && !isWechatBrowser()) {
    logout()
    if (to.name !== 'login') return { name: 'login', query: { redirect: to.fullPath } }
    return
  }
  if (to.meta.auth && !localStorage.getItem('h5_token')) return { name: 'login', query: { redirect: to.fullPath } }
})

export default router
