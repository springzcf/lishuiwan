package com.lishuiwan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lishuiwan")
public class AppProperties {
  private final Jwt jwt = new Jwt();
  private final Wechat wechat = new Wechat();
  private final Upload upload = new Upload();
  private boolean mockPaymentEnabled;
  private boolean devLoginEnabled;

  @Data public static class Jwt { private String secret; private long memberExpireSeconds = 604800; private long adminExpireSeconds = 7200; }
  @Data public static class Wechat {
    private String appId; private String appSecret;
    private String verifyTemplateId; private String issueTemplateId; private String templateThingKey = "thing1"; private String templateTimeKey = "time2";
    private final OfficialAccount officialAccount = new OfficialAccount();
    private final Pay pay = new Pay();
  }
  @Data public static class OfficialAccount {
    private boolean enabled;
    private String appId;
    private String appSecret;
    private String oauthCallbackUrl;
    private String h5BaseUrl;
  }
  @Data public static class Pay {
    private boolean enabled; private String merchantId; private String merchantSerialNumber;
    private String privateKeyPath; private String apiV3Key; private String notifyUrl;
  }
  @Data public static class Upload { private String directory = "./data/uploads"; private String urlPrefix = "/static/"; private long maxBytes = 5242880; }
}
