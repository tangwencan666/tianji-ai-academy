package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long courseId;
  public Long chapterId;
  public Long lessonId;
  public Long teacherId;
  public String title;
  public String source;
  public String content;
  public String embedding;
  public String embeddingModel;
}
