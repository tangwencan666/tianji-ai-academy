package com.tang.tianji.ai.agent;

import java.util.*;

public class LearningAgentState {
  @com.fasterxml.jackson.annotation.JsonIgnore
  public java.util.function.Consumer<String> onStep = s -> {};
  public final List<String> steps = new ArrayList<>();
  public String userGoal;
  public long courseId;
  public Object progress,
      weakPoints,
      wrongQuestions,
      examResults,
      availableLessons,
      draftPlan,
      validatedPlan;

  public void step(String status) {
    if (steps.size() >= 8) throw new com.tang.tianji.common.BusinessException(503, "Agent已达到8步上限");
    steps.add(status);
    onStep.accept(status);
  }
}
