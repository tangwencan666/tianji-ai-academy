package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("ai_call_log")
public class AiCallLog {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String requestId;
  public Long userId;
  public String conversationId;
  public String model;
  public Long latency;
  public Integer toolCalls;
  public String status;
  public java.time.LocalDateTime createTime;
}
