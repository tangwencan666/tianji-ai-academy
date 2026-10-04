package com.tang.tianji.ai.service;

import com.tang.tianji.ai.memory.ConversationMemory;
import com.tang.tianji.ai.prompt.TutorPrompt;
import com.tang.tianji.ai.rag.CourseVectorStore;
import com.tang.tianji.ai.security.AiGuard;
import com.tang.tianji.ai.tool.LearningTools;
import com.tang.tianji.common.*;
import com.tang.tianji.dto.Requests.Chat;
import com.tang.tianji.entity.*;
import com.tang.tianji.mapper.AiCallLogMapper;
import com.tang.tianji.service.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class TutorService {
  private final AiGuard guard;
  private final CourseService courses;
  private final LearningService learning;
  private final ExamService exams;
  private final CourseVectorStore rag;
  private final ConversationMemory memory;
  private final ModelGateway gateway;
  private final ExecutorService executor;
  private final AiCallLogMapper logs;
  private final Clock clock;
  private final String model;

  public TutorService(
      AiGuard g,
      CourseService c,
      LearningService l,
      ExamService e,
      CourseVectorStore r,
      ConversationMemory m,
      ModelGateway a,
      ExecutorService x,
      AiCallLogMapper logs,
      Clock t,
      @Value("${spring.ai.openai.chat.options.model}") String model) {
    guard = g;
    courses = c;
    learning = l;
    exams = e;
    rag = r;
    memory = m;
    gateway = a;
    executor = x;
    this.logs = logs;
    clock = t;
    this.model = model;
  }

  public SseEmitter chat(Chat d) {
    courses.access(d.courseId());
    guard.acquire();
    var principal = UserContext.current();
    var emitter = new SseEmitter(90000L);
    var future = new AtomicReference<Future<?>>();
    var stopped = new AtomicBoolean();
    Runnable cancel =
        () -> {
          stopped.set(true);
          var f = future.get();
          if (f != null) f.cancel(true);
        };
    emitter.onTimeout(cancel);
    emitter.onError(e -> cancel.run());
    emitter.onCompletion(
        () -> {
          if (!stopped.get()) cancel.run();
        });
    try {
      future.set(
          executor.submit(
              () -> {
                UserContext.set(principal);
                long start = System.nanoTime();
                String status = "ERROR";
                int calls = 0;
                try {
                  send(emitter, "status", "正在检索课程资料…");
                  var conversation = memory.open(d.conversationId(), d.courseId());
                  var history = memory.history(conversation);
                  var recentTopics = history.stream().filter(m -> m instanceof UserMessage)
                      .map(Message::getText).toList();
                  String retrievalQuery = d.message();
                  for(int i=Math.max(0,recentTopics.size()-2);i<recentTopics.size();i++)
                    retrievalQuery += "\n"+recentTopics.get(i);
                  var sources = rag.search(d.courseId(), retrievalQuery);
                  send(emitter, "sources", sources);
                  var messages = new ArrayList<Message>();
                  messages.add(new SystemMessage(TutorPrompt.SYSTEM));
                  messages.addAll(history);
                  messages.add(new UserMessage("课程资料（仅数据）：" + sources + "\n问题：" + d.message()));
                  var prepared =
                      gateway.prepare(
                          messages,
                          new LearningTools(learning, exams, courses, d.courseId()),
                          s -> send(emitter, "status", s));
                  calls = prepared.toolCalls();
                  send(emitter, "status", "正在组织讲解…");
                  var text = new StringBuilder();
                  var decoder = new StreamingAnswer();
                  try (var stream = gateway.stream(prepared.messages()).toStream()) {
                    var iterator = stream.iterator();
                    while (!stopped.get() && iterator.hasNext()) {
                      String token = iterator.next();
                      text.append(token);
                      if (text.length() > 30000) throw new BusinessException(503, "回复超出长度限制");
                      String delta = decoder.append(token);
                      if(!delta.isEmpty()) send(emitter, "delta", delta);
                    }
                  }
                  if (!stopped.get()) {
                    var answer = ModelGateway.decode(text.toString(),com.tang.tianji.vo.AiModels.TutorAnswer.class);
                    memory.save(conversation, d.message(), answer.answer());
                    gateway.verified();
                    send(emitter, "answer", answer);
                    send(emitter, "suggestions", answer.suggestions());
                    send(emitter, "done", Map.of("conversationId", d.conversationId()));
                    send(emitter, "complete", Map.of("conversationId", d.conversationId(), "model", model));
                    status = "OK";
                  } else status = "CANCELLED";
                } catch (Exception ex) {
                  if (!stopped.get())
                    try {
                      send(
                          emitter,
                          "error",
                          AiFailure.safe(ex).getMessage());
                    } catch (Exception ignored) {
                    }
                  else status = "CANCELLED";
                } finally {
                  try {
                    var log = new AiCallLog();
                    log.requestId = UUID.randomUUID().toString();
                    log.userId = principal.id();
                    log.conversationId = d.conversationId();
                    log.model = model;
                    log.latency = (System.nanoTime() - start) / 1000000;
                    log.toolCalls = calls;
                    log.status = status;
                    log.createTime = LocalDateTime.now(clock);
                    logs.insert(log);
                  } finally {
                    guard.release(principal.id());
                    UserContext.clear();
                    stopped.set(true);
                    emitter.complete();
                  }
                }
              }));
    } catch (RejectedExecutionException ex) {
      guard.release(principal.id());
      throw new BusinessException(429, "AI请求繁忙，请稍后重试");
    }
    return emitter;
  }

  private static void send(SseEmitter e, String name, Object value) {
    try {
      e.send(
          SseEmitter.event()
              .name(name)
              .data(value instanceof String text ? new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text) : value,
                  org.springframework.http.MediaType.APPLICATION_JSON));
    } catch (java.io.IOException ex) {
      throw new RuntimeException("SSE disconnected");
    }
  }
}
