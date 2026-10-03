package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("study_plan_day")
public class StudyPlanDay {
  @TableId(type = IdType.AUTO)
  public Long id;

  public Long planId;
  public java.time.LocalDate studyDate;
  public String topics;
  public String lessonIds;
  public Integer practiceCount;
  public String reviewKnowledgePoints;
  public Integer estimatedMinutes;
  public Boolean completed;
}
