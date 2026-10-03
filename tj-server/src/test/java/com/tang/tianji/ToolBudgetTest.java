package com.tang.tianji;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tang.tianji.ai.service.ModelGateway;
import com.tang.tianji.common.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;

class ToolBudgetTest {
  public static class CounterTool {
    int count;

    @Tool(description = "Return current progress")
    public int progress() {
      return ++count;
    }
  }

  private ChatResponse call(int count) {
    var calls =
        java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new AssistantMessage.ToolCall("call-" + i, "function", "progress", "{}"))
            .toList();
    return new ChatResponse(List.of(new Generation(new AssistantMessage("", Map.of(), calls))));
  }

  @Test
  void repeatedModelToolRequestsStopAtEight() {
    var model = mock(ChatModel.class);
    when(model.call(any(Prompt.class))).thenReturn(call(1));
    var tool = new CounterTool();
    var gateway = new ModelGateway(model);
    assertThrows(
        BusinessException.class,
        () -> gateway.prepare(List.of(new UserMessage("progress")), tool, s -> {}));
    assertEquals(8, tool.count);
    verify(model, times(8)).call(any(Prompt.class));
  }

  @Test
  void oversizedParallelToolBatchRejectedBeforeAnyExecution() {
    var model = mock(ChatModel.class);
    when(model.call(any(Prompt.class))).thenReturn(call(9));
    var tool = new CounterTool();
    assertThrows(
        BusinessException.class,
        () -> new ModelGateway(model).prepare(List.of(new UserMessage("progress")), tool, s -> {}));
    assertEquals(0, tool.count);
  }

  @Test
  void completedToolsAreIncludedInFinalGenerationContext() {
    var model = mock(ChatModel.class);
    when(model.call(any(Prompt.class)))
        .thenReturn(call(1))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("ready")))));
    var tool = new CounterTool();
    var prepared =
        new ModelGateway(model).prepare(List.of(new UserMessage("progress")), tool, s -> {});
    assertEquals(1, prepared.toolCalls());
    assertTrue(
        prepared.messages().stream()
            .anyMatch(m -> m instanceof org.springframework.ai.chat.messages.ToolResponseMessage));
  }
}
