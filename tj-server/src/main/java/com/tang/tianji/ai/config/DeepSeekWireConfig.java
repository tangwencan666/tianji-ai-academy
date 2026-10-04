package com.tang.tianji.ai.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

/** Spring AI 1.0.x has no extra_body option. Adapt only chat request serialization. */
@Configuration
public class DeepSeekWireConfig {
  public static ObjectMapper wireMapper(ObjectMapper mapper) {
    var base = mapper.copy();
    var module = new SimpleModule();
    module.addSerializer(ChatCompletionRequest.class, new JsonSerializer<ChatCompletionRequest>() {
      @Override public void serialize(ChatCompletionRequest request, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) base.valueToTree(request);
        node.putObject("thinking").put("type", "disabled");
        gen.writeTree(node);
      }
    });
    return mapper.copy().registerModule(module);
  }

  @Bean RestClientCustomizer deepSeekRestBody(ObjectMapper mapper) {
    return builder -> builder.messageConverters(converters -> converters.forEach(c -> {
      if (c instanceof MappingJackson2HttpMessageConverter json) json.setObjectMapper(wireMapper(mapper));
    }));
  }

  @Bean WebClientCustomizer deepSeekStreamBody(ObjectMapper mapper) {
    return builder -> builder.codecs(c -> c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(wireMapper(mapper))));
  }
}
