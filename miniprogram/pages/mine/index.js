const api=require('../../utils/request')

Page({
  data:{member:null,editing:false,saving:false,form:{nickname:'',birthday:'',address:''}},
  onShow(){this.load()},
  async load(){
    if(!getApp().requireLogin())return
    try{
      const member=await api.get('/wx/member/profile')
      getApp().globalData.member=member
      this.setData({member})
    }catch(e){}
  },
  go(e){
    const {url,tab}=e.currentTarget.dataset
    if(tab)wx.switchTab({url})
    else wx.navigateTo({url})
  },
  openEdit(){
    const m=this.data.member||{}
    this.setData({editing:true,form:{nickname:m.nickname||'',birthday:m.birthday||'',address:m.address||''}})
  },
  cancelEdit(){if(!this.data.saving)this.setData({editing:false})},
  onNickname(e){this.setData({'form.nickname':e.detail.value})},
  onAddress(e){this.setData({'form.address':e.detail.value})},
  onBirthday(e){this.setData({'form.birthday':e.detail.value})},
  async save(){
    if(this.data.saving)return
    const form={...this.data.form,nickname:this.data.form.nickname.trim()}
    if(!form.nickname){wx.showToast({title:'请输入昵称',icon:'none'});return}
    this.setData({saving:true})
    try{
      const member=await api.put('/wx/member/profile',form)
      getApp().globalData.member=member
      this.setData({member,editing:false,saving:false})
      wx.showToast({title:'资料已保存',icon:'success'})
    }catch(e){this.setData({saving:false})}
  },
  logout(){
    wx.removeStorageSync('token')
    getApp().globalData.member=null
    wx.navigateTo({url:'/pages/login/index'})
  }
})
