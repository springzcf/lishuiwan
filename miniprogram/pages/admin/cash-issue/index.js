const api=require('../../../utils/request')

Page({
  data:{products:[],productIndex:-1,paidAmount:'',member:null,memberCodeToken:'',staffPin:'',hasPin:null,scanning:false,submitting:false},
  async onLoad(){
    const products=await api.get('/wx/products')
    this.setData({products})
  },
  onShow(){api.get('/wx/staff/pin').then(s=>this.setData({hasPin:s.hasPin})).catch(()=>this.setData({hasPin:false}))},
  productChange(e){
    const productIndex=Number(e.detail.value)
    const product=this.data.products[productIndex]
    this.setData({productIndex,paidAmount:product.currentPrice,member:null,memberCodeToken:'',staffPin:''})
  },
  amount(e){this.setData({paidAmount:e.detail.value})},
  pin(e){this.setData({staffPin:e.detail.value})},
  async scanMember(){
    if(this.data.productIndex<0){wx.showToast({title:'请先选择卡券类型',icon:'none'});return}
    this.setData({scanning:true})
    try{
      const scan=await new Promise((resolve,reject)=>wx.scanCode({onlyFromCamera:true,scanType:['qrCode'],success:resolve,fail:reject}))
      const parsed=await api.post('/wx/staff/member-codes/parse',{token:scan.result})
      this.setData({member:parsed.member,memberCodeToken:scan.result,staffPin:''})
    }catch(error){
      if(!String(error&&error.errMsg||'').includes('cancel'))console.error('scan member failed',error)
    }finally{this.setData({scanning:false})}
  },
  async submit(){
    const product=this.data.products[this.data.productIndex]
    if(!product){wx.showToast({title:'请先选择卡券类型',icon:'none'});return}
    if(!this.data.memberCodeToken||!this.data.member){wx.showToast({title:'请先扫描会员码',icon:'none'});return}
    if(!this.data.staffPin){wx.showToast({title:'请输入操作密码',icon:'none'});return}
    const paidAmount=Number(this.data.paidAmount)
    if(!Number.isFinite(paidAmount)||paidAmount<=0){wx.showToast({title:'请输入正确的实收金额',icon:'none'});return}
    const ok=await new Promise(resolve=>wx.showModal({title:'确认现场发卡',content:`会员：${this.data.member.phone}\n卡券：${product.name}\n实收：¥${paidAmount.toFixed(2)}`,confirmText:'确认发卡',success:result=>resolve(result.confirm)}))
    if(!ok)return
    this.setData({submitting:true})
    try{
      await api.post('/wx/staff/cash-orders',{requestNo:api.uuid(),memberCodeToken:this.data.memberCodeToken,productId:product.id,paidAmount,staffPin:this.data.staffPin})
      wx.showToast({title:'发卡成功'})
      setTimeout(()=>wx.navigateBack(),1000)
    }finally{this.setData({submitting:false,staffPin:''})}
  }
})
