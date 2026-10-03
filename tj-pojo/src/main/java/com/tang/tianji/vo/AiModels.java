package com.tang.tianji.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public final class AiModels {
  public record TutorAnswer(
      @NotBlank @Size(max = 30000) String answer,
      @NotNull @Size(max = 5) List<@NotBlank String> suggestions) {}

  public record GeneratedQuestion(
      @NotBlank String question,
      @NotBlank String type,
      @NotNull @Size(max = 100) List<@NotBlank String> options,
      @NotBlank String answer,
      @NotBlank String explanation,
      @NotBlank String difficulty,
      @NotBlank String knowledgePoint) {}

  public record GeneratedQuestions(
      @NotEmpty @Size(max = 10) List<@Valid GeneratedQuestion> questions) {}

  public record AiGradingResult(
      @Min(0) int score,
      @Min(1) int maxScore,
      @NotBlank String feedback,
      @NotNull @Size(max = 100) List<@NotBlank String> strengths,
      @NotNull @Size(max = 100) List<@NotBlank String> problems,
      @NotNull @Size(max = 100) List<@NotBlank String> suggestions,
      @DecimalMin("0.0") @DecimalMax("1.0") double confidence) {}

  public record WeeklyLearningReport(
      @NotBlank String summary,
      @NotNull @Size(max = 100) List<@NotBlank String> achievements,
      @NotNull @Size(max = 100) List<@NotBlank String> weaknesses,
      @NotNull @Size(max = 100) List<@NotBlank String> recommendations,
      @NotNull @Size(max = 100) List<@NotBlank String> nextWeekGoals) {}

  public record PlanDay(
      @NotBlank String date,
      @NotNull @Size(max = 100) List<@NotBlank String> topics,
      @NotNull List<@NotNull Long> lessonIds,
      @Min(0) @Max(100) int practiceCount,
      @NotNull @Size(max = 100) List<@NotBlank String> reviewKnowledgePoints,
      @Min(1) @Max(240) int estimatedMinutes) {}

  public record PlanOutput(
      @NotBlank String goal,
      @NotBlank String startDate,
      @NotBlank String endDate,
      @Min(15) @Max(240) int dailyMinutes,
      @NotEmpty @Size(max = 30) List<@Valid PlanDay> days) {}

  public record CourseOutline(
      @NotBlank String title,
      @NotNull @Size(max = 100) List<@NotBlank String> objectives,
      @NotNull @Size(max = 100) List<@NotBlank String> sections,
      @NotNull @Size(max = 100) List<@NotBlank String> knowledgePoints,
      @NotNull @Size(max = 100) List<@NotBlank String> exercises) {}

  public record TeacherContent(
      @NotBlank String introduction,
      @NotNull @Size(max = 100) List<@NotBlank String> chapterSummaries,
      @NotNull @Size(max = 100) List<@NotBlank String> knowledgePoints,
      @NotNull @Size(max = 100) List<@NotBlank String> faq,
      @NotBlank String classAnalysis) {}
}
