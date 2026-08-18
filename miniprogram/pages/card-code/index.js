const api=require('../../utils/request')
const REFRESH_SECONDS=10

Page({
  data:{card:null,qrImage:'',loading:true,qrReady:false,error:false,secondsToRefresh:REFRESH_SECONDS},
  onLoad(o){this.id=o.id},
  onShow(){this.active=true;this.loadCard();this.startAutoRefresh()},
  onHide(){this.active=false;this.stopAutoRefresh()},
  onUnload(){this.active=false;this.stopAutoRefresh()},
  async loadCard(){try{const r=await api.get(`/wx/cards/${this.id}`);if(this.active)this.setData({card:r.card})}catch(e){}},
  startAutoRefresh(){
    this.stopAutoRefresh();this.nextRefreshAt=Date.now()+REFRESH_SECONDS*1000;this.setData({secondsToRefresh:REFRESH_SECONDS});this.refresh()
    this.timer=setInterval(()=>{if(!this.active)return;const now=Date.now();if(now>=this.nextRefreshAt){this.nextRefreshAt=now+REFRESH_SECONDS*1000;this.setData({secondsToRefresh:REFRESH_SECONDS});this.refresh();return}this.setData({secondsToRefresh:Math.max(1,Math.ceil((this.nextRefreshAt-now)/1000))})},1000)
  },
  stopAutoRefresh(){if(this.timer){clearInterval(this.timer);this.timer=null}},
  async refresh(){
    if(this.refreshing)return;this.refreshing=true
    try{const r=await api.get(`/wx/cards/${this.id}/code`);if(!this.active)return;if(!r.qrImage)throw new Error('权益卡核销码图片为空');this.setData({qrImage:r.qrImage,loading:false,qrReady:true,error:false})}
    catch(e){if(this.active)this.setData({loading:false,error:true,qrReady:false,qrImage:''})}
    finally{this.refreshing=false}
  }
})
