const api=require('../../../utils/request')

Page({
  data:{items:[]},
  onShow(){this.load()},
  onPullDownRefresh(){this.load().finally(()=>wx.stopPullDownRefresh())},
  async load(){
    const items=await api.get('/wx/staff/verifications')
    this.setData({items:items.map(item=>({...item,displayTime:String(item.createdAt||'').replace('T',' ').slice(0,19)}))})
  }
})
