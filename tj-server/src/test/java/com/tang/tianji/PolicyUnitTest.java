package com.tang.tianji;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tang.tianji.ai.agent.*;
import com.tang.tianji.ai.rag.CourseVectorStore;
import com.tang.tianji.ai.security.AiGuard;
import com.tang.tianji.common.*;
import com.tang.tianji.mapper.UserMapper;
import com.tang.tianji.service.*;
import com.tang.tianji.vo.AiModels.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PolicyUnitTest {
  @Test
  void objectiveGrading() {
    assertTrue(Grading.correct("MULTIPLE", "A,B", " B, A "));
    assertFalse(Grading.correct("MULTIPLE", "A,B", "A"));
    assertFalse(Grading.correct("MULTIPLE", "A,B", "A,B,C"));
    assertTrue(Grading.correct("BOOLEAN", "A", "a"));
    assertFalse(Grading.correct("SHORT", "hello", "hello"));
  }

  @Test
  void vectorMathRejectsDifferentDimensions() {
    assertEquals(1, CourseVectorStore.cosine(new float[] {1, 0}, new float[] {1, 0}), 1e-6);
    assertEquals(0, CourseVectorStore.cosine(new float[] {1, 0}, new float[] {0, 1}));
    assertEquals(-1, CourseVectorStore.cosine(new float[] {1}, new float[] {1, 0}));
  }

  PlanOutput plan(long lesson, int minutes) {
    return new PlanOutput(
        "goal",
        "2026-10-03",
        "2026-10-03",
        60,
        List.of(
            new PlanDay("2026-10-03", List.of("集合"), List.of(lesson), 5, List.of("Map"), minutes)));
  }

  @Test
  void planRejectsHallucinatedOrForeignLessons() {
    assertThrows(
        BusinessException.class,
        () -> PlanValidator.validate(plan(99, 50), LocalDate.of(2026, 10, 3), 1, 60, Set.of(1L)));
  }

  @Test
  void planRejectsExcessiveBudget() {
    assertThrows(
        BusinessException.class,
        () -> PlanValidator.validate(plan(1, 100), LocalDate.of(2026, 10, 3), 1, 60, Set.of(1L)));
  }

  @Test
  void validPlanAccepted() {
    assertDoesNotThrow(
        () -> PlanValidator.validate(plan(1, 50), LocalDate.of(2026, 10, 3), 1, 60, Set.of(1L)));
  }

  @Test
  void agentStopsAtEightSteps() {
    var s = new LearningAgentState();
    for (int i = 0; i < 8; i++) s.step("step");
    assertThrows(BusinessException.class, () -> s.step("extra"));
  }

  @Test
  void disabledAiReturnsFriendly503() {
    var g = new AiGuard(false, mock(ExamService.class), new AiActivity(), mock(UserMapper.class));
    var e = assertThrows(BusinessException.class, g::check);
    assertEquals(503, e.status);
    assertTrue(e.getMessage().contains("未启用"));
  }
}
