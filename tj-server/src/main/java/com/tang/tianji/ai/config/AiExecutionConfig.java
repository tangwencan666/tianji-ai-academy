package com.tang.tianji.ai.config;

import java.util.concurrent.*;
import org.springframework.context.annotation.*;

@Configuration
public class AiExecutionConfig {
  @Bean(destroyMethod = "shutdownNow")
  public ScheduledExecutorService aiDeadlines() {
    return Executors.newSingleThreadScheduledExecutor(r -> {
      var t=new Thread(r,"ai-deadlines");t.setDaemon(true);return t;
    });
  }
  @Bean(destroyMethod = "shutdownNow")
  @Primary
  public ExecutorService aiExecutor() {
    return new ThreadPoolExecutor(
        4, 4, 30, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.AbortPolicy());
  }
}
