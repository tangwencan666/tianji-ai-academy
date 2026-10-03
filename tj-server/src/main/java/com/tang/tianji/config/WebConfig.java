package com.tang.tianji.config;

import com.tang.tianji.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor auth;

  public WebConfig(AuthInterceptor a) {
    auth = a;
  }

  public void addInterceptors(InterceptorRegistry r) {
    r.addInterceptor(auth)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/auth/login", "/api/auth/register");
  }
}
