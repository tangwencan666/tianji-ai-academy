package com.tang.tianji.ai.agent;

import com.tang.tianji.common.BusinessException;
import com.tang.tianji.vo.AiModels.*;
import java.time.*;
import java.util.*;

public final class PlanValidator {
  public static void validate(
      PlanOutput p, LocalDate start, int days, int minutes, Set<Long> allowed) {
    BusinessException.check(
        p != null && p.days() != null && p.days().size() == days, 422, "计划天数不符合要求");
    BusinessException.check(
        start.toString().equals(p.startDate())
            && start.plusDays(days - 1).toString().equals(p.endDate())
            && p.dailyMinutes() == minutes,
        422,
        "计划日期或时间预算不符");
    for (int i = 0; i < days; i++) {
      var d = p.days().get(i);
      BusinessException.check(
          d != null && start.plusDays(i).toString().equals(d.date()), 422, "计划日期重复或不连续");
      BusinessException.check(
          d.estimatedMinutes() > 0
              && d.estimatedMinutes() <= minutes
              && d.practiceCount() >= 0
              && d.practiceCount() <= 100,
          422,
          "每日任务超出范围");
      BusinessException.check(
          d.lessonIds() != null && allowed.containsAll(d.lessonIds()), 422, "计划引用了不存在或其他课程的课时");
      BusinessException.check(
          d.topics() != null && !d.topics().isEmpty() && d.reviewKnowledgePoints() != null,
          422,
          "计划内容不完整");
    }
  }
}
