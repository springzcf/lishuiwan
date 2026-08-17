const api=require('../../../utils/request');Page({data:{data:{}},async onShow(){this.setData({data:await api.get('/wx/staff/reports/summary')})}})
