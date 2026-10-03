package com.tang.tianji.service;

import com.tang.tianji.common.BusinessException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Single-node admission gate; both callers first acquire the same DB user-row lock. */
@Component
public class AiActivity {
  private final ConcurrentHashMap<Long, Boolean> running = new ConcurrentHashMap<>();

  public void acquire(long uid) {
    BusinessException.check(running.putIfAbsent(uid, true) == null, 429, "已有AI请求正在处理");
  }

  public void release(long uid) {
    running.remove(uid);
  }

  public void assertIdle(long uid) {
    BusinessException.check(!running.containsKey(uid), 409, "请等待AI请求完成或取消后再开始考试");
  }
}
