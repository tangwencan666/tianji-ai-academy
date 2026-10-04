package com.tang.tianji.ai.config;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Flux;

/** Explicit offline Tutor demo only. Never calls a network provider. */
@Configuration
@ConditionalOnProperty(name = "app.ai-mode", havingValue = "mock")
public class MockAiConfig {
  @Bean
  @Primary
  public ChatModel offlineDemoChatModel() {
    return new ChatModel() {
      public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(
            "{\"answer\":\"【Mock演示】这是本地固定响应，未调用真实模型。真实工具选择、学习计划和其他结构化生成请使用live模式。\",\"suggestions\":[]}"))));
      }
      public Flux<ChatResponse> stream(Prompt prompt) { return Flux.just(call(prompt)); }
    };
  }
}
