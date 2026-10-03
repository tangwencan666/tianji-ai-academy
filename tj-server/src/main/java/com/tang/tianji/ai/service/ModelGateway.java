package com.tang.tianji.ai.service;

import com.tang.tianji.ai.prompt.TutorPrompt;
import com.tang.tianji.common.*;
import java.util.*;
import java.util.function.Consumer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ModelGateway {
  private final ChatModel model;
  private final ChatClient client;
  private static final jakarta.validation.Validator VALIDATOR =
      jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

  public static <T> T validate(T result) {
    BusinessException.check(
        result != null && VALIDATOR.validate(result).isEmpty(), 422, "AI结构化结果未通过字段校验");
    return result;
  }

  public ModelGateway(ChatModel m) {
    model = m;
    client = ChatClient.create(m);
  }

  public <T> T structured(String input, Class<T> type) {
    try {
      T result = client.prompt().system(TutorPrompt.SYSTEM).user(input).call().entity(type);
      if (Thread.currentThread().isInterrupted()) throw new BusinessException(504, "AI任务已取消");
      return validate(result);
    } catch (Exception e) {
      throw new BusinessException(503, "模型未返回有效结构化结果，请稍后重试");
    }
  }

  public record Prepared(List<Message> messages, int toolCalls) {}

  public Prepared prepare(List<Message> history, Object tools, Consumer<String> status) {
    var options =
        ToolCallingChatOptions.builder()
            .toolCallbacks(ToolCallbacks.from(tools))
            .internalToolExecutionEnabled(false)
            .build();
    var prompt = new Prompt(history, options);
    var manager = ToolCallingManager.builder().build();
    int calls = 0;
    for (int step = 0; step < 8; step++) {
      if (Thread.currentThread().isInterrupted()) throw new BusinessException(503, "请求已取消");
      var response = model.call(prompt);
      if (!response.hasToolCalls()) return new Prepared(prompt.getInstructions(), calls);
      int batch = response.getResult().getOutput().getToolCalls().size();
      BusinessException.check(calls + batch <= 8, 503, "工具调用达到上限，请缩小问题范围");
      calls += batch;
      status.accept("正在读取你的学习数据（工具调用 " + calls + "/8）");
      var result = manager.executeToolCalls(prompt, response);
      prompt = new Prompt(result.conversationHistory(), options);
    }
    throw new BusinessException(503, "AI步骤达到上限，请重试");
  }

  public static final org.springframework.ai.converter.BeanOutputConverter<
          com.tang.tianji.vo.AiModels.TutorAnswer>
      TUTOR_CONVERTER =
          new org.springframework.ai.converter.BeanOutputConverter<>(
              com.tang.tianji.vo.AiModels.TutorAnswer.class);

  public Flux<String> stream(List<Message> messages) {
    var formatted = new ArrayList<Message>(messages);
    formatted.add(new SystemMessage(TUTOR_CONVERTER.getFormat()));
    return client.prompt().messages(formatted).stream()
        .content()
        .timeout(java.time.Duration.ofSeconds(45));
  }
}
