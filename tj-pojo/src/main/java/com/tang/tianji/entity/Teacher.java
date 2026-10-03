package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("teacher")
public class Teacher {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public String bio;
}
