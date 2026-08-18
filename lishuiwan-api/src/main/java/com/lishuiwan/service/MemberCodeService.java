package com.lishuiwan.service;

import com.lishuiwan.common.*;
import com.lishuiwan.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.nio.charset.StandardCharsets; import java.util.*;

@Service
public class MemberCodeService {
  public record CodeData(long memberId){}
  private static final String PAYLOAD_PREFIX="member:";
  private final QrCodeService qrCodes; private final byte[] key;
  public MemberCodeService(QrCodeService qrCodes,AppProperties p){this.qrCodes=qrCodes;this.key=p.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);}
  public Map<String,Object> issue(long memberId){String payload=Base64.getUrlEncoder().withoutPadding().encodeToString((PAYLOAD_PREFIX+memberId).getBytes(StandardCharsets.UTF_8));String token="lishuiwan://member-id?token="+payload+"."+sign(payload);return Map.of("token",token,"qrImage",qrCodes.asPngDataUrl(token));}
  public CodeData validate(String raw){
    try{String token=raw.contains("token=")?raw.substring(raw.indexOf("token=")+6):raw;String[] parts=token.split("\\.");if(parts.length!=2||!constantEquals(sign(parts[0]),parts[1]))throw invalid();String value=new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8);if(!value.startsWith(PAYLOAD_PREFIX))throw invalid();return new CodeData(Long.parseLong(value.substring(PAYLOAD_PREFIX.length())));}catch(BizException e){throw e;}catch(Exception e){throw invalid();}
  }
  private BizException invalid(){return new BizException(41001,"会员身份码无效", HttpStatus.GONE);}
  private String sign(String v){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private boolean constantEquals(String a,String b){return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
