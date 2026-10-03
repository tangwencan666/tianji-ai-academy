package com.tang.tianji.interceptor;

import com.tang.tianji.common.*;
import com.tang.tianji.config.JwtService;
import com.tang.tianji.service.AuthService;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class AuthInterceptor implements AsyncHandlerInterceptor {
  private final JwtService jwt;
  private final AuthService auth;

  public AuthInterceptor(JwtService j, AuthService a) {
    jwt = j;
    auth = a;
  }

  public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
    UserContext.clear();
    String h = req.getHeader("Authorization");
    BusinessException.check(h != null && h.startsWith("Bearer "), 401, "请先登录");
    var u = auth.find(jwt.parse(h.substring(7)));
    UserContext.set(new UserContext.Principal(u.id, u.role));
    return true;
  }

  public void afterCompletion(HttpServletRequest r, HttpServletResponse s, Object h, Exception e) {
    UserContext.clear();
  }

  public void afterConcurrentHandlingStarted(
      HttpServletRequest r, HttpServletResponse s, Object h) {
    UserContext.clear();
  }
}
