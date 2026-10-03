# API 使用说明

普通响应为 `{"code":0,"message":"ok","data":...}`。错误使用实际HTTP状态和同样信封，`data`为null。需要认证的请求携带 `Authorization: Bearer <token>`。SSE接口直接返回事件流。

## 完整接口清单

| 方法 | 路径 | Controller |
| --- | --- | --- |
| POST | `/api/ai/tutor/chat` | AiController |
| GET | `/api/ai/conversations` | AiController |
| POST | `/api/ai/question/generate` | AiController |
| POST | `/api/ai/answers/{id}/explain` | AiController |
| POST | `/api/ai/answers/{id}/grade` | AiController |
| POST | `/api/ai/wrong-analysis` | AiController |
| GET | `/api/ai/report/weekly` | AiController |
| POST | `/api/ai/plans` | AiController |
| POST | `/api/ai/teacher/outline` | AiController |
| POST | `/api/ai/teacher/assistant` | AiController |
| POST | `/api/knowledge/import` | AiController |
| GET | `/api/knowledge/sources` | AiController |
| GET | `/api/study-plans` | AiController |
| GET | `/api/study-plans/{id}` | AiController |
| POST | `/api/study-plans/days/{id}/complete` | AiController |
| POST | `/api/auth/register` | AuthController |
| POST | `/api/auth/login` | AuthController |
| GET | `/api/auth/me` | AuthController |
| GET | `/api/admin/users` | AuthController |
| PATCH | `/api/admin/users/{id}/role` | AuthController |
| GET | `/api/courses` | CourseController |
| GET | `/api/courses/categories` | CourseController |
| GET | `/api/courses/{id}` | CourseController |
| GET | `/api/lessons/{id}` | CourseController |
| POST | `/api/courses` | CourseController |
| PUT | `/api/courses/{id}` | CourseController |
| PATCH | `/api/courses/{id}/status` | CourseController |
| POST | `/api/courses/{id}/chapters` | CourseController |
| POST | `/api/courses/{id}/lessons` | CourseController |
| POST | `/api/courses/{id}/knowledge-points` | CourseController |
| GET | `/api/favorites` | CourseController |
| PUT | `/api/favorites/{id}` | CourseController |
| DELETE | `/api/favorites/{id}` | CourseController |
| GET | `/api/learning/records` | LearningController |
| PUT | `/api/learning/lessons/{id}` | LearningController |
| GET | `/api/learning/courses/{id}` | LearningController |
| GET | `/api/learning/dashboard` | LearningController |
| GET | `/api/questions` | LearningController |
| POST | `/api/questions` | LearningController |
| POST | `/api/questions/{id}/approve` | LearningController |
| POST | `/api/questions/{id}/practice` | LearningController |
| GET | `/api/wrong-questions` | LearningController |
| GET | `/api/exams` | LearningController |
| POST | `/api/exams` | LearningController |
| POST | `/api/exams/{id}/start` | LearningController |
| GET | `/api/exam-records` | LearningController |
| GET | `/api/exam-records/{id}` | LearningController |
| GET | `/api/exam-records/{id}/answers` | LearningController |
| POST | `/api/exam-records/{id}/submit` | LearningController |
| GET | `/api/teacher/reviews` | LearningController |
| POST | `/api/teacher/reviews/{id}` | LearningController |

路径参数均为后端生成的ID。`/courses` 查询参数：`search`、`category`、`mine`；mine=true仅教师/管理员可用。`/questions` 使用 `courseId` 和可选 `teacher=true`；教师模式返回完整题目且必须拥有课程。`/exams`、`/knowledge/sources`、`/teacher/reviews` 使用 `courseId`。修改课程状态使用 `?status=PUBLISHED|DRAFT|OFFLINE`；修改角色使用 `?role=STUDENT|TEACHER|ADMIN`。新增知识点使用 `?name=...`。

## 请求示例

登录或注册：

```json
{"username":"your_student","password":"your_own_password"}
```

成功后返回 `{token,user}`。注册不能指定角色。所有学习记录由JWT确定用户，以下请求都不需要userId。

创建课程：

