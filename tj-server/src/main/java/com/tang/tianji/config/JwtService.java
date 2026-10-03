package com.tang.tianji.config;

import com.tang.tianji.common.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final SecretKey key;

  public JwtService(@Value("${app.jwt-secret}") String secret) {
    if (secret.length() < 32) throw new IllegalArgumentException("JWT_SECRET 至少32字符");
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String issue(long id) {
    return Jwts.builder()
        .subject(Long.toString(id))
        .issuer("tianji")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 7200000))
        .signWith(key)
        .compact();
  }

  public long parse(String token) {
    try {
      return Long.parseLong(
          Jwts.parser()
              .verifyWith(key)
              .requireIssuer("tianji")
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject());
    } catch (Exception e) {
      throw new BusinessException(401, "登录已失效");
    }
  }
}
