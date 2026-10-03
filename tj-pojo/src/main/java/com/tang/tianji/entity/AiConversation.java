package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ai_conversation")
public class AiConversation {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long courseId;
  public String conversationKey;
  public java.time.LocalDateTime createTime;
}
