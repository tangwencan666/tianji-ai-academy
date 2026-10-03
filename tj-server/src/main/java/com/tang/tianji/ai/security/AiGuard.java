package com.tang.tianji.ai.security;

import com.tang.tianji.common.*;
import com.tang.tianji.mapper.UserMapper;
import com.tang.tianji.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiGuard {
  private final boolean enabled;
  private final ExamService exams;
  private final AiActivity activity;
  private final UserMapper users;

  public AiGuard(@Value("${app.ai-enabled}") boolean e, ExamService x, AiActivity a, UserMapper u) {
    enabled = e;
    exams = x;
    activity = a;
    users = u;
  }

  public void check() {
    exams.noActive();
    BusinessException.check(enabled, 503, "AI尚未启用，请配置模型与Embedding密钥；课程和考试可正常使用");
  }

  @Transactional
  public void acquire() {
    long uid = UserContext.getCurrentUserId();
    users.lock(uid);
    check();
    activity.acquire(uid);
  }

  public void release(long id) {
    activity.release(id);
  }
}
