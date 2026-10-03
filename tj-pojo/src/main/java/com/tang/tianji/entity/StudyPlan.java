package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("study_plan")
public class StudyPlan {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long userId;
  public Long courseId;
  public String goal;
  public java.time.LocalDate startDate;
  public java.time.LocalDate endDate;
  public Integer dailyMinutes;
  public String status;
  public java.time.LocalDateTime createTime;
}
