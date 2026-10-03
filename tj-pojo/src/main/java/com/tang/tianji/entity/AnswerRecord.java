package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("answer_record")
public class AnswerRecord {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long examRecordId;
  public Long questionId;
  public String answer;
  public Integer score;
  public Integer maxScore;
  public Boolean correct;
  public String reviewStatus;
  public String aiFeedback;
  public java.time.LocalDateTime createTime;
}
