package com.tang.tianji.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningService {
  private final LearningRecordMapper records;
  private final LearningEventMapper events;
  private final UserMapper users;
  private final CourseService courses;
  private final Clock clock;

  public LearningService(
      LearningRecordMapper r, LearningEventMapper e, UserMapper u, CourseService c, Clock t) {
    records = r;
    events = e;
    users = u;
    courses = c;
    clock = t;
  }

  @Transactional
  public LearningRecord update(long lessonId, Progress p) {
    long uid = UserContext.getCurrentUserId();
    users.lock(uid);
    var l = courses.lesson(lessonId);
    var now = LocalDateTime.now(clock);
    var r =
        records.selectOne(
            new QueryWrapper<LearningRecord>().eq("user_id", uid).eq("lesson_id", lessonId));
    boolean fresh = r == null;
    if (fresh) {
      r = new LearningRecord();
      r.userId = uid;
      r.courseId = l.courseId;
      r.chapterId = l.chapterId;
      r.lessonId = lessonId;
      r.duration = 0;
      r.progress = 0;
    }
    // Never accept an arbitrary accumulated duration. Bound each heartbeat by server elapsed time.
    int elapsed =
        fresh ? 0 : (int) Math.max(0, Duration.between(r.lastLearnTime, now).getSeconds());
    int credited = Math.min(p.duration(), Math.min(elapsed, 300));
    r.duration += credited;
    r.progress = Math.max(r.progress, p.progress());
    r.lastPosition = p.lastPosition();
    r.lastLearnTime = now;
    r.status = r.progress == 100 ? "COMPLETED" : "LEARNING";
    if (r.progress == 100 && r.completedTime == null) r.completedTime = now;
    if (fresh) records.insert(r);
    else records.updateById(r);
    if (credited > 0) {
      var e = new LearningEvent();
      e.userId = uid;
      e.courseId = l.courseId;
      e.lessonId = lessonId;
      e.duration = credited;
      e.createTime = now;
      events.insert(e);
    }
    return r;
  }

  public List<LearningRecord> records() {
    return records.selectList(
        new QueryWrapper<LearningRecord>()
            .eq("user_id", UserContext.getCurrentUserId())
            .orderByDesc("last_learn_time"));
  }

  public Map<String, Object> progress(long id) {
    var ls = courses.lessons(id);
    var rs =
        records.selectList(
            new QueryWrapper<LearningRecord>()
                .eq("user_id", UserContext.getCurrentUserId())
                .eq("course_id", id));
    int total = ls.size();
    return Map.of(
        "courseId",
        id,
        "totalLessons",
        total,
        "completed",
        rs.stream().filter(r -> "COMPLETED".equals(r.status)).count(),
        "percent",
        total == 0 ? 0 : rs.stream().mapToInt(r -> r.progress).sum() / total,
        "records",
        rs);
  }

  public Map<String, Object> time() {
    return time(null);
  }

  public Map<String, Object> time(Long course) {
    if(course!=null) courses.access(course);
    long uid = UserContext.getCurrentUserId();
    var today = LocalDate.now(clock);
    var es =
        events.selectList(
            new QueryWrapper<LearningEvent>()
                .eq("user_id", uid)
                .eq(course!=null,"course_id",course)
                .ge("create_time", today.minusDays(6).atStartOfDay()));
    int week = es.stream().mapToInt(e -> e.duration).sum();
    int day =
        es.stream()
            .filter(e -> e.createTime.toLocalDate().equals(today))
            .mapToInt(e -> e.duration)
            .sum();
    return Map.of(
        "todaySeconds",
        day,
        "weekSeconds",
        week,
        "daily",
        es.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    e -> e.createTime.toLocalDate(),
                    java.util.stream.Collectors.summingInt(e -> e.duration))));
  }
}
