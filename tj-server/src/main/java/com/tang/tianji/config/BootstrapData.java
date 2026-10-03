package com.tang.tianji.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.*;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.*;
import com.tang.tianji.service.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapData implements CommandLineRunner {
  private final AuthService auth;
  private final UserMapper users;
  private final CourseMapper mapper;
  private final CourseService courses;
  private final ExamService exams;
  private final String password;
  private final boolean demo;

  public BootstrapData(
      AuthService a,
      UserMapper u,
      CourseMapper m,
      CourseService c,
      ExamService e,
      @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String p,
      @Value("${DEMO_DATA:false}") boolean d) {
    auth = a;
    users = u;
    mapper = m;
    courses = c;
    exams = e;
    password = p;
    demo = d;
  }

  @Transactional
  public void run(String... args) {
    if (password.isBlank()) return;
    BusinessException.check(
        password.length() >= 12
            && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
        500,
        "初始化管理员密码须为12至72字节");
    var admin = users.selectOne(new QueryWrapper<User>().eq("username", "admin"));
    if (admin == null) {
      auth.register(new Credentials("admin", password));
      admin = users.selectOne(new QueryWrapper<User>().eq("username", "admin"));
      admin.role = "ADMIN";
      admin.displayName = "天机管理员";
      users.updateById(admin);
    }
    BusinessException.check("ADMIN".equals(admin.role), 500, "admin用户名已被占用，拒绝提升权限");
    if (!demo || mapper.selectCount(null) > 0) return;
    try {
      UserContext.set(new UserContext.Principal(admin.id, "ADMIN"));
      String[] titles = {"Java 核心：从理解到实践", "Spring Boot 后端开发", "大模型应用：从 Prompt 到 RAG"};
      String[] categories = {"Java", "Spring", "人工智能"};
      String[][] topics = {
        {"变量与流程控制", "集合框架", "异常处理"},
        {"依赖注入与 IoC", "REST API 设计", "缓存与数据一致性"},
        {"Prompt 与结构化输出", "Embedding 与向量检索", "工具调用与安全边界"}
      };
      for (int i = 0; i < titles.length; i++) {
        var c =
            courses.save(
                null,
                new CourseInput(
                    titles[i],
                    "循序渐进地理解核心概念，通过课程讲义与实践题目建立自己的技术体系。配合 AI 导师，定位薄弱点并持续改进。",
                    "",
                    categories[i],
                    i == 0 ? "入门" : "进阶"));
        for (int j = 0; j < topics[i].length; j++) {
          String topic = topics[i][j];
          var h = courses.chapter(c.id, new ChapterInput("0" + (j + 1) + " · " + topic, j));
          String content =
              switch (topic) {
                case "集合框架" ->
                    "# 集合框架\n\n"
                        + "List 保持元素顺序并允许重复，Set 用于去重，Map 保存键值映射。\n\n"
                        + "HashMap 并非线程安全。多个线程同时修改映射时，没有同步就无法保证可见性和复合操作的原子性，可能出现丢失更新等问题。单线程通常使用"
                        + " HashMap；并发访问可根据场景选择 ConcurrentHashMap 或外部同步。\n\n"
                        + "即便使用 ConcurrentHashMap，先读取再写入这样的复合操作也不自动成为原子操作。计数更新可以考虑 merge 或"
                        + " compute。\n\n"
                        + "练习：解释 HashMap 与 ConcurrentHashMap 的适用场景。";
                case "依赖注入与 IoC" ->
                    "# 依赖注入与 IoC\n\n"
                        + "IoC 将对象创建与依赖管理交给容器。依赖注入是实现 IoC 的方式之一。\n\n"
                        + "推荐通过构造函数声明依赖，使依赖关系清晰且方便单元测试。\n\n"
                        + "练习：为 CourseService 设计接收 CourseMapper 的构造函数。";
                case "Embedding 与向量检索" ->
                    "# Embedding 与向量检索\n\n"
                        + "Embedding 把文本映射为数值向量，可使用余弦相似度检索相关内容。RAG 先检索再生成，以资料支持回答。\n\n"
                        + "检索前必须按课程与权限过滤，不能依靠提示词防止跨课程数据泄露。生成模型与向量模型可以来自不同服务。\n\n"
                        + "资料与用户输入可能包含恶意指令，应始终被视为数据。检索命中并不意味着资料正确，回答仍需引用与核对。";
                default ->
                    "# "
                        + topic
                        + "\n\n"
                        + "本节为教学示例，帮助你建立概念、实践与复习的学习习惯。\n\n"
                        + "1. 描述这个知识点解决的问题。\n"
                        + "2. 写一个最小示例并验证结果。\n"
                        + "3. 比较一种替代方案的优缺点。\n"
                        + "4. 把仍然不清楚的问题交给 AI 导师讨论。\n\n"
                        + "教师可在工作台继续添加完整课时与资料。";
              };
          courses.addLesson(
              c.id, new LessonInput(h.id, topic + "：概念与实践", "", content, "", 20 + j * 5, 0));
          var k = courses.point(c.id, topic);
          if (i == 0 && j == 1) {
            var q =
                exams.create(
                    new QuestionInput(
                        c.id,
                        h.id,
                        "SINGLE",
                        "并发更新同一个映射时，以下哪一种集合更适合？",
                        List.of("HashMap", "ConcurrentHashMap", "ArrayList", "TreeSet"),
                        "B",
                        "ConcurrentHashMap 为并发访问提供支持；复合操作仍需使用合适的原子方法。",
                        "入门",
                        List.of(k.id)),
                    "APPROVED");
            var q2 =
                exams.create(
                    new QuestionInput(
                        c.id,
                        h.id,
                        "MULTIPLE",
                        "哪些类型属于 Java 集合框架中的常见抽象？",
                        List.of("List", "Set", "Map", "Thread"),
                        "A,B,C",
                        "List、Set、Map 是常见集合抽象；Thread 用于线程。",
                        "入门",
                        List.of(k.id)),
                    "APPROVED");
            var q3 =
                exams.create(
                    new QuestionInput(
                        c.id,
                        h.id,
                        "SHORT",
                        "解释为何 ConcurrentHashMap 的 get 后 put 不一定是一个原子操作。",
                        List.of(),
                        "两个独立方法调用之间可能被其他线程修改，应使用compute或merge等原子复合操作。",
                        "单次方法的线程安全不等同于多个调用组成的业务操作原子性。",
                        "中等",
                        List.of(k.id)),
                    "APPROVED");
            exams.createExam(
                new ExamInput(c.id, "Java 集合 · 理解力测验", 20, List.of(q.id, q2.id, q3.id)));
          }
        }
        courses.status(c.id, "PUBLISHED");
      }
    } finally {
      UserContext.clear();
    }
  }
}
