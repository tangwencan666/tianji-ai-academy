package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tang.tianji.common.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import com.tang.tianji.vo.AiModels.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanService {
  private final StudyPlanMapper plans;
  private final StudyPlanDayMapper days;
  private final ObjectMapper json;
  private final Clock clock;

  public StudyPlanService(StudyPlanMapper p, StudyPlanDayMapper d, ObjectMapper j, Clock c) {
    plans = p;
    days = d;
    json = j;
    clock = c;
  }

  @Transactional
  public StudyPlan save(long course, PlanOutput output) {
    var p = new StudyPlan();
    p.userId = UserContext.getCurrentUserId();
    p.courseId = course;
    p.goal = output.goal();
    p.startDate = LocalDate.parse(output.startDate());
    p.endDate = LocalDate.parse(output.endDate());
    p.dailyMinutes = output.dailyMinutes();
    p.status = "ACTIVE";
    p.createTime = LocalDateTime.now(clock);
    plans.insert(p);
    for (var x : output.days()) {
      var d = new StudyPlanDay();
      d.planId = p.id;
      d.studyDate = LocalDate.parse(x.date());
      d.topics = encode(x.topics());
      d.lessonIds = encode(x.lessonIds());
      d.practiceCount = x.practiceCount();
      d.reviewKnowledgePoints = encode(x.reviewKnowledgePoints());
      d.estimatedMinutes = x.estimatedMinutes();
      d.completed = false;
      days.insert(d);
    }
    return p;
  }

  private String encode(Object v) {
    try {
      return json.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalArgumentException();
    }
  }

  public List<StudyPlan> list() {
    return plans.selectList(
        new QueryWrapper<StudyPlan>()
            .eq("user_id", UserContext.getCurrentUserId())
            .orderByDesc("id"));
  }

  private StudyPlan own(long id) {
    var p = plans.selectById(id);
    BusinessException.check(p != null && p.userId == UserContext.getCurrentUserId(), 404, "计划不存在");
    return p;
  }

  public Object detail(long id) {
    return Map.of(
        "plan",
        own(id),
        "days",
        days.selectList(
            new QueryWrapper<StudyPlanDay>().eq("plan_id", id).orderByAsc("study_date")));
  }

  public void complete(long id) {
    var d = days.selectById(id);
    BusinessException.check(d != null, 404, "任务不存在");
    own(d.planId);
    d.completed = true;
    days.updateById(d);
  }
}
