package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("knowledge_point")
public class KnowledgePoint {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public String name;
}
