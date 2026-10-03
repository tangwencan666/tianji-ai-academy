# 项目学习路线

目标是能解释一条真实请求，而不是背诵技术名词。

1. **启动与数据**：用 dev 连接独立MySQL，查看Flyway历史；读V1的课程、学习、考试外键关系，说明为什么不能重复手工建表。用demo_student实际登录。
2. **身份链路**：从Login.vue到AuthController/AuthService/JwtService/Interceptor/UserContext；追踪BCrypt哈希和当前数据库角色。试用学生访问教师接口，解释403与资源404。
3. **课程与学习**：从Learn.vue的30秒心跳进入LearningService.update，解释服务器时间上限、单调进度、用户行锁和learning_event。不要把前端传入时长当可信累计值。
4. **考试**：跟踪start → paper → submit → AnswerRecord → WrongQuestion。对三对一错的4题考试手算30/40，再对照页面。重复交卷应返回原结果。
5. **Redis**：读CategoryService、CacheConfig和CourseService的CacheEvict。复现miss→回填→hit→编辑→失效；只操作本项目前缀，禁止FLUSHALL。
6. **RAG**：先读knowledge/java/collections.md，再读CourseVectorStore。手工用两个二维向量计算余弦相似度；解释为什么必须先按courseId过滤。真实模型密钥由本人配置。
7. **Tools与Memory**：读LearningTools和ModelGateway.prepare，查看Mock工具测试与真实MySQL工具测试的区别。Memory测试证明用户隔离和20条窗口，不证明真实模型语义理解。
8. **结构化与Agent**：读AiModels/PlanValidator/LearningAgent。给计划一个其他课程的lessonId，观察拒绝；讲清模型输出格式合法不代表业务合法。
9. **权限与防作弊**：运行SecurityIntegrationTest中的注入、跨用户、教师所有权、草稿题和并发admission用例，解释每一层信任边界。
10. **演示与面试**：按VERIFICATION的浏览器流程操作，对照FINAL_REPORT诚实说明真实AI和Docker未验收项。

练习时不要改生产数据库，不在源码放密码。不必先引入微服务、消息队列或DDD；优先把状态转换、SQL、事务和失败路径讲明白。
