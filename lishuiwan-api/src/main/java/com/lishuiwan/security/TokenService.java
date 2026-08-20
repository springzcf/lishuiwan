package com.lishuiwan.security;

import com.lishuiwan.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {
  private final AppProperties properties;
  private final SecretKey key;
  public TokenService(AppProperties properties) {
    this.properties = properties;
    if (properties.getJwt().getSecret() == null || properties.getJwt().getSecret().length() < 32) throw new IllegalStateException("JWT_SECRET must contain at least 32 characters");
    this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
  }
  public String member(long id) { return issue(Long.toString(id), "member", Map.of(), properties.getJwt().getMemberExpireSeconds()); }
  public String admin(long id) { return issue(Long.toString(id), "admin", Map.of(), properties.getJwt().getAdminExpireSeconds()); }
  public String registration(String openid,String unionid) { Map<String,Object> claims=new HashMap<>();if(unionid!=null)claims.put("unionid",unionid);return issue(openid,"registration",claims,300); }
  private String issue(String subject, String type, Map<String,Object> claims, long seconds) {
    Instant now = Instant.now();
    return Jwts.builder().subject(subject).claim("type", type).claims(claims).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(seconds))).signWith(key).compact();
  }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
