package com.tang.tianji.ai.tool;

import com.tang.tianji.service.*;
import org.springframework.ai.tool.annotation.Tool;

/**
 * Constructed per request. Course scope comes from the authenticated request, never from model
 * arguments.
 */
public class LearningTools {
  private final LearningService learning;
  private final ExamService exams;
  private final CourseService courses;
  private final long course;

  public LearningTools(LearningService l, ExamService e, CourseService c, long id) {
    learning = l;
    exams = e;
    courses = c;
    course = id;
  }

  @Tool(description = "查询当前用户所有课程学习记录")
  public Object getCurrentLearningProgress() {
    return learning.records();
  }

  @Tool(description = "查询当前课程学习进度")
  public Object getCourseProgress() {
    return learning.progress(course);
  }

  @Tool(description = "查询当前用户最近错题，不接受用户ID")
  public Object getRecentWrongQuestions() {
    return exams.wrongs();
  }

  @Tool(description = "查询Java统计的当前用户知识点掌握度")
  public Object getWeakKnowledgePoints() {
    return exams.mastery();
  }

  @Tool(description = "查询当前用户最近考试结果")
  public Object getRecentExamResults() {
    return exams.history();
  }

  @Tool(description = "查询当前用户最近七天学习时长")
  public Object getLearningTimeStatistics() {
    return learning.time();
  }

  @Tool(description = "查询当前课程真实可学习课时")
  public Object getRecommendedLessons() {
    var weak =
        exams.mastery().stream()
            .filter(
                k ->
                    ((Number) k.get("courseId")).longValue() == course
                        && ((Number) k.get("correctRate")).intValue() < 60)
            .map(k -> (String) k.get("name"))
            .toList();
    var done =
        learning.records().stream()
            .filter(r -> r.courseId == course && "COMPLETED".equals(r.status))
            .map(r -> r.lessonId)
            .toList();
    return courses.lessons(course).stream()
        .sorted(
            java.util.Comparator.comparingInt(
                (com.tang.tianji.entity.Lesson l) ->
                    weak.stream().anyMatch(k -> l.title.contains(k))
                        ? 0
                        : done.contains(l.id) ? 2 : 1))
        .map(
            l ->
                java.util.Map.of(
                    "id",
                    l.id,
                    "title",
                    l.title,
                    "estimatedMinutes",
                    l.estimatedMinutes,
                    "reason",
                    weak.stream().anyMatch(k -> l.title.contains(k))
                        ? "薄弱知识点匹配"
                        : done.contains(l.id) ? "已完成，可复习" : "尚未完成"))
        .toList();
  }
}
