package com.lishuiwan.security;

import com.lishuiwan.config.AppProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {
  @Test void issuesTypedMemberToken(){AppProperties p=new AppProperties();p.getJwt().setSecret("01234567890123456789012345678901-secure");TokenService s=new TokenService(p);var claims=s.parse(s.member(42));assertThat(claims.getSubject()).isEqualTo("42");assertThat(claims.get("type",String.class)).isEqualTo("member");}
}
