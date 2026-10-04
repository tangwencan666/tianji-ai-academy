# 天机智学真实 AI 审计

审计日期：2026-10-04。审计对象仅为 Tianji AI Academy；未改动苍穹智膳、Sentinel 或旧数据库。

## 改造前的实际状态

| 能力 | 源码实际实现 | 本机运行状态 / 缺口 |
|---|---|---|
| AI Tutor / ModelGateway | Spring AI ChatModel、ChatClient，真实网络调用路径 | AI_ENABLED=false，AiGuard 拒绝请求；不是自动回退 Mock |
| RAG | MySQL knowledge_chunk，课程与 embedding_model 先过滤，余弦检索 | OpenAI Embedding 需要独立凭据；仅有 DeepSeek 密钥无法使用原配置 |
| Learning Tools | 7 个 @Tool → Service → Mapper | 查询 JWT 当前用户真实数据；部分工具原来没有限定课程 |
| Memory | MySQL conversation + message | userId + conversationId 绑定，保留最近 20 条消息；已有权限检查 |
| Structured Output | BeanOutputConverter → Java record → Bean Validation | 普通测试使用 Mockito，不能作为真实 API 验收证明 |
| Learning Agent | Java 编排实际进度、错题、考试、薄弱点和课时，再由 LLM 生成计划 | 非自主工具规划模型；有 Java 日期、预算、课时校验；之前未真实验证 |
| SSE | 调用真实模型 stream 接口 | 原来缓冲整个 JSON 后只发送一次 delta，浏览器看不到实时增量 |
| 失败处理 / 防作弊 | 任务并发限制、请求超时、ExamRecord IN_PROGRESS 拦截 | 原来错误提示较笼统，需补充错误分类与真实调用证据 |

没有发现生产 AI 路径返回固定假成绩、固定模型回答的实现。界面欢迎语、状态文案是固定 UI 文字；不能当作模型能力证据。普通测试的 Mockito 模型和向量明确只用于测试。

## 本轮实现

- AI_MODE=live + AI_ENABLED=true；AI_API_KEY / AI_BASE_URL / AI_MODEL 通过进程环境读取。启动不调用模型。
- 官方 DeepSeek OpenAI-compatible 地址 https://api.deepseek.com，当前模型 deepseek-flash。兼容 Spring AI 1.0.9 请求序列化，明确发送 thinking.type=disabled，不展示内部推理。
- Provider 的 verified 仅由当前后端实例成功的真实调用设置，重启恢复“待验证”；状态查询不收费。
- RAG 默认使用 local-lexical-hash-v1：本地文本特征哈希向量、归一化余弦检索，不调用外部 Embedding、不伪装语义模型。它的同义词/跨语言检索能力有限。可通过 EMBEDDING_MODE=remote 与独立 EMBEDDING_* 配置换为远程 Embedding，切换后需重新导入向量；本轮远程神经 Embedding **未验证**。
- Learning Tools 无 userId 参数，课程由服务端请求权限检查后固定；错题、成绩、薄弱点、时长限定当前课程和当前用户。
- Tutor 实际模型 token 解码为 answer 字段增量，每个可见片段立即发送；完整 JSON 仍需最终校验，失败不写入 Memory，前端清除不完整回答。
- 认证、余额、限流、超时、5xx、网络失败只返回安全提示；不输出密钥、Header、完整上游错误正文。

官方依据：[首次调用](https://api-docs.deepseek.com/)、[Thinking Mode](https://api-docs.deepseek.com/guides/thinking_mode/)。最终实际验证状态见 [REAL_AI_VERIFICATION.md](REAL_AI_VERIFICATION.md)，不能由代码存在推断验收完成。
