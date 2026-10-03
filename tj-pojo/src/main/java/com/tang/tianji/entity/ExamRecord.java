package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("exam_record")
public class ExamRecord {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long examId;
  public String status;
  public Integer score;
  public Integer maxScore;
  public java.time.LocalDateTime startTime;
  public java.time.LocalDateTime submitTime;
}
