const api=require('../../../utils/request')
Page({
  data:{member:null,card:null,benefitIndex:0,quantity:1,submitting:false},
  onLoad(){const p=getApp().globalData.pendingScan;if(!p){wx.navigateBack();return}this.pending=p;this.setData({member:p.parsed.member,card:p.parsed.card,quantity:p.quantity||1})},
  benefitChange(e){this.setData({benefitIndex:Number(e.detail.value)})},
  quantity(e){this.setData({quantity:e.detail.value})},
  async submit(){
    const card=this.data.card,benefit=card&&card.benefits[this.data.benefitIndex]
    if(!benefit){wx.showToast({title:'没有可用权益',icon:'none'});return}
    const quantity=Number(this.data.quantity)
    if(!Number.isFinite(quantity)||quantity<=0){wx.showToast({title:'请输入正确的核销数量',icon:'none'});return}
    const ok=await new Promise(r=>wx.showModal({title:'确认核销',content:`${card.productName||'权益卡'}\n${benefit.item} ${quantity}${benefit.type==='hours'?'小时':'次'}`,success:x=>r(x.confirm)}));if(!ok)return
    this.setData({submitting:true})
    try{await api.post('/wx/staff/verifications',{requestNo:api.uuid(),cardCodeToken:this.pending.token,cardId:card.id,benefitId:benefit.benefitId,quantity});getApp().globalData.pendingScan=null;wx.setStorageSync('verificationQuantity',1);wx.showToast({title:`已核销 ${quantity} ${benefit.type==='hours'?'小时':'次'}`});setTimeout(()=>wx.navigateBack(),1000)}finally{this.setData({submitting:false})}
  }
})
