const api=require('../../utils/request');Page({data:{orders:[]},onShow(){this.load()},async load(){this.setData({orders:await api.get('/wx/orders')})}})
