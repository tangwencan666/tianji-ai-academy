package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class TeacherStatisticsService {
  private final CourseService courses;
  private final LearningRecordMapper learning;
  private final ExamMapper exams;
  private final ExamRecordMapper results;
  private final QuestionMapper questions;
  private final AnswerRecordMapper answers;
  private final KnowledgePointMapper points;
  private final QuestionKnowledgeMapper links;

  public TeacherStatisticsService(
      CourseService c,
      LearningRecordMapper l,
      ExamMapper e,
      ExamRecordMapper r,
      QuestionMapper q,
      AnswerRecordMapper a,
      KnowledgePointMapper p,
      QuestionKnowledgeMapper k) {
    courses = c;
    learning = l;
    exams = e;
    results = r;
    questions = q;
    answers = a;
    points = p;
    links = k;
  }

  public Object learning(long id) {
    courses.own(id);
    var rs = learning.selectList(new QueryWrapper<LearningRecord>().eq("course_id", id));
    return Map.of(
        "students",
        rs.stream().map(x -> x.userId).distinct().count(),
        "totalSeconds",
        rs.stream().mapToLong(x -> x.duration).sum(),
        "completedLessons",
        rs.stream().filter(x -> "COMPLETED".equals(x.status)).count());
  }

  public Object exams(long id) {
    courses.own(id);
    var ids =
        exams.selectList(new QueryWrapper<Exam>().eq("course_id", id)).stream()
            .map(x -> x.id)
            .toList();
    if (ids.isEmpty()) return Map.of("attempts", 0, "averageScore", 0);
    var rs =
        results.selectList(
            new QueryWrapper<ExamRecord>()
                .in("exam_id", ids)
                .in("status", List.of("SUBMITTED", "EXPIRED")));
    return Map.of(
        "attempts",
        rs.size(),
        "averagePercent",
        rs.stream()
            .mapToDouble(x -> x.maxScore == 0 ? 0 : x.score * 100.0 / x.maxScore)
            .average()
            .orElse(0));
  }

  public Object wrong(long id) {
    courses.own(id);
    return answers.courseWrongs(id);
  }

  public Object weak(long id) {
    courses.own(id);
    return answers.courseWeakness(id);
  }
}
