package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ai_message")
public class AiMessage {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long conversationId;
  public String role;
  public String content;
  public java.time.LocalDateTime createTime;
}
