import {createRouter,createWebHistory} from 'vue-router'
const view=name=>()=>import(`./views/${name}.vue`)
const router=createRouter({history:createWebHistory('/admin/'),routes:[{path:'/login',component:view('Login'),meta:{public:true}},{path:'/',redirect:'/dashboard'},{path:'/dashboard',component:view('Dashboard')},{path:'/products',component:view('Products')},{path:'/activities',component:view('Activities')},{path:'/members',component:view('Members')},{path:'/orders',component:view('Orders')},{path:'/verifications',component:view('Verifications')},{path:'/staff',component:view('Staff')}]})
router.beforeEach(to=>{if(!to.meta.public&&!localStorage.getItem('admin_token'))return '/login';if(to.path==='/login'&&localStorage.getItem('admin_token'))return '/dashboard'})
export default router
