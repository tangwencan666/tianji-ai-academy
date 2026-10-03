package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("learning_record")
public class LearningRecord {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long courseId;
  public Long chapterId;
  public Long lessonId;
  public Integer duration;
  public Integer progress;
  public Integer lastPosition;
  public String status;
  public java.time.LocalDateTime lastLearnTime;
  public java.time.LocalDateTime completedTime;
}
