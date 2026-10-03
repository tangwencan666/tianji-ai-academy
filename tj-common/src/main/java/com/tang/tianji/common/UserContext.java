package com.tang.tianji.common;

public final class UserContext {
  public record Principal(long id, String role) {}

  private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

  public static void set(Principal p) {
    CURRENT.set(p);
  }

  public static Principal current() {
    var p = CURRENT.get();
    if (p == null) throw new BusinessException(401, "请先登录");
    return p;
  }

  public static long getCurrentUserId() {
    return current().id();
  }

  public static void clear() {
    CURRENT.remove();
  }

  public static void require(String... roles) {
    for (String r : roles) if (r.equals(current().role())) return;
    throw new BusinessException(403, "权限不足");
  }
}
