# 真实环境验收记录

时间：2026-10-03，Asia/Shanghai。验收对象为原天机智学项目的加强修改。

## 已通过

|项目|环境与证据|结果|
|---|---|---|
|快速后端测试|mvn test；PolicyUnitTest 7、SecurityIntegrationTest 25、ToolBudgetTest 3|35通过，0失败；RealEnvironmentTest 1项默认按环境条件跳过|
|真实工具集成|RUN_REAL_DB_TESTS=true，dev profile，无ChatModel/Embedding Mockito|RealEnvironmentTest 1通过，七LearningTools及教师聚合读取MySQL|
|MySQL|本机8.0.44，独立空库tianji_ai_academy|Flyway V1/V2 success=1，重启校验通过|
|结构完整性|information_schema仅限本项目库|23表（含Flyway）、82索引、23主键、9唯一约束、22外键|
|种子|真实AuthService与业务Service|3角色、3课程、12章节/课时/知识点、24题、3考试；BCrypt前缀$2a$|
|实际账号|HTTP登录并校验返回角色|demo_student/demo_teacher/demo_admin均通过|
|真实HTTP|scripts/verify_http.py，新后端包+MySQL+本地Redis|39请求检查通过，详见mysql-http-verification.json|
|Redis|本机3.0.504，当前Lettuce|PONG；首次回填TTL300；重复读取TTL298；编辑后键不存在；新分类回填成功|
|前端|npm run build|Vite6.4.3成功，1692模块|
|浏览器|真实Vue → Spring Boot → MySQL|登录、课时完成100%、练习错误反馈、考试30/40、错题详情展示|
|Compose|docker compose config --quiet|5服务配置语法通过，环境值未打印|
|敏感信息|对待提交源码检查已设置的DB_PASSWORD/AI_API_KEY/EMBEDDING_API_KEY实际值，另检查密钥模式|首次提交通过；最终提交前再执行，真实密码未写入源码|

## 浏览器过程

1. demo_student登录，首页显示持久化记录。
2. Java课程详情显示demo_teacher、8章节/课时、课程进度。
3. 进入集合框架，目录/讲义/内嵌导师在桌面三栏展示；点击完成，实际变为100%。
4. 在并发与缓存课程，单计数题选A，反馈正确答案D并写入错题。
5. 四题考试选择A/A/C/A，三对一错，交卷显示30/40且SUBMITTED。
6. 错题本显示我的答案A、正确D、知识点多线程、累计错2次。
7. 点击AI错因分析及生成计划，明确提示AI尚未启用；没有返回假分析或假计划。
8. 首页显示2个已完成课时、真实累计时长与最近考试。截图在screenshots/mysql-learning.jpg及mysql-dashboard.jpg；桌面1700宽验收后恢复浏览器默认尺寸。

## Mock边界

H2测试中的ChatModel和EmbeddingModel为Mockito，用于重复验证工具循环、8次上限、结构化解析、来源事件、Memory窗口/隔离、课程向量过滤、Agent计划持久化、AI生成题待审核、周报数字隔离。业务Service/Mapper使用真实实现。真实MySQL工具测试没有模型Mock，也没有调用模型；不能将其描述为真实LLM Tool Calling选择能力通过。

## 未通过或未执行

- 未设置聊天和Embedding密钥，未执行真实生成、真实向量入库/召回、两轮语义Memory、真实AI批改与计划效果验收。verify_ai.py已通过Python语法检查，但没有运行供应商调用。
- Docker引擎检查报Docker Desktop is unable to start / DockerDesktop/Wsl/ExecError / 0xc00000fd。引擎不可用，未继续build/up，容器运行与镜像构建未验收。没有重启WSL、停止或删除其他项目容器。
- 本轮没有对线上负载、渗透测试、浏览器矩阵、供应商结构兼容矩阵进行验证。

## 发现并修复

测试暴露教师聚合查询别名在H2/MySQL大小写差异，统一snake_case后两种数据库均通过。真实跨用户考试读取返回404（设计隐藏资源），HTTP脚本原预期403，已与业务规则对齐。Compose原默认变量语法不合法，改成${VAR:-default}并通过config。打包曾被旧H2进程持有jar影响，停止本任务旧预览后构建成功，后续运行副本避免锁定构建产物。

## 重跑

见README本地启动与测试命令。快速测试默认跳过显式真实服务测试，避免MySQL未启动导致全部失败。verify_http会创建独立注册测试用户、提交演示学生考试、修改后恢复演示课程分类，仅用于明确启用的本项目演示环境。它只删除本项目分类缓存键，不清空Redis。
