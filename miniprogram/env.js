const account=wx.getAccountInfoSync?.().miniProgram||{}
const localRuntime=account.envVersion==='develop'
const develop=localRuntime&&!wx.getStorageSync('useRealWechat')
module.exports={
  apiBase:localRuntime?(wx.getStorageSync('localApiBase')||'http://127.0.0.1:8080'):'https://springboot-lxer-299021-11-1470340989.sh.run.tcloudbase.com',
  devMode:develop,
  devOpenId:wx.getStorageSync('devOpenId')||'local_customer',
  devPhone:wx.getStorageSync('devPhone')||'13800000000',
  useMockPayment:localRuntime&&!wx.getStorageSync('useRealPayment'),
  verifyTemplateId:'',
  issueTemplateId:''
}