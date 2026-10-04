# 真实 AI 验证记录

日期：2026-10-04。仅使用独立数据库 tianji_ai_academy 和本地开发演示账号。未修改苍穹智膳、Sentinel 或旧数据库。本次启动禁用迁移与种子初始化。

## 真实模型与凭据

独立最小探测和后端 Spring AI probe 均成功；实际返回模型 deepseek-flash，provider 为 live、verified=true。后端重新启动后，仅显式执行一次64-token“你好”验证，启动和状态轮询不调用收费 API。密钥仅在环境变量中，来源为用户授权的本地文件，不复制进仓库。

页面公共 provider 接口返回“真实模型 · 已验证”和 deepseek-flash。Vue 已接入该状态；**本轮 Chrome 页面实际呈现仍待验收**，不以 HTTP 状态冒充浏览器截图。

## 逐项证据

|项目|结果|实际证据与边界|
|---|---|---|
|真实 Tutor|通过|HashMap 213 个增量、首段6.340秒；ArrayList/LinkedList 175个增量、首段6.787秒，均有完整答案|
|课程 RAG|通过（词法检索）|Java检索返回courseId=1的讲义；已有另一课程向量夹具未混入结果。按课程/模型先SQL过滤。远程神经Embedding未验证|
|模型 Learning Tool Calling|通过|真实模型一次问题调用5个工具，后端记录tool_calls=5；基于实际错题、考试等数据回答。另1项真实MySQL只读测试验证全部7个工具及教师聚合通过|
|Memory|通过|同一会话先复习Java集合，再问“给我出5道题”，实际返回List/Set/Map、ArrayList/LinkedList、HashMap、HashSet、迭代删除题；无资料的部分明确为通用知识|
|Learning Agent|通过 API/SSE|8个真实业务步骤；前6步在0.286秒内，生成后10.084秒进行Java校验、10.091秒保存；7天，每天120分钟，课时均来自授权课程|
|TutorAnswer|通过|真实JSON经字段校验，正文与建议分离|
|GeneratedQuestion|通过|5道真实题目，全部AI_GENERATED，未自动审批或加入试卷|
|StudyPlan|通过|7日结构与日期、预算、课时归属通过Java校验并保存|
|WeeklyLearningReport|通过|基于真实MySQL统计生成，返回facts与报告|
|AiGradingResult|通过|一条明确标注本地演示的简答练习，真实模型给出建议，状态NEEDS_REVIEW，未自动更改正式成绩|
|错题解释|通过|针对demo_student真实answerRecord调用并返回解释|
|SSE增量/完成|通过|真实模型多个delta后发送answer/suggestions/done/complete；不将完整答案人为切片|
|SSE取消|通过|首个真实delta后关闭连接，MySQL ai_call_log记录CANCELLED、9318ms|
|SSE超时/余额错误|隔离测试通过|向本地测试ChatModel注入TimeoutException/402；返回error，无done、无原始错误泄漏，并释放并发锁。不故意耗尽真实额度或制造供应商故障|
|考试防作弊|真实API通过|演示考试进行中Tutor返回HTTP409，模型调用前拦截，随后提交演示考试|
|用户A/B隔离|自动测试通过|独立测试用户的Memory、错题与计划等权限覆盖；本轮不声称真实Chrome多账号流程完成|
|Chrome完整学习→AI流程|未完成|Chrome未连接到浏览器工具；保留用户要求，未用内置浏览器替代|

## 本轮发现与修复

1. 原实例AI_ENABLED=false，AI未启用；显式live配置且不回退Mock。
2. 原RAG需要另一个Embedding密钥；现提供明确标注的本地词法向量模式，不伪装成神经Embedding。
3. 原SSE缓冲全部模型JSON才显示一次；改为按真实输出增量解码正文，最终严格校验。
4. Memory追问检索原来丢失话题，加入最近用户主题并修正词法特征，复验保留Java集合上下文。
5. 7天计划首次4096-token输出被截断。计划/题目集合改为8192上限，检查finish_reason=length，严格解析失败不保存、不记录原始模型正文。真实计划复验成功。
6. 一次组合验收最后因没有简答题出现StopIteration；已完成的计划、5题、错题、周报结果保留。另建明确标注的本地演示简答题，仅重跑grading阶段成功，没有重复支付前四项。

## 测试与证据位置

- 普通mvn test：44项，43通过，1项真实MySQL可选测试跳过；无失败，无真实收费调用。
- 独立RealEnvironmentTest：1项真实MySQL只读测试通过，AI关闭、Flyway关闭；修复旧测试写死16道题的假设，改为验证当前课程题目集合（本次含新增演示草稿共22道）。
- npm test：4项全部通过；npm run build通过。
- 本机忽略目录work/real-ai-{tutor,tools,memory,structured,grading,cancel,security}.json保存逐项安全证据，不包含凭据。structured旧组合记录整体FAIL（最后缺少简答题），应结合其已完成results与独立grading PASS理解，不能隐去失败历史。
- scripts/verify_real_ai.py分阶段显式执行，不由mvn/npm自动触发；无自动重试。setup只导入课程文档/本地向量，其他模型阶段会收费。
- 词法RAG不擅长同义词/跨语言；后续若接远程神经Embedding，应另行授权供应商并单独验收。

## 安全

工作区和Git历史扫描：187个候选文件、186个历史blob；授权真实API Key、当前数据库密码及常见Key/JWT特征未命中。无意外大文件、target、node_modules、dist或.env进入候选文件。配置使用环境变量，运行日志与证据在忽略目录。Git仅本地提交，不push。
