const api=require('../../../utils/request')
Page({
  data:{member:null,operatorName:'',verificationQuantity:1},
  onShow(){this.load()},
  async load(){const member=await api.get('/wx/member/profile');if(!['verifier','admin'].includes(member.staffRole)){wx.showToast({title:'无管理权限',icon:'none'});wx.navigateBack();return}this.setData({member,operatorName:wx.getStorageSync('operatorName')||member.nickname||'',verificationQuantity:Number(wx.getStorageSync('verificationQuantity'))||1})},
  name(e){this.setData({operatorName:e.detail.value});wx.setStorageSync('operatorName',e.detail.value)},
  quantity(e){this.setQuantity(e.detail.value)},
  decrease(){this.setQuantity(Math.max(1,(Number(this.data.verificationQuantity)||1)-1))},
  increase(){this.setQuantity((Number(this.data.verificationQuantity)||0)+1)},
  setQuantity(value){this.setData({verificationQuantity:value});wx.setStorageSync('verificationQuantity',value)},
  async scan(e){
    if(!this.data.operatorName.trim()){wx.showToast({title:'请先填写核销人',icon:'none'});return}
    const mode=e.currentTarget.dataset.mode
    const quantity=Number(this.data.verificationQuantity)
    if(mode==='verify'&&(!Number.isFinite(quantity)||quantity<=0)){wx.showToast({title:'请输入正确的核销数量',icon:'none'});return}
    const scan=await new Promise((resolve,reject)=>wx.scanCode({onlyFromCamera:true,scanType:['qrCode'],success:resolve,fail:reject}))
    const endpoint=mode==='cash'?'/wx/staff/member-codes/parse':'/wx/staff/card-codes/parse'
    const parsed=await api.post(endpoint,{token:scan.result})
    getApp().globalData.pendingScan={token:scan.result,parsed,operatorName:this.data.operatorName,quantity:mode==='verify'?quantity:undefined}
    wx.navigateTo({url:mode==='cash'?'/pages/admin/cash-issue/index':'/pages/admin/verify/index'})
  },
  go(e){wx.navigateTo({url:e.currentTarget.dataset.url})}
})
