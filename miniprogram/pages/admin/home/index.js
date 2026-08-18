const api=require('../../../utils/request')
Page({
  data:{member:null,verificationQuantity:1},
  onShow(){this.load()},
  async load(){const member=await api.get('/wx/member/profile');if(!['verifier','admin'].includes(member.staffRole)){wx.showToast({title:'无管理权限',icon:'none'});wx.navigateBack();return}this.setData({member,verificationQuantity:Number(wx.getStorageSync('verificationQuantity'))||1})},
  quantity(e){this.setQuantity(e.detail.value)},
  decrease(){this.setQuantity(Math.max(1,(Number(this.data.verificationQuantity)||1)-1))},
  increase(){this.setQuantity((Number(this.data.verificationQuantity)||0)+1)},
  setQuantity(value){this.setData({verificationQuantity:value});wx.setStorageSync('verificationQuantity',value)},
  async scan(e){
    const quantity=Number(this.data.verificationQuantity)
    if(!Number.isFinite(quantity)||quantity<=0){wx.showToast({title:'请输入正确的核销数量',icon:'none'});return}
    const scan=await new Promise((resolve,reject)=>wx.scanCode({onlyFromCamera:true,scanType:['qrCode'],success:resolve,fail:reject}))
    const parsed=await api.post('/wx/staff/card-codes/parse',{token:scan.result})
    getApp().globalData.pendingScan={token:scan.result,parsed,quantity}
    wx.navigateTo({url:'/pages/admin/verify/index'})
  },
  cashIssue(){wx.navigateTo({url:'/pages/admin/cash-issue/index'})},
  go(e){wx.navigateTo({url:e.currentTarget.dataset.url})}
})
