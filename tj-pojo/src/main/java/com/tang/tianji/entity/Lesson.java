package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("lesson")
public class Lesson {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public Long chapterId;
  public String title;
  public String videoUrl;
  public String content;
  public String attachment;
  public Integer estimatedMinutes;
  public Integer sortOrder;
}
