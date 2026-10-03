package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question_knowledge")
public class QuestionKnowledge {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long questionId;
  public Long knowledgePointId;
}
