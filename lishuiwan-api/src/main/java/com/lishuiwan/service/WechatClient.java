package com.lishuiwan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.lishuiwan.common.BizException;
import com.lishuiwan.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
public class WechatClient {
  private final AppProperties p; private final RestClient http; private final StringRedisTemplate redis;
  public WechatClient(AppProperties p, RestClient.Builder builder, StringRedisTemplate redis) { this.p=p; this.http=builder.build(); this.redis=redis; }
  public String openid(String code) {
    if (p.isDevLoginEnabled() && code != null && code.startsWith("dev:")) return "dev_" + code.substring(4);
    requireConfig();
    JsonNode node = http.get().uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",p.getWechat().getAppId(),p.getWechat().getAppSecret(),code).retrieve().body(JsonNode.class);
    if (node == null || !node.hasNonNull("openid")) throw new BizException(40003,"微信登录失败");
    return node.get("openid").asText();
  }
  public String phone(String code) {
    if (p.isDevLoginEnabled() && code != null && code.matches("dev:1[3-9]\\d{9}")) return code.substring(4);
    String token = accessToken();
    JsonNode node = http.post().uri("https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={t}",token)
      .contentType(MediaType.APPLICATION_JSON).body(Map.of("code",code)).retrieve().body(JsonNode.class);
    if (node == null || node.path("errcode").asInt(-1) != 0 || !node.path("phone_info").hasNonNull("phoneNumber")) throw new BizException(40004,"手机号授权失败");
    return node.path("phone_info").path("phoneNumber").asText();
  }
  public int sendSubscribe(String openid,String templateId,String page,String thing,String time){JsonNode node=http.post().uri("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={t}",accessToken()).contentType(MediaType.APPLICATION_JSON).body(Map.of("touser",openid,"template_id",templateId,"page",page,"miniprogram_state","formal","lang","zh_CN","data",Map.of(p.getWechat().getTemplateThingKey(),Map.of("value",truncate(thing,20)),p.getWechat().getTemplateTimeKey(),Map.of("value",time)))).retrieve().body(JsonNode.class);return node==null?-1:node.path("errcode").asInt(-1);}
  private String accessToken() {
    requireConfig(); String key="wechat:access-token:"+p.getWechat().getAppId(); String cached=redis.opsForValue().get(key); if(cached!=null)return cached;
    JsonNode node=http.get().uri("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={a}&secret={s}",p.getWechat().getAppId(),p.getWechat().getAppSecret()).retrieve().body(JsonNode.class);
    if(node==null||!node.hasNonNull("access_token"))throw new BizException(50001,"微信服务暂不可用");
    String token=node.get("access_token").asText(); redis.opsForValue().set(key,token,Duration.ofSeconds(Math.max(60,node.path("expires_in").asLong(7200)-300))); return token;
  }
  private void requireConfig(){
    if (p.getWechat().getAppId() == null || p.getWechat().getAppId().isBlank()
        || p.getWechat().getAppSecret() == null || p.getWechat().getAppSecret().isBlank()
        || p.getWechat().getAppSecret().contains("change-me")) {
      throw new BizException(50002,"微信参数未配置");
    }
  }
  private String truncate(String v,int max){return v==null?"":v.substring(0,Math.min(v.length(),max));}
}
