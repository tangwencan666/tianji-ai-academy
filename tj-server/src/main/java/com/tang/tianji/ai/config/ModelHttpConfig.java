package com.tang.tianji.ai.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class ModelHttpConfig {
  @Bean
  public RestClientCustomizer modelTimeout() {
    return builder -> {
      var client =
          java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
      var factory = new JdkClientHttpRequestFactory(client);
      factory.setReadTimeout(Duration.ofSeconds(30));
      builder.requestFactory(factory);
    };
  }
}
