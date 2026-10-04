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
  private final ScheduledExecutorService deadlines;

  public AiJobRunner(
      AiGuard g,
      ExecutorService e,
      AiCallLogMapper l,
      Clock c,
      @Value("${spring.ai.openai.chat.options.model}") String m, ScheduledExecutorService deadlines) {
    this.deadlines=deadlines;
    guard = g;
    executor = e;
    logs = l;
    clock = c;
    model = m;
  }

  public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(
      java.util.function.Function<java.util.function.Consumer<String>,Object> action) {
    guard.acquire();
    var principal=UserContext.current();
    var emitter=new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(95000L);
    var stopped=new java.util.concurrent.atomic.AtomicBoolean();
    var future=new java.util.concurrent.atomic.AtomicReference<Future<?>>();
    Runnable cancel=()->{stopped.set(true);var f=future.get();if(f!=null)f.cancel(true);};
    java.util.function.BiConsumer<String,Object> send=(name,value)->{
      try{emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name(name)
          .data(value instanceof String text ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text) : value,
              org.springframework.http.MediaType.APPLICATION_JSON));}
      catch(java.io.IOException e){throw new BusinessException(503,"连接已关闭");}
    };
    emitter.onTimeout(cancel);emitter.onError(e->cancel.run());emitter.onCompletion(cancel);
    var timeout=deadlines.schedule(()->{
      if(!stopped.get()) {try{send.accept("error","AI请求超过90秒，请缩小任务范围");}catch(Exception ignored){} cancel.run();emitter.complete();}
    },90,TimeUnit.SECONDS);
    try{
      future.set(executor.submit(()->{
        UserContext.set(principal);long start=System.nanoTime();String status="ERROR";
        try{
          Object result=action.apply(s->send.accept("status",s));
          if(!stopped.get()){send.accept("result",result);send.accept("done",java.util.Map.of("complete",true));status="OK";}
        }catch(Exception e){if(!stopped.get())try{send.accept("error",AiFailure.safe(e).getMessage());}catch(Exception ignored){}}
        finally{
          timeout.cancel(false);
          try{
            var log=new AiCallLog();log.requestId=UUID.randomUUID().toString();log.userId=principal.id();
            log.model=model;log.latency=(System.nanoTime()-start)/1000000;log.toolCalls=0;
            log.status=stopped.get()?"CANCELLED":status;log.createTime=LocalDateTime.now(clock);logs.insert(log);
          }finally{guard.release(principal.id());UserContext.clear();stopped.set(true);emitter.complete();}
        }
      }));
    }catch(RejectedExecutionException e){timeout.cancel(false);guard.release(principal.id());throw new BusinessException(429,"AI服务繁忙");}
    return emitter;
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
