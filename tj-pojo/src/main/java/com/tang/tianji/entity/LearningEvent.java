package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("learning_event")
public class LearningEvent {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long courseId;
  public Long lessonId;
  public Integer duration;
  public java.time.LocalDateTime createTime;
}
