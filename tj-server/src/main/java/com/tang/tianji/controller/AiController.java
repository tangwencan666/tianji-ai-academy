package com.tang.tianji.controller;

import com.tang.tianji.ai.agent.LearningAgent;
import com.tang.tianji.ai.memory.ConversationMemory;
import com.tang.tianji.ai.rag.CourseVectorStore;
import com.tang.tianji.ai.security.AiGuard;
import com.tang.tianji.ai.service.*;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.service.*;
import jakarta.validation.Valid;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class AiController {
  private final AiJobRunner jobs;
  private final TutorService tutor;
  private final AiApplicationService app;
  private final LearningAgent agent;
  private final AiGuard guard;
  private final CourseVectorStore rag;
  private final ConversationMemory memory;
  private final StudyPlanService plans;

  public AiController(
      TutorService t,
      AiApplicationService a,
      LearningAgent l,
      AiGuard g,
      CourseVectorStore r,
      ConversationMemory m,
      StudyPlanService p,
      AiJobRunner jobs) {
    this.jobs = jobs;
    tutor = t;
    app = a;
    agent = l;
    guard = g;
    rag = r;
    memory = m;
    plans = p;
  }

  private Object run(Supplier<Object> action) {
    return Result.ok(jobs.run(action));
  }

  @PostMapping(value = "/ai/tutor/chat", produces = "text/event-stream")
  public SseEmitter chat(@Valid @RequestBody Chat d) {
    return tutor.chat(d);
  }

  @GetMapping("/ai/conversations")
  public Object conversations() {
    return Result.ok(memory.list());
  }

  @PostMapping("/ai/question/generate")
  public Object generate(@Valid @RequestBody Generate d) {
    return run(() -> app.generate(d));
  }

  @PostMapping("/ai/answers/{id}/explain")
  public Object explain(@PathVariable long id) {
    return run(() -> app.explain(id));
  }

  @PostMapping("/ai/answers/{id}/grade")
  public Object grade(@PathVariable long id) {
    return run(() -> app.grade(id));
  }

  @PostMapping("/ai/wrong-analysis")
  public Object analyze() {
    return run(app::analyze);
  }

  @GetMapping("/ai/report/weekly")
  public Object report() {
    return run(app::report);
  }

  @PostMapping("/ai/plans")
  public Object plan(@Valid @RequestBody Plan d) {
    return run(() -> agent.run(d));
  }

  @PostMapping("/ai/teacher/outline")
  public Object outline(@Valid @RequestBody Generate d) {
    return run(() -> app.teacher(d, true));
  }

  @PostMapping("/ai/teacher/assistant")
  public Object teacher(@Valid @RequestBody Generate d) {
    return run(() -> app.teacher(d, false));
  }

  @PostMapping("/knowledge/import")
  public Object ingest(@Valid @RequestBody Material d) {
    return run(() -> rag.ingest(d));
  }

  @GetMapping("/knowledge/sources")
  public Object sources(@RequestParam long courseId) {
    return Result.ok(rag.sources(courseId));
  }

  @GetMapping("/study-plans")
  public Object plans() {
    return Result.ok(plans.list());
  }

  @GetMapping("/study-plans/{id}")
  public Object plan(@PathVariable long id) {
    return Result.ok(plans.detail(id));
  }

  @PostMapping("/study-plans/days/{id}/complete")
  public Object complete(@PathVariable long id) {
    plans.complete(id);
    return Result.ok(null);
  }
}