```json
{"title":"Java 集合实战","description":"理解与实践集合框架","cover":"","category":"Java","difficulty":"入门"}
```

新增章节：`{"title":"集合框架","sortOrder":0}`。

新增课时：

```json
{"chapterId":1,"title":"HashMap","videoUrl":"","content":"课程正文","attachment":"","estimatedMinutes":30,"sortOrder":0}
```

`chapterId`必须属于当前课程。视频/附件只接受https地址或`/media/`路径，应用不提供视频转码。

学习心跳（每30秒）：

```json
{"duration":30,"progress":50,"lastPosition":120}
```

duration是增量秒数，每次最多300且受服务端真实间隔限制。首次建立记录不计入时长；progress单调不减，100代表完成。按可见页面心跳计时，不证明实际注意力。

创建题目：

```json
{"courseId":1,"chapterId":1,"type":"MULTIPLE","stem":"哪些属于集合抽象？","options":["List","Set","Thread"],"answer":"A,B","explanation":"Thread是线程类","difficulty":"入门","knowledgePointIds":[1]}
```

客观题字母选项从A开始；多选答案用英文逗号分隔。简答题type为SHORT，options可为空数组。判断题按A/B选项表示正确/错误。创建考试请求：

```json
{"courseId":1,"title":"集合测验","durationMinutes":30,"questionIds":[1,2]}
```

每题10分。开始接口返回 `record`、`exam`、`deadline`和无答案的`questions`。提交给 `/exam-records/{recordId}/submit`：

```json
{"answers":{"1":"B","2":"A,C"}}
```

过期提交不再接受有效得分。简答题分数先为0并标记复核；教师确认后更新总分。教师复核请求：`{"score":8}`。重复确认返回409，避免重复计入错题。

## AI 请求

导师聊天：

```json
{"courseId":1,"conversationId":"a-client-generated-uuid","message":"为什么HashMap不是线程安全的？"}
```

客户端保持同一conversationId实现连续对话。切换课程必须新建会话。

```text
event: status
data: 正在检索课程资料…

event: sources
data: [{"id":1,"title":"集合讲义","source":"java.md",...}]

event: delta
data: 文本片段

event: done
data: {"conversationId":"..."}
```

服务端事件data可能是JSON值或文本，前端解析器兼容两者。`sources`来自后端，不由模型任意指定。发生错误时发送`error`并结束；准入阶段失败返回普通HTTP 409/429/503。

AI出题/教师助手/大纲：

```json
{"courseId":1,"topic":"Java集合框架","count":5}
```

AI出题是教师能力，资料必须先入库。讲解与辅助评分通过自己的 `answerRecordId` 发起，不接受任意用户题目上下文。

生成计划：

```json
{"courseId":1,"goal":"30天准备Java面试","days":30,"dailyMinutes":120}
```

返回已保存的plan、days和8个高层执行状态。日期按Asia/Shanghai，开始日期为当天。

入库资料：

```json
{"courseId":1,"chapterId":1,"lessonId":1,"title":"集合讲义","source":"java-collections.md","content":"Markdown或纯文本正文"}
```

chapterId/lessonId可为null，若填写必须匹配本课程。资料不由后端从任意URL抓取；source只是引用标签，不是访问地址。未提供文件解析/外部URL抓取接口。

## 常用错误

| 状态 | 含义 |
| --- | --- |
| 400 | 参数非法、章节归属不匹配、选项答案格式错误 |
| 401 | 未登录、令牌失效 |
| 403 | 角色或课程所有权不允许 |
| 404 | 资源不存在或私人资源不属于自己 |
| 409 | 考试进行中、重复资源、重复复核 |
| 422 | AI结构化结果未通过业务校验或无相关资料 |
| 429 | AI并发已满或Nginx登录限流 |
| 503/504 | AI未启用、模型失败或超时 |

## 加强版补充

`GET /api/wrong-questions/details`：本人历史错题详情，包含wrong/question/myAnswer/answerRecordId/knowledgePoints；考试进行中409。

`GET /api/courses/{id}` 新增teacherName。Tutor SSE 新增answer事件（TutorAnswer）；正文在结构化验证后一次通过delta发送，原有status/sources/suggestions/done保持。
