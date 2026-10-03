package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("exam")
public class Exam {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public String title;
  public Integer durationMinutes;
  public String status;
}
