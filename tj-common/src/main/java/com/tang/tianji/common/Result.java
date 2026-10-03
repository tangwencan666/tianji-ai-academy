package com.tang.tianji.common;

public record Result<T>(int code, String message, T data) {
  public static <T> Result<T> ok(T data) {
    return new Result<>(0, "ok", data);
  }
}
