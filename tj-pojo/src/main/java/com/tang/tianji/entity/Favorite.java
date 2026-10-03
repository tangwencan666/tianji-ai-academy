package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("favorite")
public class Favorite {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long courseId;
}
