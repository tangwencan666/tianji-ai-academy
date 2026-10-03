package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("wrong_question")
public class WrongQuestion {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long questionId;
  public Integer wrongCount;
  public String masteryStatus;
  public java.time.LocalDateTime lastWrongTime;
}
