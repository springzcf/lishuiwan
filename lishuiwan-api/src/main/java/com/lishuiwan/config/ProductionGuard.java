package com.lishuiwan.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ProductionGuard {
  private final AppProperties p; private final Environment env;
  public ProductionGuard(AppProperties p, Environment env) { this.p=p; this.env=env; }
  @PostConstruct void check() {
    if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
      if (p.isMockPaymentEnabled() || p.isDevLoginEnabled()) throw new IllegalStateException("Mock payment and dev login must be disabled in prod");
      if (p.getJwt().getSecret().contains("replace-") || p.getJwt().getSecret().contains("change-me")) throw new IllegalStateException("JWT_SECRET or MYSQL_PASSWORD must be replaced in prod");
      String privateKeyPath = p.getWechat().getPay().getPrivateKeyPath();
      if (p.getWechat().getPay().isEnabled()
          && (privateKeyPath == null || privateKeyPath.isBlank() || !Files.isReadable(Path.of(privateKeyPath)))) {
        throw new IllegalStateException("WeChat merchant private key is not readable");
      }
      AppProperties.OfficialAccount oa=p.getWechat().getOfficialAccount();
      if(oa.isEnabled()){
        if(blank(oa.getAppId())||blank(oa.getAppSecret())||blank(oa.getOauthCallbackUrl())||blank(oa.getH5BaseUrl()))throw new IllegalStateException("Official account OAuth configuration is incomplete");
        if(!oa.getOauthCallbackUrl().startsWith("https://")||!oa.getH5BaseUrl().startsWith("https://"))throw new IllegalStateException("Official account OAuth and H5 URLs must use HTTPS in prod");
      }
    }
  }
  private boolean blank(String value){return value==null||value.isBlank()||value.contains("change-me");}
}
