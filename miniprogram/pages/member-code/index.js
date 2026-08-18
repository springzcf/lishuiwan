const api=require('../../utils/request')
Page({
  data:{
    qrImage:'',loading:true,error:false
  },

  onShow(){
    this.active=true
    this.refresh()
  },

  onHide(){this.active=false},
  onUnload(){this.active=false},

  subscribe(){
    const ids=[api.env.verifyTemplateId,api.env.issueTemplateId].filter(Boolean)
    if(!ids.length){wx.showToast({title:'提醒模板尚未配置',icon:'none'});return}
    wx.requestSubscribeMessage({tmplIds:ids,success:()=>wx.showToast({title:'提醒设置完成'})})
  },

  async refresh(){
    if(this.refreshing)return
    this.refreshing=true
    try{
      const r=await api.get('/wx/member/code')
      if(!this.active)return
      if(!r.qrImage)throw new Error('会员码图片为空')
      this.setData({qrImage:r.qrImage,loading:false,error:false})
    }catch(e){
      if(this.active)this.setData({loading:false,error:true,qrImage:''})
    }finally{
      this.refreshing=false
    }
  }
})
