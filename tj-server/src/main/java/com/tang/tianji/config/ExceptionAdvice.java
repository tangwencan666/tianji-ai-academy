package com.tang.tianji.config;

import com.tang.tianji.common.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ExceptionAdvice {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Result<Void>> business(BusinessException e) {
    return ResponseEntity.status(e.status).body(new Result<>(e.status, e.getMessage(), null));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Result<Void>> invalid(Exception e) {
    return ResponseEntity.badRequest().body(new Result<>(400, "参数不合法，请检查输入", null));
  }

  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<Result<Void>> duplicate(Exception e) {
    return ResponseEntity.status(409).body(new Result<>(409, "记录已存在，请刷新重试", null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .error("request_failed type={}", e.getClass().getSimpleName());
    return ResponseEntity.status(500).body(new Result<>(500, "服务暂时不可用，请稍后重试", null));
  }
}
