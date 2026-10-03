package com.tang.tianji.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("course")
public class Course {
  @TableId(type = IdType.AUTO)
  public Long id;

  public String title;
  public String description;
  public String cover;
  public Long teacherId;
  public String category;
  public String difficulty;
  public String status;
  public Integer totalLessons;
  public java.time.LocalDateTime createTime;
  public java.time.LocalDateTime updateTime;
}
