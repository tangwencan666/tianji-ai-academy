package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("question")
public class Question {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public Long chapterId;
  public String type;
  public String stem;
  public String options;
  public String answer;
  public String explanation;
  public String difficulty;
  public String status;
}
