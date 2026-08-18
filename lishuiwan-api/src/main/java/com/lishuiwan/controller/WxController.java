package com.lishuiwan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.*; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*; import com.lishuiwan.service.*;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/wx")
public class WxController {
  private final CatalogService catalog; private final MemberMapper members; private final AuthService auth; private final MemberCodeService codes; private final CardCodeService cardCodes;
  private final OrderService orders; private final VerificationService verification; private final VerificationMapper verifications; private final NotificationMapper notifications;
  private final WechatPayService wechatPay;
  public WxController(CatalogService catalog,MemberMapper members,AuthService auth,MemberCodeService codes,CardCodeService cardCodes,OrderService orders,VerificationService verification,VerificationMapper verifications,NotificationMapper notifications,WechatPayService wechatPay){this.catalog=catalog;this.members=members;this.auth=auth;this.codes=codes;this.cardCodes=cardCodes;this.orders=orders;this.verification=verification;this.verifications=verifications;this.notifications=notifications;this.wechatPay=wechatPay;}
  public record ProfileRequest(@Size(max=64) String nickname,@Size(max=255) String avatar,@Size(max=10) String birthday,@Size(max=255) String address){}
  public record CreateOrderRequest(@NotNull Long productId,@NotBlank String requestNo){}
  public record PayRequest(@NotBlank String requestNo){}
  @GetMapping("/home") public ApiResponse<Map<String,Object>> home(){return ApiResponse.ok(catalog.home());}
  @GetMapping("/products") public ApiResponse<List<Map<String,Object>>> products(){return ApiResponse.ok(catalog.products(true));}
  @GetMapping("/products/{id}") public ApiResponse<Map<String,Object>> product(@PathVariable long id){return ApiResponse.ok(catalog.product(id));}
  @GetMapping("/member/profile") public ApiResponse<Map<String,Object>> profile(){Member m=members.selectById(RequestActor.memberId());if(m==null)throw BizException.unauthorized();return ApiResponse.ok(auth.safe(m));}
  @PutMapping("/member/profile") public ApiResponse<Map<String,Object>> update(@Valid @RequestBody ProfileRequest r){Member m=members.selectById(RequestActor.memberId());m.setNickname(r.nickname());m.setAvatar(r.avatar());m.setBirthday(r.birthday());m.setAddress(r.address());members.updateById(m);return ApiResponse.ok(auth.safe(m));}
  @GetMapping("/member/code") public ApiResponse<Map<String,Object>> memberCode(){return ApiResponse.ok(codes.issue(RequestActor.memberId()));}
  @PostMapping("/orders") public ApiResponse<Map<String,Object>> createOrder(@Valid @RequestBody CreateOrderRequest r){return ApiResponse.ok(orders.create(RequestActor.memberId(),r.productId(),r.requestNo()));}
  @PostMapping("/orders/{orderNo}/mock-pay") public ApiResponse<Map<String,Object>> mockPay(@PathVariable String orderNo,@Valid @RequestBody PayRequest r){return ApiResponse.ok(orders.mockPay(RequestActor.memberId(),orderNo,r.requestNo()));}
  @PostMapping("/orders/{orderNo}/prepay") public ApiResponse<Map<String,Object>> prepay(@PathVariable String orderNo){return ApiResponse.ok(wechatPay.prepay(RequestActor.memberId(),orderNo));}
  @GetMapping("/orders") public ApiResponse<List<Map<String,Object>>> orderList(){return ApiResponse.ok(orders.memberOrders(RequestActor.memberId()));}
  @GetMapping("/orders/{orderNo}") public ApiResponse<Map<String,Object>> order(@PathVariable String orderNo){OrderEntity o=orders.requireOrder(orderNo);if(!o.getMemberId().equals(RequestActor.memberId()))throw BizException.forbidden();return ApiResponse.ok(orders.view(o));}
  @GetMapping("/cards") public ApiResponse<List<Map<String,Object>>> cards(){return ApiResponse.ok(orders.memberCards(RequestActor.memberId()).stream().map(verification::cardView).toList());}
  @GetMapping("/cards/{id}") public ApiResponse<Map<String,Object>> card(@PathVariable long id){MembershipCard c=orders.memberCards(RequestActor.memberId()).stream().filter(x->x.getId()==id).findFirst().orElseThrow(()->BizException.notFound("权益卡"));return ApiResponse.ok(Map.of("card",verification.cardView(c),"verifications",orders.cardVerifications(RequestActor.memberId(),id,verifications)));}
  @GetMapping("/cards/{id}/code") public ApiResponse<Map<String,Object>> cardCode(@PathVariable long id){return ApiResponse.ok(cardCodes.issue(RequestActor.memberId(),id));}
  @GetMapping("/notifications") public ApiResponse<List<Notification>> notificationList(){return ApiResponse.ok(notifications.selectList(new LambdaQueryWrapper<Notification>().eq(Notification::getMemberId,RequestActor.memberId()).orderByDesc(Notification::getId).last("limit 200")));}
  @PutMapping("/notifications/read") public ApiResponse<Void> read(@RequestBody(required=false) Map<String,Object> body){long memberId=RequestActor.memberId();if(body!=null&&body.get("id")!=null){Notification n=notifications.selectById(Long.parseLong(body.get("id").toString()));if(n!=null&&n.getMemberId()==memberId){n.setIsRead(1);notifications.updateById(n);}}else notifications.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Notification>().eq(Notification::getMemberId,memberId).set(Notification::getIsRead,1));return ApiResponse.ok(null);}
}
