package com.lishuiwan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lishuiwan.common.BizException;
import com.lishuiwan.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class WechatClient {
  public record Identity(String openid,String unionid) {}
  private final AppProperties p; private final RestClient http; private final StringRedisTemplate redis;
  public WechatClient(AppProperties p, RestClient.Builder builder, StringRedisTemplate redis) { this.p=p; this.http=builder.build(); this.redis=redis; }
  public Identity miniIdentity(String code) {
    if (p.isDevLoginEnabled() && code != null && code.startsWith("dev:")) {
      String id=code.substring(4);return new Identity("dev_"+id,"dev_union_"+id);
    }
    requireMiniConfig();
    JsonNode node = http.get().uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",p.getWechat().getAppId(),p.getWechat().getAppSecret(),code).retrieve().body(JsonNode.class);
    if (node == null || !node.hasNonNull("openid")) throw new BizException(40003,"微信登录失败");
    return new Identity(node.get("openid").asText(),text(node,"unionid"));
  }
  public String officialAuthorizeUrl(String state) {
    AppProperties.OfficialAccount oa=requireOfficialConfig();
    return "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+encode(oa.getAppId())
      +"&redirect_uri="+encode(oa.getOauthCallbackUrl())
      +"&response_type=code&scope=snsapi_base&state="+encode(state)+"#wechat_redirect";
  }
  public Identity officialIdentity(String code) {
    AppProperties.OfficialAccount oa=requireOfficialConfig();
    JsonNode node=http.get().uri("https://api.weixin.qq.com/sns/oauth2/access_token?appid={a}&secret={s}&code={c}&grant_type=authorization_code",oa.getAppId(),oa.getAppSecret(),code).retrieve().body(JsonNode.class);
    if(node==null||!node.hasNonNull("openid")||node.has("errcode"))throw new BizException(40003,"服务号授权失败");
    return new Identity(node.get("openid").asText(),text(node,"unionid"));
  }
  public String phone(String code) {
    if (p.isDevLoginEnabled() && code != null && code.matches("dev:1[3-9]\\d{9}")) return code.substring(4);
    String token = miniAccessToken();
    JsonNode node = http.post().uri("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={t}",token)
      .contentType(MediaType.APPLICATION_JSON).body(Map.of("code",code)).retrieve().body(JsonNode.class);
    if (node == null || node.path("errcode").asInt(-1) != 0 || !node.path("phone_info").hasNonNull("phoneNumber")) throw new BizException(40004,"手机号授权失败");
    return node.path("phone_info").path("phoneNumber").asText();
  }
  public int sendSubscribe(String openid,String templateId,String page,String thing,String time){JsonNode node=http.post().uri("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={t}",miniAccessToken()).contentType(MediaType.APPLICATION_JSON).body(Map.of("touser",openid,"template_id",templateId,"page",page,"miniprogram_state","formal","lang","zh_CN","data",Map.of(p.getWechat().getTemplateThingKey(),Map.of("value",truncate(thing,20)),p.getWechat().getTemplateTimeKey(),Map.of("value",time)))).retrieve().body(JsonNode.class);return node==null?-1:node.path("errcode").asInt(-1);}
  private String miniAccessToken() {
    requireMiniConfig(); String key="wechat:access-token:"+p.getWechat().getAppId(); String cached=redis.opsForValue().get(key); if(cached!=null)return cached;
    JsonNode node=http.get().uri("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={a}&secret={s}",p.getWechat().getAppId(),p.getWechat().getAppSecret()).retrieve().body(JsonNode.class);
    if(node==null||!node.hasNonNull("access_token"))throw new BizException(50001,"微信服务暂不可用");
    String token=node.get("access_token").asText(); redis.opsForValue().set(key,token,Duration.ofSeconds(Math.max(60,node.path("expires_in").asLong(7200)-300))); return token;
  }
  private void requireMiniConfig(){
    if (p.getWechat().getAppId() == null || p.getWechat().getAppId().isBlank()
        || p.getWechat().getAppSecret() == null || p.getWechat().getAppSecret().isBlank()
        || p.getWechat().getAppSecret().contains("change-me")) {
      throw new BizException(50002,"微信参数未配置");
    }
  }
  private AppProperties.OfficialAccount requireOfficialConfig(){
    AppProperties.OfficialAccount oa=p.getWechat().getOfficialAccount();
    if(!oa.isEnabled()||blank(oa.getAppId())||blank(oa.getAppSecret())||blank(oa.getOauthCallbackUrl())||blank(oa.getH5BaseUrl())||oa.getAppSecret().contains("change-me"))throw new BizException(50002,"服务号网页授权参数未配置");
    return oa;
  }
  private String text(JsonNode node,String key){return node.hasNonNull(key)&&!node.get(key).asText().isBlank()?node.get(key).asText():null;}
  private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
  private boolean blank(String value){return value==null||value.isBlank();}
  private String truncate(String v,int max){return v==null?"":v.substring(0,Math.min(v.length(),max));}
}
