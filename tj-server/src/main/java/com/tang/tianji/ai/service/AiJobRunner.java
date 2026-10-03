package com.tang.tianji.ai.service;

import com.tang.tianji.ai.security.AiGuard;
import com.tang.tianji.common.*;
import com.tang.tianji.entity.AiCallLog;
import com.tang.tianji.mapper.AiCallLogMapper;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiJobRunner {
  private final AiGuard guard;
  private final ExecutorService executor;
  private final AiCallLogMapper logs;
  private final Clock clock;
  private final String model;

  public AiJobRunner(
      AiGuard g,
      ExecutorService e,
      AiCallLogMapper l,
      Clock c,
      @Value("${spring.ai.openai.chat.options.model}") String m) {
    guard = g;
    executor = e;
    logs = l;
    clock = c;
    model = m;
  }

  public Object run(Supplier<Object> job) {
    guard.acquire();
    var p = UserContext.current();
    Future<Object> future;
    try {
      future =
          executor.submit(
              () -> {
                UserContext.set(p);
                long start = System.nanoTime();
                String status = "ERROR";
                try {
                  var result = job.get();
                  if (Thread.currentThread().isInterrupted())
                    throw new BusinessException(504, "AI请求超时");
                  status = "OK";
                  return result;
                } finally {
                  try {
                    var log = new AiCallLog();
                    log.requestId = UUID.randomUUID().toString();
                    log.userId = p.id();
                    log.model = model;
                    log.latency = (System.nanoTime() - start) / 1000000;
                    log.toolCalls = 0;
                    log.status = Thread.currentThread().isInterrupted() ? "TIMEOUT" : status;
                    log.createTime = LocalDateTime.now(clock);
                    logs.insert(log);
                  } finally {
                    guard.release(p.id());
                    UserContext.clear();
                  }
                }
              });
    } catch (RejectedExecutionException e) {
      guard.release(p.id());
      throw new BusinessException(429, "AI服务繁忙");
    }
    try {
      return future.get(90, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new BusinessException(504, "AI请求超过90秒，请减少资料长度或缩小任务范围");
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new BusinessException(503, "AI请求已取消");
    } catch (ExecutionException e) {
      if (e.getCause() instanceof BusinessException b) throw b;
      throw new BusinessException(503, "AI服务暂时不可用");
    }
  }
}
