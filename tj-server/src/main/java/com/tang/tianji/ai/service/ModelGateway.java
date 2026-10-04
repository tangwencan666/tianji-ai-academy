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
  private final AiProviderState provider;
  private static final jakarta.validation.Validator VALIDATOR =
      jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

  public static <T> T validate(T result) {
    BusinessException.check(
        result != null && VALIDATOR.validate(result).isEmpty(), 422, "AI结构化结果未通过字段校验");
    return result;
  }

  public ModelGateway(ChatModel m) {
    this(m, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public ModelGateway(ChatModel m, AiProviderState provider) {
    model = m;
    client = ChatClient.create(m);
    this.provider = provider;
  }

  public Object probe() {
    try {
      String reply = client.prompt().user("你好")
          .options(org.springframework.ai.openai.OpenAiChatOptions.builder().maxTokens(64).build())
          .call().content();
      BusinessException.check(reply != null && !reply.isBlank(), 503, "模型返回空响应");
      if(provider != null) provider.success();
      return provider == null ? Map.of("verified",false) : provider.status();
    } catch(Exception e) { throw AiFailure.safe(e); }
  }

  public <T> T structured(String input, Class<T> type) {
    try {
      var converter = new org.springframework.ai.converter.BeanOutputConverter<>(type);
      int limit = type == com.tang.tianji.vo.AiModels.PlanOutput.class
          || type == com.tang.tianji.vo.AiModels.GeneratedQuestions.class ? 8192 : 4096;
      var response = client.prompt().system(TutorPrompt.SYSTEM)
          .user(input + "\n回答应简洁，避免重复。\n" + converter.getFormat())
          .options(org.springframework.ai.openai.OpenAiChatOptions.builder().maxTokens(limit)
              .responseFormat(new org.springframework.ai.openai.api.ResponseFormat(
                  org.springframework.ai.openai.api.ResponseFormat.Type.JSON_OBJECT, null)).build())
          .call().chatResponse();
      BusinessException.check(response != null && response.getResult() != null, 503, "模型返回空响应");
      BusinessException.check(!"length".equalsIgnoreCase(response.getResult().getMetadata().getFinishReason()),
          422, "模型输出达到长度上限，未保存不完整结果；请缩小任务范围");
      T result = decode(response.getResult().getOutput().getText(), type);
      if (Thread.currentThread().isInterrupted()) throw new BusinessException(504, "AI任务已取消");
      result = validate(result);
      if(provider != null) provider.success();
      return result;
    } catch (Exception e) {
      throw AiFailure.safe(e);
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
        .timeout(java.time.Duration.ofSeconds(45))
        .onErrorMap(AiFailure::safe);
  }

  /** Parse locally without BeanOutputConverter's raw-response error logging. */
  public static <T> T decode(String text, Class<T> type) {
    try {
      var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
          .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
      return validate(mapper.readValue(text, type));
    } catch (BusinessException e) { throw e; }
    catch (Exception e) { throw new BusinessException(422,"模型输出不是完整的有效 JSON，未保存结果"); }
  }

  public void verified() { if(provider != null) provider.success(); }
}
