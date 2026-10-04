package com.tang.tianji;

import static org.junit.jupiter.api.Assertions.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.ai.tool.LearningTools;
import com.tang.tianji.common.UserContext;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import com.tang.tianji.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Opt-in read-only assertions against the dedicated development database. No model mocks. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {"app.dev-seed=false", "DEMO_DATA=false"})
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_REAL_DB_TESTS", matches = "true")
class RealEnvironmentTest {
  @Autowired UserMapper users;
  @Autowired CourseMapper courses;
  @Autowired CourseService courseService;
  @Autowired ExamService exams;
  @Autowired LearningService learning;
  @Autowired TeacherStatisticsService stats;

  @Test
  void sevenToolsAndTeacherAggregatesReadMySql() {
    var student = users.selectOne(new QueryWrapper<User>().eq("username", "demo_student"));
    var teacher = users.selectOne(new QueryWrapper<User>().eq("username", "demo_teacher"));
    var course =
        courses.selectOne(
            new QueryWrapper<Course>().eq("teacher_id", teacher.id).eq("category", "Java"));
    assertNotNull(student);
    assertNotNull(course);
    try {
      UserContext.set(new UserContext.Principal(student.id, student.role));
      var tools = new LearningTools(learning, exams, courseService, course.id);
      assertFalse(((List<?>) tools.getCurrentLearningProgress()).isEmpty());
      assertEquals(8, ((Map<?, ?>) tools.getCourseProgress()).get("totalLessons"));
      assertFalse(((List<?>) tools.getRecentWrongQuestions()).isEmpty());
      assertFalse(((List<?>) tools.getWeakKnowledgePoints()).isEmpty());
      assertFalse(((List<?>) tools.getRecentExamResults()).isEmpty());
      assertNotNull(((Map<?, ?>) tools.getLearningTimeStatistics()).get("todaySeconds"));
      assertEquals(8, ((List<?>) tools.getRecommendedLessons()).size());
      UserContext.set(new UserContext.Principal(teacher.id, teacher.role));
      // Real demo data grows after explicit AI draft/short-answer acceptance.
      // Assert course membership against the current teacher-visible bank, not a seed count.
      var expectedIds = new HashSet<>(exams.bank(course.id, true).stream().map(q -> q.id).toList());
      var actualIds = new HashSet<Long>();
      for (Object item : (List<?>) stats.wrong(course.id)) {
        var row = (Map<?, ?>) item;
        actualIds.add(((Number) row.get("question_id")).longValue());
        assertTrue(((Number) row.get("wrong_count")).longValue() >= 0);
      }
      assertEquals(expectedIds, actualIds);
      assertEquals(8, ((List<?>) stats.weak(course.id)).size());
    } finally {
      UserContext.clear();
    }
  }
}
