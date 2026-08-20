package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.common.Ids;
import com.lishuiwan.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class H5OauthService {
  private static final Duration STATE_TTL=Duration.ofMinutes(5);
  private static final Duration TICKET_TTL=Duration.ofSeconds(60);
  private final StringRedisTemplate redis;
  private final WechatClient wechat;
  private final AuthService auth;
  private final AppProperties properties;

  public H5OauthService(StringRedisTemplate redis,WechatClient wechat,AuthService auth,AppProperties properties){this.redis=redis;this.wechat=wechat;this.auth=auth;this.properties=properties;}

  public String authorize(String returnUrl){
    String safe=safeReturnUrl(returnUrl),state=Ids.nonce();
    redis.opsForValue().set("h5:oauth:state:"+state,safe,STATE_TTL);
    return wechat.officialAuthorizeUrl(state);
  }

  public String callback(String code,String state){
    if(code==null||code.isBlank()||state==null||state.isBlank())throw invalid("服务号授权参数不完整");
    String returnUrl=redis.opsForValue().getAndDelete("h5:oauth:state:"+state);
    if(returnUrl==null)throw invalid("服务号授权已失效，请重新进入");
    long memberId=auth.officialMemberId(wechat.officialIdentity(code));
    String ticket=Ids.nonce();redis.opsForValue().set("h5:oauth:ticket:"+ticket,Long.toString(memberId),TICKET_TTL);
    String base=properties.getWechat().getOfficialAccount().getH5BaseUrl();
    if(base==null||base.isBlank())throw new BizException(50002,"H5 地址未配置");
    return stripSlash(base)+"/auth/callback?ticket="+encode(ticket)+"&returnUrl="+encode(returnUrl);
  }

  public Map<String,Object> exchange(String ticket){
    if(ticket==null||!ticket.matches("[a-fA-F0-9]{32}"))throw invalid("登录票据无效");
    String memberId=redis.opsForValue().getAndDelete("h5:oauth:ticket:"+ticket);
    if(memberId==null)throw invalid("登录票据已使用或已过期");
    try{return auth.memberLogin(Long.parseLong(memberId));}catch(NumberFormatException e){throw invalid("登录票据无效");}
  }

  String safeReturnUrl(String value){
    String candidate=value==null||value.isBlank()?"/h5/":value;
    try{URI uri=URI.create(candidate);if(uri.isAbsolute()||uri.getAuthority()!=null||uri.getFragment()!=null||uri.getPath()==null||!uri.getPath().startsWith("/h5/"))throw invalid("登录返回地址无效");return candidate;}
    catch(IllegalArgumentException e){throw invalid("登录返回地址无效");}
  }

  private BizException invalid(String message){return new BizException(40007,message,HttpStatus.BAD_REQUEST);}
  private String stripSlash(String value){return value.endsWith("/")?value.substring(0,value.length()-1):value;}
  private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
