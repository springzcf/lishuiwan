package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.*;
import com.lishuiwan.config.AppProperties;
import com.lishuiwan.domain.ConsumedMemberCode;
import com.lishuiwan.mapper.ConsumedMemberCodeMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.nio.charset.StandardCharsets; import java.time.*; import java.util.*;

@Service
public class MemberCodeService {
  public record CodeData(long memberId,String nonce,long exp){}
  private final StringRedisTemplate redis; private final ConsumedMemberCodeMapper consumed; private final Jsons json; private final byte[] key;
  public MemberCodeService(StringRedisTemplate redis,ConsumedMemberCodeMapper consumed,Jsons json,AppProperties p){this.redis=redis;this.consumed=consumed;this.json=json;this.key=p.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);}
  public Map<String,Object> issue(long memberId){String nonce=Ids.nonce();long exp=Instant.now().plusSeconds(90).getEpochSecond();String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(json.write(Map.of("memberId",memberId,"nonce",nonce,"exp",exp)).getBytes(StandardCharsets.UTF_8));String token=payload+"."+sign(payload);redis.opsForValue().set("member-code:"+nonce,Long.toString(memberId),Duration.ofSeconds(90));return Map.of("token","lishuiwan://member-code?token="+token,"expiresAt",exp);}
  public CodeData validate(String raw){
    try{String token=raw.contains("token=")?raw.substring(raw.indexOf("token=")+6):raw;String[] parts=token.split("\\.");if(parts.length!=2||!constantEquals(sign(parts[0]),parts[1]))throw invalid();@SuppressWarnings("unchecked") Map<String,Object> p=json.read(new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),Map.class);long memberId=Long.parseLong(p.get("memberId").toString()), exp=Long.parseLong(p.get("exp").toString());String nonce=p.get("nonce").toString();if(Instant.now().getEpochSecond()>=exp||redis.opsForValue().get("member-code:"+nonce)==null)throw invalid();if(consumed.selectCount(new LambdaQueryWrapper<ConsumedMemberCode>().eq(ConsumedMemberCode::getNonce,nonce))>0)throw invalid();return new CodeData(memberId,nonce,exp);}catch(BizException e){throw e;}catch(Exception e){throw invalid();}
  }
  public void consume(CodeData code,String action){ConsumedMemberCode c=new ConsumedMemberCode();c.setNonce(code.nonce());c.setMemberId(code.memberId());c.setAction(action);c.setExpireAt(LocalDateTime.now().plusDays(1));consumed.insert(c);}
  public void clear(CodeData code){redis.delete("member-code:"+code.nonce());}
  private BizException invalid(){return new BizException(41001,"会员码已过期或已使用", HttpStatus.GONE);}
  private String sign(String v){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private boolean constantEquals(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
