package com.tang.tianji.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tang.tianji.ai.rag.CourseVectorStore;
import com.tang.tianji.ai.service.ModelGateway;
import com.tang.tianji.ai.tool.LearningTools;
import com.tang.tianji.dto.Requests.Plan;
import com.tang.tianji.service.*;
import com.tang.tianji.vo.AiModels.PlanOutput;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class LearningAgent {
  private final LearningService l;
  private final ExamService e;
  private final CourseService c;
  private final CourseVectorStore rag;
  private final ModelGateway model;
  private final StudyPlanService plans;
  private final ObjectMapper json;
  private final Clock clock;

  public LearningAgent(
      LearningService l,
      ExamService e,
      CourseService c,
      CourseVectorStore r,
      ModelGateway m,
      StudyPlanService p,
      ObjectMapper j,
      Clock clock) {
    this.l = l;
    this.e = e;
    this.c = c;
    rag = r;
    model = m;
    plans = p;
    json = j;
    this.clock = clock;
  }

  public Object run(Plan d) {
    return run(d, s -> {});
  }

  public Object run(Plan d, java.util.function.Consumer<String> status) {
    c.access(d.courseId());
    var t = new LearningTools(l, e, c, d.courseId());
    var s = new LearningAgentState();
    s.onStep = status;
    s.userGoal = d.goal();
    s.courseId = d.courseId();
    s.step("获取课程进度");
    s.progress = t.getCourseProgress();
    s.step("获取最近考试");
    s.examResults = t.getRecentExamResults();
    s.step("读取错题");
    s.wrongQuestions = t.getRecentWrongQuestions();
    s.step("统计知识掌握度");
    s.weakPoints = t.getWeakKnowledgePoints();
    s.step("检索课程资料与真实课时");
    var sources = rag.search(d.courseId(), d.goal());
    s.availableLessons = t.getRecommendedLessons();
    s.step("生成学习计划草案");
    var start = LocalDate.now(clock);
    PlanOutput output =
        model.structured(
            "依据下列学习数据生成学习计划。开始日期="
                + start
                + "，天数="
                + d.days()
                + "，每日上限="
                + d.dailyMinutes()
                + "分钟。dailyMinutes必须等于上限，日期连续，lessonIds只能选提供的课时。目标="
                + d.goal()
                + "\n状态="
                + encode(s)
                + "\n资料="
                + sources,
            PlanOutput.class);
    s.draftPlan = output;
    s.step("Java校验日期、时间预算与课时权限");
    PlanValidator.validate(
        output,
        start,
        d.days(),
        d.dailyMinutes(),
        new HashSet<>(c.lessons(d.courseId()).stream().map(x -> x.id).toList()));
    s.validatedPlan = output;
    s.step("保存已验证的个人计划");
    var p = plans.save(d.courseId(), output);
    return Map.of("plan", p, "days", output.days(), "steps", s.steps);
  }

  private String encode(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }
  }
}
