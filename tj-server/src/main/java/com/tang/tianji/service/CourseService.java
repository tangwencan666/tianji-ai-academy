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
public class CourseService {
  private final CourseMapper courses;
  private final ChapterMapper chapters;
  private final LessonMapper lessons;
  private final KnowledgePointMapper points;
  private final FavoriteMapper favorites;
  private final Clock clock;
  private final UserMapper users;

  public CourseService(
      CourseMapper c,
      ChapterMapper h,
      LessonMapper l,
      KnowledgePointMapper k,
      FavoriteMapper f,
      Clock t,
      UserMapper users) {
    this.users = users;
    courses = c;
    chapters = h;
    lessons = l;
    points = k;
    favorites = f;
    clock = t;
  }

  public Course access(long id) {
    var c = courses.selectById(id);
    BusinessException.check(c != null, 404, "课程不存在");
    var p = UserContext.current();
    BusinessException.check(
        "PUBLISHED".equals(c.status) || p.role().equals("ADMIN") || c.teacherId == p.id(),
        403,
        "课程未发布");
    return c;
  }

  public Course own(long id) {
    UserContext.require("TEACHER", "ADMIN");
    var c = access(id);
    BusinessException.check(
        UserContext.current().role().equals("ADMIN")
            || c.teacherId == UserContext.getCurrentUserId(),
        403,
        "只能管理自己的课程");
    return c;
  }

  public List<Course> list(String search, String category, boolean mine) {
    var q = new QueryWrapper<Course>();
    if (mine) {
      UserContext.require("TEACHER", "ADMIN");
      if (!UserContext.current().role().equals("ADMIN"))
        q.eq("teacher_id", UserContext.getCurrentUserId());
    } else q.eq("status", "PUBLISHED");
    if (search != null && !search.isBlank()) q.like("title", search);
    if (category != null && !category.isBlank()) q.eq("category", category);
    return courses.selectList(q.orderByDesc("id").last("LIMIT 100"));
  }

  public Map<String, Object> detail(long id) {
    var c = access(id);
    return Map.of(
        "course",
        c,
        "teacherName",
        users.selectById(c.teacherId).displayName,
        "chapters",
        chapters.selectList(
            new QueryWrapper<Chapter>().eq("course_id", id).orderByAsc("sort_order", "id")),
        "lessons",
        lessons(id),
        "knowledgePoints",
        points.selectList(new QueryWrapper<KnowledgePoint>().eq("course_id", id)));
  }

  public List<Lesson> lessons(long id) {
    access(id);
    return lessons.selectList(
        new QueryWrapper<Lesson>()
            .eq("course_id", id)
            .orderByAsc("chapter_id", "sort_order", "id"));
  }

  public Lesson lesson(long id) {
    var l = lessons.selectById(id);
    BusinessException.check(l != null, 404, "课时不存在");
    access(l.courseId);
    return l;
  }

  @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
  public Course save(Long id, CourseInput d) {
    UserContext.require("TEACHER", "ADMIN");
    var c = id == null ? new Course() : own(id);
    if (id == null) {
      c.teacherId = UserContext.getCurrentUserId();
      c.status = "DRAFT";
      c.totalLessons = 0;
      c.createTime = LocalDateTime.now(clock);
    }
    c.title = d.title();
    c.description = d.description();
    c.cover = d.cover();
    c.category = d.category();
    c.difficulty = d.difficulty();
    c.updateTime = LocalDateTime.now(clock);
    if (id == null) courses.insert(c);
    else courses.updateById(c);
    return c;
  }

  @org.springframework.cache.annotation.CacheEvict(value = "categories", allEntries = true)
  public void status(long id, String status) {
    BusinessException.check(Set.of("PUBLISHED", "DRAFT", "OFFLINE").contains(status), 400, "无效状态");
    var c = own(id);
    c.status = status;
    c.updateTime = LocalDateTime.now(clock);
    courses.updateById(c);
  }

  public Chapter chapter(long id, ChapterInput d) {
    own(id);
    var c = new Chapter();
    c.courseId = id;
    c.title = d.title();
    c.sortOrder = d.sortOrder();
    chapters.insert(c);
    return c;
  }

  @Transactional
  public Lesson addLesson(long id, LessonInput d) {
    var c = own(id);
    var h = chapters.selectById(d.chapterId());
    BusinessException.check(h != null && h.courseId == id, 400, "章节不属于此课程");
    safeUrl(d.videoUrl());
    safeUrl(d.attachment());
    var l = new Lesson();
    l.courseId = id;
    l.chapterId = d.chapterId();
    l.title = d.title();
    l.content = d.content();
    l.videoUrl = d.videoUrl();
    l.attachment = d.attachment();
    l.estimatedMinutes = d.estimatedMinutes();
    l.sortOrder = d.sortOrder();
    lessons.insert(l);
    c.totalLessons =
        Math.toIntExact(lessons.selectCount(new QueryWrapper<Lesson>().eq("course_id", id)));
    courses.updateById(c);
    return l;
  }

  private void safeUrl(String u) {
    BusinessException.check(
        u == null || u.isBlank() || u.startsWith("https://") || u.startsWith("/media/"),
        400,
        "资源仅支持https或/media/路径");
  }

  public KnowledgePoint point(long id, String name) {
    own(id);
    BusinessException.check(name != null && !name.isBlank() && name.length() < 100, 400, "知识点名称无效");
    var k = new KnowledgePoint();
    k.courseId = id;
    k.name = name;
    points.insert(k);
    return k;
  }

  public void validateChapter(long course, Long chapter) {
    if (chapter != null) {
      var h = chapters.selectById(chapter);
      BusinessException.check(h != null && h.courseId == course, 400, "章节不匹配");
    }
  }

  public List<Course> favorites() {
    var fs =
        favorites.selectList(
            new QueryWrapper<Favorite>().eq("user_id", UserContext.getCurrentUserId()));
    if (fs.isEmpty()) return List.of();
    return courses.selectList(
        new QueryWrapper<Course>()
            .in("id", fs.stream().map(f -> f.courseId).toList())
            .eq("status", "PUBLISHED"));
  }

  public void favorite(long id, boolean add) {
    access(id);
    var q =
        new QueryWrapper<Favorite>()
            .eq("user_id", UserContext.getCurrentUserId())
            .eq("course_id", id);
    if (add) {
      if (favorites.selectCount(q) == 0) {
        var f = new Favorite();
        f.userId = UserContext.getCurrentUserId();
        f.courseId = id;
        favorites.insert(f);
      }
    } else favorites.delete(q);
  }
}
