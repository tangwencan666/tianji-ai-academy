# 天机智学 · Tianji AI Academy

面向 Java 技术成长的 AI 原生在线学习平台。以「学习 → 练习 → 错题 → 知识掌握度 → 个性化计划」形成闭环，适合后端学习、毕业作品和面试演示。

这是一套可运行的第一版多模块单体应用，包含前后端源码、数据库迁移、示例课程、自动化测试与部署配置。AI 使用真实 Spring AI 接口；未配置密钥时返回明确的不可用提示，不生成假回答。默认数据库是 MySQL，Redis 用于课程分类缓存；另提供可选 H2 本地开发模式。

## 快速开始：Docker Compose

需要 Docker Desktop / Docker Engine 与 Compose。默认仅开放本机 `8088` 端口。

```powershell
# 在项目根目录运行。生成随机数据库密码、JWT密钥和管理员密码。
./scripts/init-env.ps1
docker compose up -d --build
docker compose logs -f server
```

出现 `Started TianjiApplication` 后打开 [http://localhost:8088](http://localhost:8088)。初始管理员用户名为 `admin`，密码在你本机 `.env` 的 `BOOTSTRAP_ADMIN_PASSWORD` 中。首次启动会创建示例课程；管理员密码不会在重启时被重置。普通用户在前端注册，默认是 `STUDENT`。管理员在「平台管理」分配教师身份。

Linux/macOS 可复制 `.env.example` 为 `.env`，为每个 `CHANGE_ME` 项填入独立随机值后启动。不要把 `.env` 提交到 Git。

## 无 Docker 的本地运行

需要 **JDK 17、Maven 3.9+、Node.js 22+**。先运行 `java -version`，确保 Maven 使用的是 JDK 17。H2 模式只供本地开发与演示，不等同于 MySQL 集成验证。

```powershell
./scripts/init-env.ps1
mvn clean package -Plocal
./scripts/start-local.ps1
```

新终端中：

```powershell
cd tj-web
npm ci
npm run dev
```

浏览器打开 Vite 输出的地址（通常是 [http://localhost:5173](http://localhost:5173)）。默认前端代理到 `http://localhost:8080`，可用 `API_PROXY_TARGET` 环境变量修改。本地数据库保存到项目 `work/`，不清空就会保留账号和学习记录。

如果使用本机 MySQL/Redis：创建空库 `tianji`，设置 `DB_URL`、`DB_USER`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`JWT_SECRET`，正常 `mvn clean package`，再执行 `java -jar tj-server/target/tj-server-1.0.0.jar`，**不启用 local profile**。表结构由 Flyway 自动管理，不要再手工重复执行 `sql/001-schema.sql`。

## 开启 AI

编辑 `.env`：

```dotenv
AI_ENABLED=true
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat
AI_API_KEY=你的聊天模型密钥
EMBEDDING_BASE_URL=https://api.openai.com
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_API_KEY=你的向量模型密钥
```

聊天与 Embedding 分开配置，可使用兼容 OpenAI API 的服务。基础 URL 不包含 `/chat/completions` 或 `/embeddings`，兼容服务若要求路径前缀则遵循该服务说明。模型必须支持工具调用；向量服务必须提供 Embedding。默认聊天服务地址并不意味着它同时支持向量接口。

重新创建服务：`docker compose up -d server`；本地模式重启后端即可。教师进入「课程知识库」，导入 `knowledge/java-collections.md` 等资料。先选择对应课程；同一课程同一来源重新导入会原子替换旧分块。更换向量模型后须重新导入资料。

## 已实现的学习闭环

| 领域 | 功能 |
| --- | --- |
| 身份与权限 | 注册、BCrypt、JWT、Interceptor/UserContext、学生/教师/管理员、课程所有权 |
| 课程 | 搜索/分类/收藏/详情、创建/修改/发布/下架、章节、文章与视频课时、附件链接 |
| 学习 | 学习心跳、课时进度、累计与近7天时长、继续学习 |
| 练习与考试 | 单选/多选/判断/简答、组卷、限时考试、重复提交幂等、Java客观判分 |
| 复习 | 自动错题本、错误次数、重练掌握状态、Java计算知识点正确率与最近10次准确率 |
| 教师 | 题目审核、正式组卷、简答题复核、课程汇总与AI备课草稿 |
| AI Tutor | Spring AI ChatClient、真实 SSE、按课程检索、来源面板、工具状态、终止和超时 |
| RAG | 文本/Markdown、900字符窗口/700字符步长、独立Embedding、MySQL持久向量、课程过滤 |
| 个性化 AI | 出题、讲解、辅助批改、错因分析、薄弱点解释、周报、学习计划 |
| Learning Agent | 8步有界流程、真实课时白名单、日期和时间预算校验、计划持久化与每日打卡 |
| AI 安全 | 用户隔离记忆窗口、后端防作弊、工具次数上限、每用户并发限制、失败降级、调用日志 |

AI 自动出题放在教师工作台，状态为 `AI_GENERATED`，经教师审核后才能进入正式考试。简答题的 AI 评分始终只是建议，统一留给教师确认。第一版课程免费，不含真实支付或模拟购买流程。

## 项目结构

```text
tianji-ai-academy/
├── pom.xml                  Maven聚合工程
├── tj-common/               Result、异常、当前身份
├── tj-pojo/                 Entity、请求DTO、AI输出模型
├── tj-server/               Controller → Service → Mapper
│   └── src/main/java/com/tang/tianji/ai/
│       ├── config/          执行器、HTTP超时
│       ├── service/         ChatClient、Tutor、结构化任务、日志
│       ├── tool/            学生和教师工具
│       ├── memory/          用户/会话隔离的最近10轮消息
│       ├── prompt/          导师系统规则
│       ├── rag/             课程向量库和来源
│       ├── agent/           8步工作流、状态、计划校验
│       └── security/        考试拦截与AI准入
├── tj-web/                  Vue3 / Pinia / Element Plus
├── knowledge/               示例知识资料
├── sql/                     数据库结构阅读副本
├── docs/                    架构、API、测试、安全与面试说明
├── scripts/                 环境初始化和本地启动
└── docker-compose.yml       MySQL / Redis / 后端 / Nginx前端
```

## 验证

```sh
mvn test
cd tj-web
npm ci
npm run build
npm audit
```

后端自动化测试使用 H2 MySQL 模式、模拟模型接口；不会消耗真实模型额度。包括跨用户隔离、教师归属、答案隐藏、防作弊、幂等判分、RAG课程过滤、窗口记忆、SSE与工具预算等。实际执行记录与环境限制见 [测试报告](docs/verification.md)。

## 阅读与演示

- [系统架构与数据流](docs/architecture.md)
- [接口清单与请求示例](docs/api.md)
- [安全边界与当前限制](docs/security.md)
- [需求覆盖与扩展路线](docs/coverage.md)
- [十分钟演示与面试讲解](docs/demo-and-interview.md)

开发接口参考：[Spring AI 1.0 工具调用](https://docs.spring.io/spring-ai/reference/1.0/api/tools.html)、[ChatClient](https://docs.spring.io/spring-ai/reference/1.0/api/chatclient.html)、[Embedding](https://docs.spring.io/spring-ai/reference/1.0/api/embeddings/openai-embeddings.html)。本工程固定 Spring Boot 3.4.13 / Spring AI 1.0.9，未声称这是最新版；升级时应重新验证兼容性。
