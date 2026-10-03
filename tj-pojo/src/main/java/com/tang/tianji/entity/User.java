package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("app_user")
public class User {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String username;
  @com.fasterxml.jackson.annotation.JsonIgnore public String passwordHash;
  public String displayName;
  public String role;
  public java.time.LocalDateTime createTime;
}
