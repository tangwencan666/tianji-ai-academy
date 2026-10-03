package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("chapter")
public class Chapter {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public String title;
  public Integer sortOrder;
}
