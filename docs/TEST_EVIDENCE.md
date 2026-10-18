# 测试证据与版本索引

日期：2026-10-18。历史成绩与本次验收分开。源码快照 Commit 只说明报告所在版本；没有执行时源码哈希的历史记录不升级为当前源码验收。

| 日期 / 版本 | 范围与限制 | 原始证据 |
| --- | --- | --- |
| 2026-10-04 发布复验 | 后端45项中44通过1跳过，前端4；普通模型为Mock | [docs/REAL_AI_VERIFICATION.md](../docs/REAL_AI_VERIFICATION.md) |
| 2026-10-04 真实AI专项 | 有限API/SSE；远程神经Embedding和完整Chrome流程未完成 | [docs/REAL_AI_VERIFICATION.md](../docs/REAL_AI_VERIFICATION.md) |
| 2026-10-03 早期快照 | 35项及NOT_RUN_NO_KEYS属于早期记录，不覆盖后续记录 | [docs/test-summary.json](../docs/test-summary.json) |

详细报告文件 SHA-256 与承载快照登记在 [acceptance-20261018.json](acceptance-20261018.json)。测试数字不跨批次相加，Mock/Stub 不计入真实模型样本。
