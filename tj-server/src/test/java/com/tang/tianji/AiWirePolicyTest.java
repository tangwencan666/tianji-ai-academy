package com.tang.tianji;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tang.tianji.ai.config.*;
import com.tang.tianji.ai.service.*;
import com.tang.tianji.ai.rag.CourseVectorStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

class AiWirePolicyTest {
  @Test void incompleteOrExtraJsonIsRejectedWithoutEchoingModelContent() {
    for (String text : List.of("{\"answer\":\"private-model-output",
        "{\"answer\":\"private-model-output\",\"suggestions\":[]} {}",
        "{\"answer\":\"private-model-output\"}")) {
      var error=assertThrows(com.tang.tianji.common.BusinessException.class,
          ()->ModelGateway.decode(text,com.tang.tianji.vo.AiModels.TutorAnswer.class));
      assertEquals(422,error.status);
      assertFalse(error.getMessage().contains("private-model-output"));
    }
  }
  @Test void fragmentedEscapesAndUnicodeAreDecodedWithoutSuggestionsLeakage() {
    var decoder=new StreamingAnswer();
    String json="{\"answer\":\"集合\\n\\\"例子\\\"\\u4f60好\",\"suggestions\":[\"秘密后缀\"]}";
    var answer=new StringBuilder();
    for(char c:json.toCharArray()) answer.append(decoder.append(String.valueOf(c)));
    assertEquals("集合\n\"例子\"你好",answer.toString());
  }
  @Test void realTokenChunksAreReturnedBeforeCompleteJson() {
    var decoder=new StreamingAnswer();
    assertEquals("HashMap",decoder.append("{\"answer\":\"HashMap"));
    assertEquals(" 非线程安全",decoder.append(" 非线程安全"));
    assertEquals("",decoder.append("\",\"suggestions\":[]}"));
  }
  @Test void providerCannotBeMarkedVerifiedInDisabledOrTestMode() {
    var state=new AiProviderState(false,"live","model","embedding"); state.success();
    assertEquals(false,state.status().get("verified"));
    state=new AiProviderState(true,"test","model","embedding"); state.success();
    assertEquals(false,state.status().get("verified"));
  }
  @Test void modelWireDisablesThinkingForBothStreamingAndBlockingBodies() throws Exception {
    var mapper=DeepSeekWireConfig.wireMapper(new ObjectMapper());
    for(boolean stream:List.of(false,true)) {
      var body=mapper.readTree(mapper.writeValueAsString(new ChatCompletionRequest(List.of(),"deepseek-flash",0.4,stream)));
      assertEquals("disabled",body.at("/thinking/type").asText());
      assertEquals(stream,body.get("stream").asBoolean());
    }
  }
  @Test void balanceAndTimeoutErrorsAreSafe() {
    var error=new HttpClientErrorException(HttpStatus.PAYMENT_REQUIRED,"private-provider-body");
    assertTrue(AiFailure.safe(error).getMessage().contains("余额"));
    assertFalse(AiFailure.safe(error).getMessage().contains("private-provider"));
    assertEquals(504,AiFailure.safe(new java.util.concurrent.TimeoutException()).status);
  }
  @Test void localVectorsRankActualJavaContentAboveUnrelatedContent() {
    var q=LexicalEmbeddingConfig.vector("ArrayList和LinkedList有什么区别");
    assertTrue(CourseVectorStore.cosine(q,LexicalEmbeddingConfig.vector("ArrayList 数组 LinkedList 双向链表"))>
        CourseVectorStore.cosine(q,LexicalEmbeddingConfig.vector("Python 文件读写异常处理")));
  }
}
