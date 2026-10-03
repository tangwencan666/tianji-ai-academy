package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("exam_question")
public class ExamQuestion {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long examId;
  public Long questionId;
  public Integer maxScore;
  public Integer sortOrder;
}
