# 真实环境审计

审计日期：2026-10-03。范围：原有三模块 Java 后端、13 个 Vue 页面、SQL、配置、文档；排除 target/node_modules/dist。

|分类|发现|处理/验证边界|
|---|---|---|
|真实实现|JWT、BCrypt、课程、学习心跳、客观题判分、考试、错题均调用 Mapper 持久化|原先只在 H2 验证，正在迁移真实 MySQL|
|测试 Mock|SecurityIntegrationTest、ToolBudgetTest 的 ChatModel/EmbeddingModel Mockito|只在 src/test；不等于真实模型验收|
|开发替代|local profile 使用 H2 文件与内存缓存；test 使用 H2 内存|保留；dev/production 使用 MySQL 与 Redis|
|演示数据|BootstrapData 三门固定课程、九课时、三道题；部分讲义只有练习提纲|是明确标记的 seed，不是查询接口硬编码响应；新增 dev 专用完整素材与 seed|
|固定建议|TutorService 的 follow-up 提示、首页通用学习建议|只是 UI 引导，不能称个性化 AI 分析|
|RAG|真实 EmbeddingModel 调用、MySQL 持久化向量、按 courseId/model 过滤后余弦 top-k|无密钥时未验证真实 Embedding 质量，禁止宣称真实模型通过|
|Tools|七个学习工具通过当前 UserContext 调用业务服务|不接收 userId；推荐课程此前只是可用课时清单，需改进|
|Agent|有界 Java 工作流、工具读取、结构化计划、Java 校验、保存|不是任意自主行动；轨迹为业务状态，无隐藏思维链|
|缓存|课程分类 Redis 五分钟 TTL，错误回源数据库|增加独立前缀；必须核实命中与写后失效，PONG 不算缓存验收|
|代码质量|掌握度统计与教师统计存在逐知识点/题目查询；ExamService 职责较多|需消除明显 N+1；保持业务边界，不强行引入 DDD|
|安全|考试状态后端禁止 AI；成绩/记忆按用户隔离，题库隐藏答案|继续执行权限与注入测试；AI 活动锁仍为单实例限制|

检索 TODO/FIXME/Fake/Stub/hardcoded/UnsupportedOperationException/@Disabled 未发现生产占位实现。Mock 主要来自测试。H2 出现在测试/local 配置及说明。return null 无生产业务占位；Result.ok(null) 表示操作成功无返回体。没有固定考试成绩或绕过数据库的学习数据接口。

本机已通过环境变量连接 MySQL 8.0.44，创建独立空库 tianji_ai_academy（迁移前业务表 0）。没有读取、修改其他项目数据库。Redis PING=PONG。环境未设置 AI_API_KEY/EMBEDDING_API_KEY，真实 AI 验证留待用户配置，其他任务继续。

## 本轮处置结果

- MySQL真实空库迁移V1/V2通过：23表（含Flyway）、82索引、22外键、9唯一约束。3角色通过真实登录；3课程/12课时/24题/3考试。
- Redis3.0.504回填TTL300、重复读取TTL298、课程修改失效和新分类回填通过；不需要更换服务。
- 掌握度N+1改批量关联，教师统计改聚合SQL；H2别名大小写差异在回归中发现并修复。
- 七个LearningTools及教师聚合查询在真实MySQL读取通过；Tools模型选择仍待真实API。
- TutorAnswer及所有结构化输出补Bean Validation；正文JSON校验后再呈现，明确不伪称逐token输出。
- 错题历史答案、教师信息、首页我的课程与最近考试、学习页内嵌Tutor已补齐；仍保持13页面。
- Compose默认变量语法原为Spring占位符形式，改为Compose的${VAR:-default}；五服务配置验证通过。Docker WSL引擎无法启动，容器运行未通过。
- 无真实API Key，未写假Embedding或返回固定模型答案。详细边界见FINAL_REPORT。
