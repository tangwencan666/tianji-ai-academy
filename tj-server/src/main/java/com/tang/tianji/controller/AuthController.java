package com.tang.tianji.controller;

import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
  private final AuthService service;

  public AuthController(AuthService s) {
    service = s;
  }

  @PostMapping("/auth/register")
  public Object register(@Valid @RequestBody Credentials c) {
    return Result.ok(service.register(c));
  }

  @PostMapping("/auth/login")
  public Object login(@Valid @RequestBody Credentials c) {
    return Result.ok(service.login(c));
  }

  @GetMapping("/auth/me")
  public Object me() {
    return Result.ok(service.find(UserContext.getCurrentUserId()));
  }

  @GetMapping("/admin/users")
  public Object users() {
    return Result.ok(service.list());
  }

  @PatchMapping("/admin/users/{id}/role")
  public Object role(@PathVariable long id, @RequestParam String role) {
    service.role(id, role);
    return Result.ok(null);
  }
}
