package com.tang.tianji.controller;

import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.service.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LearningController {
  private final LearningService l;
  private final ExamService e;

  public LearningController(LearningService l, ExamService e) {
    this.l = l;
    this.e = e;
  }

  @GetMapping("/learning/records")
  public Object records() {
    return Result.ok(l.records());
  }

  @PutMapping("/learning/lessons/{id}")
  public Object update(@PathVariable long id, @Valid @RequestBody Progress p) {
    return Result.ok(l.update(id, p));
  }

  @GetMapping("/learning/courses/{id}")
  public Object progress(@PathVariable long id) {
    return Result.ok(l.progress(id));
  }

  @GetMapping("/learning/dashboard")
  public Object dashboard() {
    return Result.ok(
        Map.of(
            "time",
            l.time(),
            "records",
            l.records(),
            "exams",
            e.history(),
            "mastery",
            e.mastery(),
            "wrongQuestions",
            e.wrongs()));
  }

  @GetMapping("/questions")
  public Object bank(
      @RequestParam long courseId, @RequestParam(defaultValue = "false") boolean teacher) {
    return Result.ok(e.bank(courseId, teacher));
  }

  @PostMapping("/questions")
  public Object question(@Valid @RequestBody QuestionInput d) {
    return Result.ok(e.create(d, "APPROVED"));
  }

  @PostMapping("/questions/{id}/approve")
  public Object approve(@PathVariable long id) {
    e.approve(id);
    return Result.ok(null);
  }

  @PostMapping("/questions/{id}/practice")
  public Object practice(@PathVariable long id, @Valid @RequestBody Practice d) {
    return Result.ok(e.practice(id, d));
  }

  @GetMapping("/wrong-questions")
  public Object wrongs() {
    return Result.ok(e.wrongs());
  }

  @GetMapping("/wrong-questions/details")
  public Object wrongDetails() {
    return Result.ok(e.wrongDetails());
  }

  @GetMapping("/exams")
  public Object exams(@RequestParam long courseId) {
    return Result.ok(e.exams(courseId));
  }

  @PostMapping("/exams")
  public Object exam(@Valid @RequestBody ExamInput d) {
    return Result.ok(e.createExam(d));
  }

  @PostMapping("/exams/{id}/start")
  public Object start(@PathVariable long id) {
    return Result.ok(e.start(id));
  }

  @GetMapping("/exam-records")
  public Object history() {
    return Result.ok(e.history());
  }

  @GetMapping("/exam-records/{id}")
  public Object paper(@PathVariable long id) {
    return Result.ok(e.paper(id));
  }

  @GetMapping("/exam-records/{id}/answers")
  public Object answers(@PathVariable long id) {
    return Result.ok(e.ownAnswers(id));
  }

  @PostMapping("/exam-records/{id}/submit")
  public Object submit(@PathVariable long id, @Valid @RequestBody Answers d) {
    return Result.ok(e.submit(id, d));
  }

  @GetMapping("/teacher/reviews")
  public Object pending(@RequestParam long courseId) {
    return Result.ok(e.pending(courseId));
  }

  @PostMapping("/teacher/reviews/{id}")
  public Object review(@PathVariable long id, @Valid @RequestBody Review d) {
    e.review(id, d.score());
    return Result.ok(null);
  }
}
