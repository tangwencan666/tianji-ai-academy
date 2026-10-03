package com.tang.tianji.ai.tool;

import com.tang.tianji.service.TeacherStatisticsService;
import org.springframework.ai.tool.annotation.Tool;

public class TeacherTools {
  private final TeacherStatisticsService s;
  private final long course;

  public TeacherTools(TeacherStatisticsService s, long course) {
    this.s = s;
    this.course = course;
  }

  @Tool(description = "查询自己课程的学习汇总，无学生身份信息")
  public Object getCourseLearningStatistics() {
    return s.learning(course);
  }

  @Tool(description = "查询自己课程的考试汇总")
  public Object getCourseExamStatistics() {
    return s.exams(course);
  }

  @Tool(description = "查询自己课程常见错题统计")
  public Object getCommonWrongQuestions() {
    return s.wrong(course);
  }

  @Tool(description = "查询自己课程班级薄弱知识点")
  public Object getClassWeakKnowledgePoints() {
    return s.weak(course);
  }
}
