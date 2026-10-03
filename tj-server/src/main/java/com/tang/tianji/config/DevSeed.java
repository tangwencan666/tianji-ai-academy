package com.tang.tianji.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import com.tang.tianji.service.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit opt-in development fixtures persisted through real business services. */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.dev-seed", havingValue = "true")
public class DevSeed implements CommandLineRunner {
  private final AuthService auth;
  private final UserMapper users;
  private final CourseMapper courseMapper;
  private final CourseService courses;
  private final ExamService exams;
  private final LearningService learning;
  private final ObjectMapper json;
  private final String password;

  public DevSeed(
      AuthService a,
      UserMapper u,
      CourseMapper m,
      CourseService c,
      ExamService e,
      LearningService l,
      ObjectMapper j,
      @Value("${DEMO_PASSWORD:}") String p) {
    auth = a;
    users = u;
    courseMapper = m;
    courses = c;
    exams = e;
    learning = l;
    json = j;
    password = p;
  }

  private User account(String name, String role) {
    var u = users.selectOne(new QueryWrapper<User>().eq("username", name));
    if (u != null) {
      BusinessException.check(role.equals(u.role), 500, "演示账号角色冲突，拒绝提升已有账号权限");
      return u;
    }
    auth.register(new Credentials(name, password));
    u = users.selectOne(new QueryWrapper<User>().eq("username", name));
    if ("ADMIN".equals(role)) {
      u.role = role;
      users.updateById(u);
    } else if ("TEACHER".equals(role)) {
      auth.role(u.id, role);
    }
    return u;
  }

  @Transactional
  public void run(String... args) throws Exception {
    BusinessException.check(
        password.length() >= 12
            && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
        500,
        "DEV_SEED需要设置12至72字节DEMO_PASSWORD");
    try {
      var admin = account("demo_admin", "ADMIN");
      UserContext.set(new UserContext.Principal(admin.id, "ADMIN"));
      var teacher = account("demo_teacher", "TEACHER");
      var student = account("demo_student", "STUDENT");
      if (courseMapper.selectCount(new QueryWrapper<Course>().eq("teacher_id", teacher.id)) > 0)
        return;
      UserContext.set(new UserContext.Principal(teacher.id, "TEACHER"));
      var data = json.readTree(new ClassPathResource("seed/courses.json").getInputStream());
      Long firstLesson = null, firstQuestion = null;
      for (var item : data) {
        var c =
            courses.save(
                null,
                new CourseInput(
                    item.path("title").asText(),
                    "开发演示课程：包含可阅读讲义、知识点、真实练习和考试。数据由明确启用的开发seed创建。",
                    "",
                    item.path("category").asText(),
                    "入门"));
        var ids = new ArrayList<Long>();
        int order = 0;
        for (var entry : item.path("lessons")) {
          var chapter =
              courses.chapter(c.id, new ChapterInput(entry.path("title").asText(), order++));
          var lesson =
              courses.addLesson(
                  c.id,
                  new LessonInput(
                      chapter.id,
                      entry.path("title").asText(),
                      "",
                      entry.path("content").asText(),
                      "",
                      30,
                      0));
          var point = courses.point(c.id, entry.path("title").asText());
          if (firstLesson == null) firstLesson = lesson.id;
          for (var q : entry.path("questions")) {
            var options = new ArrayList<String>();
            q.path("options").forEach(v -> options.add(v.asText()));
            var saved =
                exams.create(
                    new QuestionInput(
                        c.id,
                        chapter.id,
                        "SINGLE",
                        q.path("stem").asText(),
                        options,
                        q.path("answer").asText(),
                        q.path("explanation").asText(),
                        "入门",
                        List.of(point.id)),
                    "APPROVED");
            ids.add(saved.id);
            if (firstQuestion == null) firstQuestion = saved.id;
          }
        }
        courses.status(c.id, "PUBLISHED");
        exams.createExam(new ExamInput(c.id, "基础理解测验", 30, ids));
      }
      UserContext.set(new UserContext.Principal(student.id, "STUDENT"));
      learning.update(firstLesson, new Progress(0, 35, 0));
      exams.practice(firstQuestion, new Practice("D"));
    } finally {
      UserContext.clear();
    }
  }
}
