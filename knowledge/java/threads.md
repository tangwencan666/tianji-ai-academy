# 多线程

线程共享堆对象，但各自拥有调用栈。数据竞争发生在缺乏同步的并发冲突访问中。synchronized 提供互斥与可见性；volatile 提供可见性等内存语义，却不能让 count++ 自动原子。可以使用 AtomicInteger.incrementAndGet。

线程池限制并发数量，队列也必须有界，避免请求无限堆积。本项目 AI 使用有界线程池和超时，中断只表达取消请求，网络客户端也必须配置超时。不要把 ThreadLocal 用户上下文直接假定为异步线程可见，应显式传递并在 finally 清理。

练习：比较 volatile int 自增与 AtomicInteger 自增的多线程结果。
