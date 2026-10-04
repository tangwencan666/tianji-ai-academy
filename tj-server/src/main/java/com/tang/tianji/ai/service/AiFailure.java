package com.tang.tianji.ai.service;

import com.tang.tianji.common.BusinessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public final class AiFailure {
  public static BusinessException safe(Throwable error) {
    for(Throwable e=error;e!=null;e=e.getCause()) {
      if(e instanceof BusinessException b) return b;
      int status=e instanceof RestClientResponseException r?r.getStatusCode().value():
          e instanceof WebClientResponseException w?w.getStatusCode().value():0;
      if(status==401||status==403) return new BusinessException(503,"模型认证失败，请管理员检查配置；课程和考试仍可使用");
      if(status==402) return new BusinessException(503,"模型账户余额不足；课程和考试仍可使用");
      if(status==429) return new BusinessException(429,"模型服务繁忙或额度受限，请稍后重试");
      if(status>=500) return new BusinessException(503,"模型服务暂时不可用；课程和考试仍可使用");
      if(e instanceof java.util.concurrent.TimeoutException || e instanceof java.net.http.HttpTimeoutException)
        return new BusinessException(504,"模型请求超时，请稍后重试");
    }
    return new BusinessException(503,"模型网络或输出异常，请稍后重试；课程和考试仍可使用");
  }
}
