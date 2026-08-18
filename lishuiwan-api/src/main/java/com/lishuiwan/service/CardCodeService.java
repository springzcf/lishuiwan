package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.common.Ids;
import com.lishuiwan.common.Jsons;
import com.lishuiwan.config.AppProperties;
import com.lishuiwan.domain.ConsumedMemberCode;
import com.lishuiwan.domain.MembershipCard;
import com.lishuiwan.mapper.ConsumedMemberCodeMapper;
import com.lishuiwan.mapper.MembershipCardMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class CardCodeService {
  public record CodeData(long memberId,long cardId,String nonce,long exp){}
  private final StringRedisTemplate redis; private final ConsumedMemberCodeMapper consumed; private final MembershipCardMapper cards;
  private final Jsons json; private final QrCodeService qrCodes; private final byte[] key;

  public CardCodeService(StringRedisTemplate redis,ConsumedMemberCodeMapper consumed,MembershipCardMapper cards,Jsons json,QrCodeService qrCodes,AppProperties p){this.redis=redis;this.consumed=consumed;this.cards=cards;this.json=json;this.qrCodes=qrCodes;this.key=p.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);}

  public Map<String,Object> issue(long memberId,long cardId){
    MembershipCard card=cards.selectById(cardId);requireUsable(card,memberId);
    String nonce=Ids.nonce();long exp=Instant.now().plusSeconds(90).getEpochSecond();
    String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(json.write(Map.of("purpose","card_verify","memberId",memberId,"cardId",cardId,"nonce",nonce,"exp",exp)).getBytes(StandardCharsets.UTF_8));
    String token="lishuiwan://card-code?token="+payload+"."+sign(payload);
    redis.opsForValue().set("card-code:"+nonce,memberId+":"+cardId,Duration.ofSeconds(90));
    return Map.of("token",token,"qrImage",qrCodes.asPngDataUrl(token),"cardId",cardId,"expiresAt",exp);
  }

  public CodeData validate(String raw){
    try{
      String token=raw.contains("token=")?raw.substring(raw.indexOf("token=")+6):raw;String[] parts=token.split("\\.");
      if(parts.length!=2||!constantEquals(sign(parts[0]),parts[1]))throw invalid();
      @SuppressWarnings("unchecked") Map<String,Object> p=json.read(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),Map.class);
      if(!"card_verify".equals(p.get("purpose")))throw invalid();
      long memberId=Long.parseLong(p.get("memberId").toString()),cardId=Long.parseLong(p.get("cardId").toString()),exp=Long.parseLong(p.get("exp").toString());String nonce=p.get("nonce").toString();
      if(Instant.now().getEpochSecond()>=exp||!((memberId+":"+cardId).equals(redis.opsForValue().get("card-code:"+nonce))))throw invalid();
      if(consumed.selectCount(new LambdaQueryWrapper<ConsumedMemberCode>().eq(ConsumedMemberCode::getNonce,nonce))>0)throw invalid();
      return new CodeData(memberId,cardId,nonce,exp);
    }catch(BizException e){throw e;}catch(Exception e){throw invalid();}
  }

  public void consume(CodeData code){ConsumedMemberCode c=new ConsumedMemberCode();c.setNonce(code.nonce());c.setMemberId(code.memberId());c.setAction("verification");c.setExpireAt(LocalDateTime.now().plusDays(1));consumed.insert(c);}
  public void clear(CodeData code){redis.delete("card-code:"+code.nonce());}
  private void requireUsable(MembershipCard card,long memberId){if(card==null||!card.getMemberId().equals(memberId))throw BizException.notFound("权益卡");if(!card.getValidUntil().isAfter(LocalDateTime.now())||!java.util.Set.of("unused","using").contains(card.getStatus()))throw new BizException(42201,"该权益卡当前不可核销");}
  private BizException invalid(){return new BizException(41001,"权益卡核销码已过期或已使用",HttpStatus.GONE);}
  private String sign(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private boolean constantEquals(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
