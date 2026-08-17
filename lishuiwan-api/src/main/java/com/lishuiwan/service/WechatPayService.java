package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.*; import com.lishuiwan.config.AppProperties; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*;
import com.wechat.pay.java.core.*; import com.wechat.pay.java.core.notification.*;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service;
import java.math.RoundingMode; import java.time.ZoneOffset; import java.time.format.DateTimeFormatter; import java.util.Map;

@Service
public class WechatPayService {
  private final AppProperties p; private final OrderService orderService; private final OrderMapper orders; private final OrderItemMapper items; private final MemberMapper members;
  private volatile RSAAutoCertificateConfig config; private volatile JsapiServiceExtension service;
  public WechatPayService(AppProperties p,OrderService orderService,OrderMapper orders,OrderItemMapper items,MemberMapper members){this.p=p;this.orderService=orderService;this.orders=orders;this.items=items;this.members=members;}
  public Map<String,Object> prepay(long memberId,String orderNo){requireEnabled();OrderEntity o=orderService.requireOrder(orderNo);if(!o.getMemberId().equals(memberId))throw BizException.forbidden();if(o.getPayStatus()==1)throw new BizException(40902,"订单已支付",HttpStatus.CONFLICT);if(o.getPayStatus()!=0||!o.getExpireAt().isAfter(java.time.LocalDateTime.now()))throw new BizException(40902,"订单已关闭",HttpStatus.CONFLICT);Member m=members.selectById(memberId);OrderItem item=items.selectOne(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId,o.getId()));
    PrepayRequest req=new PrepayRequest();req.setAppid(p.getWechat().getAppId());req.setMchid(p.getWechat().getPay().getMerchantId());req.setOutTradeNo(o.getOrderNo());req.setDescription("丽水湾会员卡-"+item.getProductId());req.setNotifyUrl(p.getWechat().getPay().getNotifyUrl());req.setTimeExpire(o.getExpireAt().atOffset(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));Amount amount=new Amount();amount.setTotal(cents(o));amount.setCurrency("CNY");req.setAmount(amount);Payer payer=new Payer();payer.setOpenid(m.getOpenid());req.setPayer(payer);PrepayWithRequestPaymentResponse r=payClient().prepayWithRequestPayment(req);return Map.of("appId",r.getAppId(),"timeStamp",r.getTimeStamp(),"nonceStr",r.getNonceStr(),"package",r.getPackageVal(),"signType",r.getSignType(),"paySign",r.getPaySign());}
  public void notify(String serial,String signature,String timestamp,String nonce,String body){requireEnabled();RequestParam param=new RequestParam.Builder().serialNumber(serial).signature(signature).timestamp(timestamp).nonce(nonce).body(body).build();Transaction tx=new NotificationParser(payConfig()).parse(param,Transaction.class);if(tx.getTradeState()!=Transaction.TradeStateEnum.SUCCESS)return;OrderEntity o=orderService.requireOrder(tx.getOutTradeNo());if(!p.getWechat().getAppId().equals(tx.getAppid())||!p.getWechat().getPay().getMerchantId().equals(tx.getMchid())||tx.getAmount()==null||tx.getAmount().getTotal()!=cents(o))throw new BizException(40006,"支付回调数据不匹配",HttpStatus.BAD_REQUEST);orderService.paySuccessLocked(tx.getOutTradeNo(),tx.getTransactionId());}
  private int cents(OrderEntity o){return o.getPayableAmount().movePointRight(2).setScale(0,RoundingMode.UNNECESSARY).intValueExact();}
  private void requireEnabled(){AppProperties.Pay x=p.getWechat().getPay();if(!x.isEnabled())throw new BizException(40304,"微信支付未启用",HttpStatus.FORBIDDEN);if(blank(x.getMerchantId())||blank(x.getMerchantSerialNumber())||blank(x.getPrivateKeyPath())||blank(x.getApiV3Key())||blank(x.getNotifyUrl()))throw new BizException(50003,"微信支付参数不完整");}
  private boolean blank(String s){return s==null||s.isBlank();}
  private synchronized RSAAutoCertificateConfig payConfig(){if(config==null){AppProperties.Pay x=p.getWechat().getPay();config=new RSAAutoCertificateConfig.Builder().merchantId(x.getMerchantId()).privateKeyFromPath(x.getPrivateKeyPath()).merchantSerialNumber(x.getMerchantSerialNumber()).apiV3Key(x.getApiV3Key()).build();}return config;}
  private synchronized JsapiServiceExtension payClient(){if(service==null)service=new JsapiServiceExtension.Builder().config(payConfig()).signType("RSA").build();return service;}
}
