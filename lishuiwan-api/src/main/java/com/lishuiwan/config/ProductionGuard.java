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
      if (p.getJwt().getSecret().startsWith("replace-") || p.getJwt().getSecret().contains("change-me")) throw new IllegalStateException("JWT_SECRET must be replaced in prod");
      if (p.getWechat().getAppId().isBlank() || p.getWechat().getAppSecret().isBlank() || p.getWechat().getAppSecret().contains("change-me")) throw new IllegalStateException("WeChat credentials are required in prod");
      String privateKeyPath = p.getWechat().getPay().getPrivateKeyPath();
      if (p.getWechat().getPay().isEnabled()
          && (privateKeyPath == null || privateKeyPath.isBlank() || !Files.isReadable(Path.of(privateKeyPath)))) {
        throw new IllegalStateException("WeChat merchant private key is not readable");
      }
    }
  }
}
