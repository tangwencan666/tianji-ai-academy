package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.common.*;
import com.tang.tianji.config.JwtService;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserMapper users;
  private final TeacherMapper teachers;
  private final JwtService jwt;
  private final Clock clock;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

  public AuthService(UserMapper u, TeacherMapper t, JwtService j, Clock c) {
    users = u;
    teachers = t;
    jwt = j;
    clock = c;
  }

  public User find(long id) {
    var u = users.selectById(id);
    BusinessException.check(u != null, 401, "用户不存在");
    return u;
  }

  public Map<String, Object> login(Credentials c) {
    var u = users.selectOne(new QueryWrapper<User>().eq("username", c.username()));
    BusinessException.check(
        u != null && encoder.matches(c.password(), u.passwordHash), 401, "用户名或密码错误");
    return Map.of("token", jwt.issue(u.id), "user", u);
  }

  public Map<String, Object> register(Credentials c) {
    BusinessException.check(
        c.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
        400,
        "密码UTF-8长度不能超过72字节");
    var u = new User();
    u.username = c.username();
    u.passwordHash = encoder.encode(c.password());
    u.displayName = c.username();
    u.role = "STUDENT";
    u.createTime = LocalDateTime.now(clock);
    users.insert(u);
    return Map.of("token", jwt.issue(u.id), "user", u);
  }

  public List<User> list() {
    UserContext.require("ADMIN");
    return users.selectList(new QueryWrapper<User>().orderByDesc("id").last("LIMIT 200"));
  }

  @Transactional
  public void role(long id, String role) {
    UserContext.require("ADMIN");
    BusinessException.check(Set.of("STUDENT", "TEACHER", "ADMIN").contains(role), 400, "无效角色");
    BusinessException.check(id != UserContext.getCurrentUserId(), 400, "不能修改自己的角色");
    var u = find(id);
    u.role = role;
    users.updateById(u);
    if (role.equals("TEACHER")
        && teachers.selectCount(new QueryWrapper<Teacher>().eq("user_id", id)) == 0) {
      var t = new Teacher();
      t.userId = id;
      t.bio = "";
      teachers.insert(t);
    }
  }
}
