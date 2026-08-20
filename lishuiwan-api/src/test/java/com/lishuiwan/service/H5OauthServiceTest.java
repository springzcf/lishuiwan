package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class H5OauthServiceTest {
  private final Map<String,String> storage=new HashMap<>();
  private WechatClient wechat;
  private AuthService auth;
  private H5OauthService service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp(){
    StringRedisTemplate redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    doAnswer(call->{storage.put(call.getArgument(0),call.getArgument(1));return null;}).when(values).set(anyString(),anyString(),any(Duration.class));
    when(values.getAndDelete(anyString())).thenAnswer(call->storage.remove(call.getArgument(0)));
    wechat=mock(WechatClient.class);auth=mock(AuthService.class);
    AppProperties properties=new AppProperties();properties.getWechat().getOfficialAccount().setH5BaseUrl("https://member.example.com/h5/");
    service=new H5OauthService(redis,wechat,auth,properties);
  }

  @Test
  void oauthStateAndTicketAreSingleUse(){
    when(wechat.officialAuthorizeUrl(anyString())).thenAnswer(call->"https://wechat.example/auth?state="+call.getArgument(0));
    String authorize=service.authorize("/h5/cards/9");String state=authorize.substring(authorize.indexOf("state=")+6);
    WechatClient.Identity identity=new WechatClient.Identity("oa-openid","union-1");when(wechat.officialIdentity("oauth-code")).thenReturn(identity);when(auth.officialMemberId(identity)).thenReturn(7L);
    String callback=service.callback("oauth-code",state);String ticket=callback.substring(callback.indexOf("ticket=")+7,callback.indexOf("&returnUrl="));
    when(auth.memberLogin(7L)).thenReturn(Map.of("token","jwt","member",Map.of("id",7L)));

    assertThat(service.exchange(ticket)).containsEntry("token","jwt");
    assertThatThrownBy(()->service.exchange(ticket)).isInstanceOf(BizException.class).hasMessageContaining("已使用或已过期");
    assertThatThrownBy(()->service.callback("oauth-code",state)).isInstanceOf(BizException.class).hasMessageContaining("已失效");
  }

  @Test
  void returnUrlMustStayInsideH5(){
    assertThat(service.safeReturnUrl(null)).isEqualTo("/h5/");
    assertThatThrownBy(()->service.safeReturnUrl("https://evil.example/h5/")) .isInstanceOf(BizException.class);
    assertThatThrownBy(()->service.safeReturnUrl("//evil.example/h5/")) .isInstanceOf(BizException.class);
    assertThatThrownBy(()->service.safeReturnUrl("/admin/")) .isInstanceOf(BizException.class);
  }
}
