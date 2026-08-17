import axios from 'axios'
import { ElMessage } from 'element-plus'
export const http=axios.create({baseURL:'/api',timeout:12000})
http.interceptors.request.use(c=>{const t=localStorage.getItem('admin_token');if(t)c.headers.Authorization=`Bearer ${t}`;c.headers['X-Trace-Id']=crypto.randomUUID().replaceAll('-','');return c})
http.interceptors.response.use(r=>r.data.data,e=>{if(e.response?.status===401){localStorage.removeItem('admin_token');location.href='/admin/login'}const msg=e.response?.data?.msg||e.message||'请求失败';ElMessage.error(`${msg}${e.response?.data?.traceId?`（${e.response.data.traceId}）`:''}`);return Promise.reject(e)})
export const api={
  login:data=>http.post('/public/admin/login',data),overview:()=>http.get('/admin/reports/overview'),sales:()=>http.get('/admin/reports/sales'),
  products:()=>http.get('/admin/products'),saveProduct:(id,data)=>id?http.put(`/admin/products/${id}`,data):http.post('/admin/products',data),productStatus:(id,status)=>http.put(`/admin/products/${id}/status`,{status}),deleteProduct:id=>http.delete(`/admin/products/${id}`),
  activities:()=>http.get('/admin/activities'),saveActivity:(id,data)=>id?http.put(`/admin/activities/${id}`,data):http.post('/admin/activities',data),deleteActivity:id=>http.delete(`/admin/activities/${id}`),
  members:q=>http.get('/admin/members',{params:{keyword:q||undefined}}),orders:()=>http.get('/admin/orders'),verifications:()=>http.get('/admin/verifications'),staff:()=>http.get('/admin/staff'),grant:data=>http.post('/admin/staff',data),revoke:id=>http.delete(`/admin/staff/${id}`),
  admins:()=>http.get('/admin/admin-users'),saveAdmin:(id,data)=>id?http.put(`/admin/admin-users/${id}`,data):http.post('/admin/admin-users',data),disableAdmin:id=>http.delete(`/admin/admin-users/${id}`)
}
