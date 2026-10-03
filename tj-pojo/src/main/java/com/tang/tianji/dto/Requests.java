package com.tang.tianji.dto;

import jakarta.validation.constraints.*;
import java.util.*;

public final class Requests {
  public record Credentials(
      @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,32}") String username,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record CourseInput(
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 5000) String description,
      String cover,
      @NotBlank @Size(max = 80) String category,
      @NotBlank @Size(max = 30) String difficulty) {}

  public record ChapterInput(@NotBlank String title, @Min(0) int sortOrder) {}

  public record LessonInput(
      @NotNull Long chapterId,
      @NotBlank String title,
      String videoUrl,
      @Size(max = 50000) String content,
      String attachment,
      @Min(1) @Max(300) int estimatedMinutes,
      @Min(0) int sortOrder) {}

  public record Progress(
      @Min(0) @Max(300) int duration, @Min(0) @Max(100) int progress, @Min(0) int lastPosition) {}

  public record QuestionInput(
      @NotNull Long courseId,
      Long chapterId,
      @Pattern(regexp = "SINGLE|MULTIPLE|BOOLEAN|SHORT") String type,
      @NotBlank String stem,
      List<String> options,
      @NotBlank String answer,
      @NotBlank String explanation,
      @NotBlank String difficulty,
      @NotEmpty List<Long> knowledgePointIds) {}

  public record ExamInput(
      @NotNull Long courseId,
      @NotBlank String title,
      @Min(1) @Max(180) int durationMinutes,
      @NotEmpty @Size(max = 50) List<Long> questionIds) {}

  public record Answers(@NotNull @Size(max = 50) Map<Long, String> answers) {}

  public record Practice(@NotBlank @Size(max = 8000) String answer) {}

  public record Chat(
      @NotNull Long courseId,
      @Pattern(regexp = "[a-zA-Z0-9-]{1,64}") @NotNull String conversationId,
      @NotBlank @Size(max = 4000) String message) {}

  public record Generate(
      @NotNull Long courseId,
      @NotBlank @Size(max = 2000) String topic,
      @Min(1) @Max(10) int count) {}

  public record Plan(
      @NotNull Long courseId,
      @NotBlank @Size(max = 1000) String goal,
      @Min(1) @Max(30) int days,
      @Min(15) @Max(240) int dailyMinutes) {}

  public record Material(
      @NotNull Long courseId,
      Long chapterId,
      Long lessonId,
      @NotBlank @Size(max = 150) String title,
      @NotBlank @Size(max = 250) String source,
      @NotBlank @Size(max = 60000) String content) {}

  public record Review(@Min(0) int score) {}
}
