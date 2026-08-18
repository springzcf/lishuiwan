package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.*; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*;
import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*;

@Service
public class VerificationService {
  private final CardCodeService codes; private final MemberMapper members; private final MembershipCardMapper cards; private final VerificationMapper verifications;
  private final NotificationMapper notifications; private final IdempotencyService idem; private final Jsons json; private final AuthService auth; private final OrderItemMapper items;
  public VerificationService(CardCodeService codes,MemberMapper members,MembershipCardMapper cards,VerificationMapper verifications,NotificationMapper notifications,IdempotencyService idem,Jsons json,AuthService auth,OrderItemMapper items){this.codes=codes;this.members=members;this.cards=cards;this.verifications=verifications;this.notifications=notifications;this.idem=idem;this.json=json;this.auth=auth;this.items=items;}
  public Map<String,Object> parse(String token){CardCodeService.CodeData code=codes.validate(token);Member m=members.selectById(code.memberId());if(m==null||m.getStatus()!=0)throw BizException.notFound("会员");MembershipCard card=cards.selectById(code.cardId());if(card==null||!card.getMemberId().equals(code.memberId())||!card.getValidUntil().isAfter(LocalDateTime.now())||!Set.of("unused","using").contains(card.getStatus()))throw new BizException(42201,"该权益卡当前不可核销");return Map.of("member",auth.safe(m),"card",cardView(card),"expiresAt",code.exp());}
  public Map<String,Object> verify(long operatorId,String requestNo,String token,long cardId,String benefitId,BigDecimal quantity,String operatorName){Map<String,Object> req=new LinkedHashMap<>();req.put("cardId",cardId);req.put("benefitId",benefitId);req.put("quantity",quantity);req.put("operatorName",operatorName);return idem.execute("member",operatorId,"verification",requestNo,req,()->verifyInternal(operatorId,requestNo,token,cardId,benefitId,quantity,operatorName));}
  private Map<String,Object> verifyInternal(long operatorId,String requestNo,String token,long cardId,String benefitId,BigDecimal quantity,String operatorName){
    if(operatorName==null||operatorName.isBlank()||operatorName.length()>50)throw new BizException(40002,"请输入有效核销人",HttpStatus.BAD_REQUEST);
    if(quantity==null||quantity.compareTo(BigDecimal.ZERO)<=0)throw new BizException(42202,"核销数量必须大于 0");
    CardCodeService.CodeData code=codes.validate(token);if(code.cardId()!=cardId)throw new BizException(40301,"核销码与权益卡不匹配",HttpStatus.FORBIDDEN);MembershipCard card=cards.selectForUpdate(cardId);
    if(card==null||!card.getMemberId().equals(code.memberId()))throw BizException.notFound("权益卡");if(!card.getValidUntil().isAfter(LocalDateTime.now())||"expired".equals(card.getStatus()))throw new BizException(42201,"权益卡已过期");if("used_up".equals(card.getStatus()))throw new BizException(42201,"权益已用完");
    List<Map<String,Object>> benefits=json.benefits(card.getBenefitsRemaining());Map<String,Object> target=benefits.stream().filter(b->benefitId.equals(String.valueOf(b.get("benefitId")))).findFirst().orElseThrow(()->BizException.notFound("权益项"));
    String type=String.valueOf(target.get("type"));BigDecimal deducted=quantity;
    if("unlimited".equals(type)){if(quantity.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO)!=0)throw new BizException(42202,"不限次权益的核销人数必须为正整数");}
    else {BigDecimal remaining=decimal(target.get("remaining")),step=decimal(target.getOrDefault("step","times".equals(type)?1:0.5));if(quantity.remainder(step).compareTo(BigDecimal.ZERO)!=0)throw new BizException(42202,"核销数量不符合步长 "+step);if(remaining.compareTo(quantity)<0)throw new BizException(42202,"权益不足，当前剩余 "+remaining);target.put("remaining",remaining.subtract(quantity));}
    codes.consume(code);card.setBenefitsRemaining(json.write(benefits));boolean unlimited=benefits.stream().anyMatch(b->"unlimited".equals(String.valueOf(b.get("type"))));boolean empty=benefits.stream().filter(b->!"unlimited".equals(String.valueOf(b.get("type")))).allMatch(b->decimal(b.get("remaining")).compareTo(BigDecimal.ZERO)==0);card.setStatus(!unlimited&&empty?"used_up":"using");if(cards.updateById(card)!=1)throw new BizException(40903,"权益已变化，请刷新后重试",HttpStatus.CONFLICT);
    Verification v=new Verification();v.setRequestNo(requestNo);v.setCardId(cardId);v.setMemberId(code.memberId());v.setBenefitId(benefitId);v.setItemSnapshot(String.valueOf(target.get("item")));v.setQuantity(deducted);v.setOperatorName(operatorName.trim());v.setOperatorMemberId(operatorId);verifications.insert(v);
    Notification n=new Notification();n.setMemberId(code.memberId());n.setType("verify");n.setTitle("核销成功");n.setContent("已核销"+target.get("item")+" "+quantity);n.setRefId(v.getId());n.setIsRead(0);n.setPushStatus("pending");n.setPushAttempts(0);notifications.insert(n);codes.clear(code);
    return Map.of("verificationId",v.getId(),"cardId",cardId,"status",card.getStatus(),"benefits",benefits);
  }
  @SuppressWarnings("unchecked") public Map<String,Object> cardView(MembershipCard c){Map<String,Object> m=new LinkedHashMap<>();m.put("id",c.getId());m.put("productId",c.getProductId());OrderItem item=items.selectById(c.getOrderItemId());if(item!=null){Map<String,Object> snapshot=json.read(item.getProductSnapshot(),Map.class);m.put("productName",snapshot.get("name"));m.put("category",snapshot.get("category"));}m.put("benefits",json.benefits(c.getBenefitsRemaining()));m.put("validFrom",c.getValidFrom());m.put("validUntil",c.getValidUntil());m.put("status",c.getValidUntil().isAfter(LocalDateTime.now())?c.getStatus():"expired");return m;}
  private BigDecimal decimal(Object v){return new BigDecimal(String.valueOf(v));}
  public List<Verification> records(){return verifications.selectList(new LambdaQueryWrapper<Verification>().orderByDesc(Verification::getId).last("limit 200"));}
}
